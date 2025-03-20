package com.github.aiassistant.platform;

import com.github.aiassistant.DAOProvider;
import com.github.aiassistant.util.ReflectUtil;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class Mybatis3DAOProvider implements DAOProvider {
    private final Map<Class<?>, Object> mapperMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final SqlSessionFactory sqlSessionFactory;

    public Mybatis3DAOProvider(DataSource dataSource) {
        this(dataSource,
                Mybatis3DAOProvider.class.getResourceAsStream("/mybatis3/mybatis-config.xml"),
                Mybatis3DAOProvider.class.getSimpleName());
    }

    public Mybatis3DAOProvider(DataSource dataSource, InputStream mybatisConfig, String environment) {
        Objects.requireNonNull(dataSource, "Mybatis3DAOProvider#requireNonNull(dataSource)");
        this.sqlSessionFactory = new DefaultSqlSessionFactory(createConfiguration(dataSource, mybatisConfig, environment));
    }

    public Mybatis3DAOProvider(Configuration configuration) {
        this.sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
    }

    public Mybatis3DAOProvider(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public static Configuration createConfiguration(DataSource dataSource, InputStream mybatisConfig, String environment) {
        XMLConfigBuilder parser = new XMLConfigBuilder(mybatisConfig, environment);
        Configuration configuration = parser.parse();
        configuration.setEnvironment(new Environment(environment, new JdbcTransactionFactory(), dataSource));
        return configuration;
    }

    @Override
    public <T> T getMapper(Class<T> mapperClass) {
        Object mapper = mapperMap.computeIfAbsent(mapperClass, k -> {
            ClassLoader classLoader = k.getClassLoader();
            return Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{k, Mybatis3DAOProviderMapper.class},
                    new MybatisInvocationHandler(k, sqlSessionFactory)
            );
        });
        return mapperClass.cast(mapper);
    }

    public interface Mybatis3DAOProviderMapper {

    }

    protected static class MybatisInvocationHandler implements InvocationHandler {
        private final Class<?> type;
        private final SqlSessionFactory sqlSessionFactory;

        protected MybatisInvocationHandler(Class<?> type, SqlSessionFactory sqlSessionFactory) {
            this.type = type;
            this.sqlSessionFactory = sqlSessionFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            if (method.isDefault()) {
                return ReflectUtil.invokeMethodHandle(true, proxy, method, args);
            }
            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                Object mapper = session.getMapper(type);
                return method.invoke(mapper, args);
            }
        }
    }
}
