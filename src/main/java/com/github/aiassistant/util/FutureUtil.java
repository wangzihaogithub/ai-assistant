package com.github.aiassistant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 异步工具类（1.对象转换，2.流程编排）
 */
public class FutureUtil {

    private static final Logger log = LoggerFactory.getLogger(FutureUtil.class);

    /**
     * 对象转换，将原类型转成新类型
     *
     * @param future 异步对象
     * @param mapper 转换逻辑
     * @param <R>    新对象
     * @param <T>    原对象
     * @return 转换结果
     */
    public static <R, T> CompletableFuture<R> map(CompletableFuture<T> future, Function<? super T, ? extends R> mapper) {
        CompletableFuture<R> result = new CompletableFuture<>();
        future.whenComplete((t, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                try {
                    result.complete(mapper.apply(t));
                } catch (Throwable e) {
                    try {
                        if (result.isDone()) {
                            result.obtrudeException(e);
                        } else {
                            result.completeExceptionally(e);
                        }
                    } catch (Throwable ttt) {
                        log.error("FutureUtil map Error while executing future {}", e.toString(), e);
                    }
                }
            }
        });
        return result;
    }

    public static <T> CompletableFuture<T> completeExceptionally(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    public static <T> CompletableFuture<T> accept(CompletableFuture<T> future, Consumer0<? super T> consumer) {
        CompletableFuture<T> result = new CompletableFuture<>();
        future.whenComplete((t, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
            } else {
                try {
                    consumer.accept(t);
                    result.complete(t);
                } catch (Throwable e) {
                    try {
                        if (result.isDone()) {
                            result.obtrudeException(e);
                        } else {
                            result.completeExceptionally(e);
                        }
                    } catch (Throwable ttt) {
                        log.error("FutureUtil accept Error while executing future {}", e.toString(), e);
                    }
                }
            }
        });
        return result;
    }

    /**
     * 接收异步的数据并执行后续处理逻辑
     *
     * @param future   异步
     * @param root     原异步，用于快速失败，如果中途失败，立即让原异步失败。
     * @param consumer 接收数据并执行处理逻辑
     * @param <T>      接收的数据类型
     */
    public static <T> void accept(CompletableFuture<T> future, CompletableFuture<?> root, Consumer<? super T> consumer) {
        future.whenComplete((t, throwable) -> {
            if (throwable != null) {
                root.completeExceptionally(throwable);
            } else {
                try {
                    consumer.accept(t);
                } catch (Throwable e) {
                    try {
                        if (root.isDone()) {
                            root.obtrudeException(e);
                        } else {
                            root.completeExceptionally(e);
                        }
                    } catch (Throwable ttt) {
                        log.error("FutureUtil accept Error while executing future {}", e.toString(), e);
                    }
                }
            }
        });
    }

    /**
     * 加入到原异步完成之后执行
     *
     * @param future 原异步
     * @param join   新异步
     * @param <T>    数据类型
     * @return 新异步执行后的异步
     */
    public static <T> CompletableFuture<T> join(CompletableFuture<T> future, CompletableFuture<T> join) {
        return future.whenComplete((t, throwable) -> {
            if (throwable != null) {
                try {
                    join.completeExceptionally(throwable);
                } catch (Throwable tt) {
                    log.error("FutureUtil join Error while executing future {}", tt.toString(), tt);
                }
            } else {
                try {
                    join.complete(t);
                } catch (Throwable tt) {
                    try {
                        join.obtrudeException(tt);
                    } catch (Throwable ttt) {
                        log.error("FutureUtil join Error while executing future {}", tt.toString(), tt);
                    }
                }
            }
        });
    }

    /**
     * 将装有异步的MapList的格式转为扁平格式（依次等待结果聚合为等待全部结束）
     *
     * @param map 装有异步的MapList
     * @param <K> Key数据格式
     * @param <V> Value数据格式
     * @return 扁平格式（等待全部结束）
     */
    public static <K, V> CompletableFuture<Map<K, List<V>>> allOfMapList(Map<K, List<CompletableFuture<V>>> map) {
        Map<K, List<V>> result = new ConcurrentHashMap<>();
        AtomicInteger countDown = new AtomicInteger(map.values().stream().mapToInt(List::size).sum());
        if (countDown.intValue() == 0) {
            return CompletableFuture.completedFuture(result);
        }
        CompletableFuture<Map<K, List<V>>> future = new CompletableFuture<>();
        for (Map.Entry<K, List<CompletableFuture<V>>> entry : map.entrySet()) {
            K key = entry.getKey();
            List<CompletableFuture<V>> value = entry.getValue();
            List<V> list = Collections.synchronizedList(new ArrayList<>(value.size()));
            result.put(key, list);
            for (CompletableFuture<V> f : value) {
                f.whenComplete((resp, throwable) -> {
                    if (throwable != null) {
                        try {
                            future.completeExceptionally(throwable);
                        } catch (Throwable t) {
                            log.error("FutureUtil allOfMapList Error while executing future {}", t.toString(), t);
                        }
                    } else {
                        list.add(resp);
                    }
                    if (countDown.decrementAndGet() == 0) {
                        try {
                            future.complete(result);
                        } catch (Throwable t) {
                            try {
                                future.obtrudeException(t);
                            } catch (Throwable ttt) {
                                log.error("FutureUtil allOfMapList Error while executing future {}", t.toString(), t);
                            }
                        }
                    }
                });
            }
        }
        return future;
    }

    /**
     * 将需要等待N次的有异步转为需要等待一次的扁平格式（依次等待结果聚合为等待全部结束）
     * 不同于{@link CompletableFuture#allOf(CompletableFuture[])} 的区别是，本方法的allOf是快速失败
     *
     * @param futures 异步
     * @return 需要等待一次的扁平格式（等待全部结束）
     */
    public static CompletableFuture<List<Object>> allOf(CompletableFuture<?>... futures) {
        return allOf((List) Arrays.asList(futures));
    }

    /**
     * 将需要等待N次的有异步转为需要等待一次的扁平格式（依次等待结果聚合为等待全部结束）
     * 不同于{@link CompletableFuture#allOf(CompletableFuture[])} 的区别是，本方法的allOf是快速失败
     *
     * @param futures 异步
     * @param <V>     Value数据格式
     * @return 需要等待一次的扁平格式（等待全部结束）
     */
    public static <V> CompletableFuture<List<V>> allOf(List<? extends CompletableFuture<V>> futures) {
        switch (futures.size()) {
            case 0: {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }
            case 1: {
                return futures.get(0).thenApply(v -> {
                    ArrayList<V> list = new ArrayList<>(1);
                    list.add(v);
                    return list;
                });
            }
            default: {
                CompletableFuture<List<V>> result = new CompletableFuture<>();
                List<V> list = new ArrayList<>(futures.size());
                AtomicInteger count = new AtomicInteger(futures.size());
                for (CompletableFuture<V> future : futures) {
                    future.whenComplete((v, throwable) -> {
                        if (throwable != null) {
                            try {
                                result.completeExceptionally(throwable);
                            } catch (Throwable t) {
                                log.error("FutureUtil allOf Error while executing future {}", t.toString(), t);
                            }
                        } else {
                            synchronized (list) {
                                list.add(v);
                            }
                            if (count.decrementAndGet() == 0) {
                                try {
                                    result.complete(list);
                                } catch (Throwable t) {
                                    try {
                                        future.obtrudeException(t);
                                    } catch (Throwable ttt) {
                                        log.error("FutureUtil allOf Error while executing future {}", t.toString(), t);
                                    }
                                }
                            }
                        }
                    });
                }
                return result;
            }
        }
    }

    /**
     * 将需要等待两次的有异步转为需要等待一次的扁平格式（依次等待结果聚合为等待全部结束）
     *
     * @param future    异步
     * @param scheduled 是否切换线程，如果要后续任务会阻塞IO线程，建议穿一个scheduled
     * @param <V>       Value数据格式
     * @return 需要等待一次的扁平格式（等待全部结束）
     */
    public static <V> CompletableFuture<V> allOf(CompletableFuture<CompletableFuture<V>> future, Executor scheduled) {
        Executor scheduledNotNull = scheduled == null ? Runnable::run : e -> {
            boolean[] run = new boolean[1];
            try {
                scheduled.execute(() -> {
                    run[0] = true;
                    e.run();
                });
            } catch (Throwable r) {
                if (run[0]) {
                    log.error("FutureUtil allOf Error while executing future {}", r.toString(), r);
                } else {
                    e.run();
                }
            }
        };
        CompletableFuture<V> result = new CompletableFuture<>();
        future.whenComplete((future1, throwable) -> {
            if (throwable != null) {
                scheduledNotNull.execute(() -> {
                    try {
                        result.completeExceptionally(throwable);
                    } catch (Throwable t) {
                        log.error("FutureUtil allOf Error while executing future {}", t.toString(), t);
                    }
                });
            } else {
                future1.whenComplete((v, throwable1) -> {
                    if (throwable1 != null) {
                        scheduledNotNull.execute(() -> {
                            try {
                                result.completeExceptionally(throwable1);
                            } catch (Throwable t) {
                                log.error("FutureUtil allOf Error while executing future {}", throwable1.toString(), throwable1);
                            }
                        });
                    } else {
                        scheduledNotNull.execute(() -> {
                            try {
                                result.complete(v);
                            } catch (Throwable t) {
                                try {
                                    result.obtrudeException(t);
                                } catch (Throwable tt) {
                                    log.error("FutureUtil allOf Error while executing future {}", t.toString(), t);
                                }
                            }
                        });
                    }
                });
            }
        });
        return result;
    }

    public static <V> CompletableFuture<V> allOf(CompletableFuture<CompletableFuture<V>> future) {
        return allOf(future, null);
    }

    /**
     * 将装有异步的List的格式转为扁平格式（依次等待结果聚合为等待全部结束）
     *
     * @param list 装有异步的List
     * @param <V>  Value数据格式
     * @return 扁平格式（等待全部结束）
     */
    public static <V> CompletableFuture<List<List<V>>> allOfListList(List<List<CompletableFuture<V>>> list) {
        List<CompletableFuture<List<V>>> result = new ArrayList<>();
        for (List<CompletableFuture<V>> l : list) {
            result.add(allOf(l));
        }
        return allOf(result);
    }

    /**
     * 将装有异步的Map的格式转为扁平格式（依次等待结果聚合为等待全部结束）
     *
     * @param map 装有异步的Map
     * @param <K> Key数据格式
     * @param <V> Value数据格式
     * @return 扁平格式（等待全部结束）
     */
    public static <K, V> CompletableFuture<Map<K, V>> allOfMap(Map<K, CompletableFuture<V>> map) {
        Map<K, V> result = new ConcurrentHashMap<>();
        AtomicInteger countDown = new AtomicInteger(map.size());
        if (countDown.intValue() == 0) {
            return CompletableFuture.completedFuture(result);
        }
        CompletableFuture<Map<K, V>> future = new CompletableFuture<>();
        for (Map.Entry<K, CompletableFuture<V>> entry : map.entrySet()) {
            K key = entry.getKey();
            CompletableFuture<V> value = entry.getValue();
            value.whenComplete((resultList, throwable) -> {
                if (throwable != null) {
                    try {
                        future.completeExceptionally(throwable);
                    } catch (Throwable t) {
                        log.error("FutureUtil allOfMap Error while executing future {}", t.toString(), t);
                    }
                } else {
                    result.put(key, resultList);
                    if (countDown.decrementAndGet() == 0) {
                        try {
                            future.complete(result);
                        } catch (Throwable t) {
                            try {
                                future.obtrudeException(t);
                            } catch (Throwable tt) {
                                log.error("FutureUtil allOfMap Error while executing future {}", t.toString(), t);
                            }
                        }
                    }
                }
            });
        }
        return future;
    }

    @FunctionalInterface
    public interface Consumer0<T> {

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @throws Throwable Throwable
         */
        void accept(T t) throws Throwable;
    }
}
