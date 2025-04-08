package com.github.aiassistant.platform;

import com.github.aiassistant.DAOProvider;
import com.github.aiassistant.util.ReflectUtil;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class Mybatis3DAOProvider implements DAOProvider {
    public static final List<String> MAPPERS = Collections.unmodifiableList(Arrays.asList(
            "mybatis3/AiAssistantFewshotMapper.xml",
            "mybatis3/AiAssistantJsonschemaMapper.xml",
            "mybatis3/AiAssistantKnMapper.xml",
            "mybatis3/AiAssistantMstateMapper.xml",
            "mybatis3/AiChatAbortMapper.xml",
            "mybatis3/AiChatHistoryMapper.xml",
            "mybatis3/AiChatMapper.xml",
            "mybatis3/AiChatWebsearchMapper.xml",
            "mybatis3/AiEmbeddingMapper.xml",
            "mybatis3/AiMemoryErrorMapper.xml",
            "mybatis3/AiMemoryMapper.xml",
            "mybatis3/AiMemoryMessageMapper.xml",
            "mybatis3/AiMemoryMessageToolMapper.xml",
            "mybatis3/AiMemoryMstateMapper.xml",
            "mybatis3/AiQuestionClassifyMapper.xml",
            "mybatis3/AiToolMapper.xml",
            "mybatis3/AiToolParameterMapper.xml",
            "mybatis3/AiVariablesMapper.xml",
            "mybatis3/AiQuestionClassifyAssistantMapper.xml",
            "mybatis3/AiMemoryMessageKnMapper.xml",
            "mybatis3/AiChatWebsearchResultMapper.xml",
            "mybatis3/AiChatReasoningMapper.xml",
            "mybatis3/AiChatReasoningPlanMapper.xml",
            "mybatis3/AiChatClassifyMapper.xml",
            "mybatis3/KnSettingWebsearchBlacklistMapper.xml",
            "mybatis3/AiAssistantMapper.xml"));
    private final Map<Class<?>, Object> mapperMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final SqlSessionFactory sqlSessionFactory;

    public Mybatis3DAOProvider(DataSource dataSource) {
        this(createConfiguration(dataSource, MAPPERS));
    }

    public Mybatis3DAOProvider(Configuration configuration) {
        this.sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
    }

    public Mybatis3DAOProvider(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public static Configuration createConfiguration(DataSource dataSource, Collection<String> mapperXmlResource) {
        Configuration configuration = new Configuration();
        addMapperXmlResource(configuration, mapperXmlResource);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment(Mybatis3DAOProvider.class.getSimpleName(), new JdbcTransactionFactory(), dataSource));
        return configuration;
    }

    public static void addMapperXmlResource(Configuration configuration, Collection<String> mapperXmlResource) {
        for (String xmlResource : mapperXmlResource) {
            try (InputStream inputStream = Resources.getResourceAsStream(xmlResource)) {
                XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, xmlResource, configuration.getSqlFragments());
                mapperParser.parse();
            } catch (IOException e) {
                throw new BuilderException("Error parsing SQL Mapper Configuration " + xmlResource + ". Cause: " + e, e);
            }
        }
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
