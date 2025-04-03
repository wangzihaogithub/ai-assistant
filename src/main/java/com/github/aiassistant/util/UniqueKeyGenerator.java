package com.github.aiassistant.util;

import java.lang.management.ManagementFactory;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;


public class UniqueKeyGenerator {

    // 机器标识位数
    private static final long workerIdBits = 5L;
    // 数据中心标识位数
    private static final long datacenterIdBits = 5L;
    // 机器ID最大值
    private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 初始值
    private static final long INIT_SEQ = 1;
    // 步长 假定每秒100万订单
    private static final long INTERVAL = 1000000;
    // 数据中心ID最大值
    private static long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    // 数据标识id + 运行的机器ID
    private static final long distributedId = getMaxWorkerId(maxWorkerId);
    // 当前的计算值
    private static long CURRENT_SEQ = 0;

    public static synchronized String nextId() {
        String timestamp = timeGen();
        String nextId = timestamp + distributedId;
        CURRENT_SEQ = CURRENT_SEQ + 1;
        if (CURRENT_SEQ > INIT_SEQ + INTERVAL) {
            CURRENT_SEQ = INIT_SEQ;
        }
        nextId += String.format("%06d", CURRENT_SEQ);
        return nextId;
    }

    private static String timeGen() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(System.currentTimeMillis());
    }

    /**
     * 获取 maxWorkerId
     *
     * @param maxWorkerId
     * @return java.lang.Long
     * @date 2020/9/24 7:05 下午
     */
    private static Long getMaxWorkerId(Long maxWorkerId) {
        StringBuffer mpid = new StringBuffer();
        mpid.append(getDatacenterId(maxDatacenterId));
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null) {
            // GET jvmPid
            mpid.append(name.split("@")[0]);
        }
        // MAC + PID 的 hashcode 获取16个低位
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);

    }

    /**
     * 数据标识id部分
     *
     * @param maxDatacenterId
     * @return java.lang.Long
     * @date 2020/9/24 7:05 下午
     */
    private static Long getDatacenterId(Long maxDatacenterId) {

        Long id = 0L;
        try {
            NetworkInterface netInterface = getLocalNetAddress();
            if (netInterface != null) {
                byte[] mac = netInterface.getHardwareAddress();
                if (mac != null) {
                    id = ((0x000000FF & Byte.toUnsignedLong(mac[mac.length - 1])) | (0x0000FF00 & (Byte.toUnsignedLong(mac[mac.length - 2]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            } else {
                id = 1L;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * 找到本机的MAC地址
     *
     * @param
     * @return java.net.NetworkInterface
     * @date 2020/9/24 7:06 下午
     */
    protected static NetworkInterface getLocalNetAddress() {

        Enumeration<NetworkInterface> netInterfaces = null;

        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (netInterfaces != null) {
            for (NetworkInterface netInterface : Collections.list(netInterfaces)) {
                for (InterfaceAddress netAddress : netInterface.getInterfaceAddresses()) {
                    boolean flag = !netAddress.getAddress().isLoopbackAddress() && netAddress.getAddress().getHostAddress().indexOf(":") == -1;
                    if (flag) {
                        return netInterface;
                    }
                }
            }
        }

        return null;
    }

}
