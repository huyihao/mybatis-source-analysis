package org.apache.ibatis.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.JdbcType;

public class Configuration {

	protected Environment environment;                   // 存储数据库和事务等环境信息
	
	/**
	 * setting中的配置项
	 */
	// (1) 布尔型
	protected boolean aggressiveLazyLoading = true;	     // 对任意有延迟属性的调用会使带有延迟加载属性的对象完整加载
	protected boolean cacheEnabled = true;               // 映射器缓存配置的全局开关
	protected boolean callSettersOnNulls = false;        // 指定当结果集中值为null时，是否调用映射对象的setter方法
	protected boolean lazyLoadingEnabled = false;	     // 关联对象延迟加载的全局开关
	protected boolean mapUnderscoreToCamelCase = false;  // 是否开启自动驼峰命名规则映射
	protected boolean multipleResultSetsEnabled = true;  // 是否允许单一语句返回多结果集
	protected boolean safeResultHandlerEnabled = true;   // 是否允许在嵌套语句中使用分页(ResultHandler)
	protected boolean safeRowBoundsEnabled = false;      // 是否允许在嵌套语句中使用分页(RowBounds)
	protected boolean useColumnLabel = true;             // 使用标签构建列名
	protected boolean useGeneratedKeys = false;          // 是否允许JDBC支持自动生成主键
	protected boolean useActualParamName = true;         // 允许用方法参数中声明的实际名称引用参数	
	
	// (2) 枚举型
	protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;   // 指定MyBatis应如何自动映射列到字段或属性
	protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;   // 指定自动映射当中未知列(或未知属性类型)时的行为
	protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;    // 配置默认的执行器
	protected JdbcType jdbcTypeForNull = JdbcType.OTHER;                 // 当没有为参数提供特定的JDBC类型时，为空值指定JDBC类型
	protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION; // Mybatis利用本地缓存机制(Local Cache)防止循环引用(circular references)和加速重复嵌套查询
	
	// (3) 其他
	protected String logPrefix;                          // 指定Mybatis增加到日志名称的前缀
	protected Class <? extends Log> logImpl;             // 指定MyBatis所用日志的具体实现，未指定时将自动查找
	protected Class <? extends VFS> vfsImpl;             // 指定VFS的实现类
	protected Integer defaultStatementTimeout;           // 设置超时时间，它决定驱动等待数据库响应的秒数
	protected Integer defaultFetchSize;                  // 设置数据库驱动程序默认返回的条数限制，此参数可以重新设置
	protected Set<String> lazyLoadTriggerMethods =       // 指定哪个对象的方法触发一次延迟加载
			  new HashSet<String>(Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" })); 
	
	protected Properties variables = new Properties();   // XML中<Properties>定义的变量
	protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();  // 反射器工厂
	protected ObjectFactory objectFactory = new DefaultObjectFactory();           // 对象工厂
	protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();  // 对象包装工厂
	protected ProxyFactory proxyFactory = null;
	protected String databaseId;
	protected Class<?> configurationFactory;
	
	protected final MapperRegistry mapperRegistry = new MapperRegistry(this);               // Mapper 接口及其对应的代理对象工厂的注册中心
	protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();    // 类型转换器注册中心
	protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();          // 类型别名注册中心
	protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry(); // LanguageDriver注册中心
	
	protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection");
	protected final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");  // 记录Cache id与Cache对象之间的对应关系
	protected final Map<String, String> cacheRefMap = new HashMap<String, String>();        // key是<cache-ref> 节点所在的namespace，value是节点的namespace属性指定的namespace
	protected final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");   // mapper.xml中，<ResultMap>节点的id跟构建出来的ResultMap的映射关系
	protected final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");  // mapper.xml中，<parameterMap>节点的id跟构建出来的ResultMap的映射关系
	protected final Set<String> loadedResources = new HashSet<String>();    // 存储已经加载的Mapper resource
	protected final Map<String, XNode> sqlFragments = new StrictMap<XNode>("XML fragments parsed from previous mappers");
	
	protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();  // 解析缓存引用出异常的CacheRefResolver
	protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();
	
	public Configuration() {
	    typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
	    typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

	    //typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
	    typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
	    typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

	    typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
	    typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
	    typeAliasRegistry.registerAlias("LRU", LruCache.class);
	    typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
	    typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

	    //typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

	    typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
	    //typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);

	    typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
	    typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
	    typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
	    typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
	    typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
	    typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
	    typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

	    //typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
	    //typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
	    
	    languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
	    //languageRegistry.register(RawLanguageDriver.class);
	}
	
	// Getters & Setters
	// [start]	
	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	
	public boolean isAggressiveLazyLoading() {
		return aggressiveLazyLoading;
	}
	
	public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
		this.aggressiveLazyLoading = aggressiveLazyLoading;
	}

	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	public boolean isCallSettersOnNulls() {
		return callSettersOnNulls;
	}

	public void setCallSettersOnNulls(boolean callSettersOnNulls) {
		this.callSettersOnNulls = callSettersOnNulls;
	}

	public boolean isLazyLoadingEnabled() {
		return lazyLoadingEnabled;
	}

	public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
		this.lazyLoadingEnabled = lazyLoadingEnabled;
	}

	public boolean isMapUnderscoreToCamelCase() {
		return mapUnderscoreToCamelCase;
	}

	public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
		this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
	}

	public boolean isMultipleResultSetsEnabled() {
		return multipleResultSetsEnabled;
	}

	public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
		this.multipleResultSetsEnabled = multipleResultSetsEnabled;
	}

	public boolean isSafeResultHandlerEnabled() {
		return safeResultHandlerEnabled;
	}

	public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
		this.safeResultHandlerEnabled = safeResultHandlerEnabled;
	}

	public boolean isSafeRowBoundsEnabled() {
		return safeRowBoundsEnabled;
	}

	public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
		this.safeRowBoundsEnabled = safeRowBoundsEnabled;
	}

	public boolean isUseColumnLabel() {
		return useColumnLabel;
	}

	public void setUseColumnLabel(boolean useColumnLabel) {
		this.useColumnLabel = useColumnLabel;
	}

	public boolean isUseGeneratedKeys() {
		return useGeneratedKeys;
	}

	public void setUseGeneratedKeys(boolean useGeneratedKeys) {
		this.useGeneratedKeys = useGeneratedKeys;
	}

	public void setUseActualParamName(boolean useActualParamName) {
		this.useActualParamName = useActualParamName;
	}

	public AutoMappingBehavior getAutoMappingBehavior() {
		return autoMappingBehavior;
	}

	public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
		this.autoMappingBehavior = autoMappingBehavior;
	}	
	
	public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
		return autoMappingUnknownColumnBehavior;
	}

	public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
		this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
	}
	
	public ExecutorType getDefaultExecutorType() {
		return defaultExecutorType;
	}

	public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
		this.defaultExecutorType = defaultExecutorType;
	}

	public JdbcType getJdbcTypeForNull() {
		return jdbcTypeForNull;
	}

	public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
		this.jdbcTypeForNull = jdbcTypeForNull;
	}

	public LocalCacheScope getLocalCacheScope() {
		return localCacheScope;
	}

	public void setLocalCacheScope(LocalCacheScope localCacheScope) {
		this.localCacheScope = localCacheScope;
	}	

	public Integer getDefaultStatementTimeout() {
		return defaultStatementTimeout;
	}

	public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
		this.defaultStatementTimeout = defaultStatementTimeout;
	}

	public Integer getDefaultFetchSize() {
		return defaultFetchSize;
	}

	public void setDefaultFetchSize(Integer defaultFetchSize) {
		this.defaultFetchSize = defaultFetchSize;
	}
	
	public Set<String> getLazyLoadTriggerMethods() {
		return lazyLoadTriggerMethods;
	}

	public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
		this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
	}

	public String getLogPrefix() {
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	public Class<? extends Log> getLogImpl() {
		return logImpl;
	}	
	
	public void setLogImpl(Class<? extends Log> logImpl) {
		if (logImpl != null) {
			this.logImpl = logImpl;
			LogFactory.useCustomLogging(logImpl);
		}
	}

	public Class<? extends VFS> getVfsImpl() {
		return vfsImpl;
	}

	public void setVfsImpl(Class<? extends VFS> vfsImpl) {
		if (vfsImpl != null) {
			this.vfsImpl = vfsImpl;
			VFS.addImplClass(this.vfsImpl);
		}
	}	
	
	public Properties getVariables() {
		return variables;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}
	
	
	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}

	public void setReflectorFactory(ReflectorFactory reflectorFactory) {
		this.reflectorFactory = reflectorFactory;
	}

	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}		
	
	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
		this.objectWrapperFactory = objectWrapperFactory;
	}
	
	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	public void setProxyFactory(ProxyFactory proxyFactory) {
		this.proxyFactory = proxyFactory;
	}
	
	public Class<?> getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(Class<?> configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	public TypeHandlerRegistry getTypeHandlerRegistry() {
		return typeHandlerRegistry;
	}
	
	public TypeAliasRegistry getTypeAliasRegistry() {
		return typeAliasRegistry;
	}	
	
	public void addLoadedResource(String resource) {
	    loadedResources.add(resource);
	}

	public boolean isResourceLoaded(String resource) {
	    return loadedResources.contains(resource);
	}	
	
	public void addCache(Cache cache) {
		caches.put(cache.getId(), cache);
	}
	
	// 获取所有Mapper的namespace
	public Collection<String> getCacheNames() {
		return caches.keySet();
	}
	
	public Collection<Cache> getCaches() {
		return caches.values();
	}
	
	public Cache getCache(String id) {
		return caches.get(id);
	}
	
	// 检查缓存中是否已包含某个mapper(的namespace)
	public boolean hasCache(String id) {
		return caches.containsKey(id);
	}
	
	public Map<String, XNode> getSqlFragments() {
		return sqlFragments;
	}
	
	public void addMappers(String packageName, Class<?> superType) {
		mapperRegistry.addMappers(packageName, superType);
	}
	
	public void addMappers(String packageName) {
		mapperRegistry.addMappers(packageName);
	}
	
	public <T> void addMapper(Class<T> type) {
		mapperRegistry.addMapper(type);
	}
	
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		return mapperRegistry.getMapper(type, sqlSession);
	}
	
	public boolean hasMapper(Class<?> type) {
		return mapperRegistry.hasMapper(type);
	}
	
	public void addCacheRef(String namespace, String referencedNamespace) {
		cacheRefMap.put(namespace, referencedNamespace);
	}
	
	public Collection<CacheRefResolver> getIncompleteCacheRefs() {
		return incompleteCacheRefs;
	}
	
	public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
		incompleteCacheRefs.add(incompleteCacheRef);
	}
	
	public void addResultMap(ResultMap rm) {
		resultMaps.put(rm.getId(), rm);
		//checkLocallyForDiscriminatedNestedResultMaps(rm);
		//checkGloballyForDiscriminatedNestedResultMaps(rm);
	}

	public Collection<String> getResultMapNames() {
		return resultMaps.keySet();
	}

	public Collection<ResultMap> getResultMaps() {
		return resultMaps.values();
	}

	public ResultMap getResultMap(String id) {
		return resultMaps.get(id);
	}

	public boolean hasResultMap(String id) {
		return resultMaps.containsKey(id);
	}
	
	public String getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(String databaseId) {
		this.databaseId = databaseId;
	}
	
	public void addParameterMap(ParameterMap pm) {
		parameterMaps.put(pm.getId(), pm);
	}

	public Collection<String> getParameterMapNames() {
		return parameterMaps.keySet();
	}

	public Collection<ParameterMap> getParameterMaps() {
		return parameterMaps.values();
	}

	public ParameterMap getParameterMap(String id) {
		return parameterMaps.get(id);
	}

	public boolean hasParameterMap(String id) {
		return parameterMaps.containsKey(id);
	}
	
	// 根据SQL语句的名称，检测配置是否有加载该SQL
	public boolean hasStatement(String statementName) {
		return hasStatement(statementName, true);
	}

	public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			//buildAllStatements();
		}
		return mappedStatements.containsKey(statementName);
	}
	
	public MappedStatement getMappedStatement(String id) {
		return this.getMappedStatement(id, true);
	}

	public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			//buildAllStatements();
		}
		return mappedStatements.get(id);
	}
	
	public Collection<XMLStatementBuilder> getIncompleteStatements() {
		return incompleteStatements;
	}
	
	public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
		incompleteStatements.add(incompleteStatement);
	}
	
	public LanguageDriverRegistry getLanguageRegistry() {
		return languageRegistry;
	}

	public void setDefaultScriptingLanguage(Class<?> driver) {
		if (driver == null) {
			driver = XMLLanguageDriver.class;
		}
		getLanguageRegistry().setDefaultDriverClass(driver);
	}

	public LanguageDriver getDefaultScriptingLanuageInstance() {
		return languageRegistry.getDefaultDriver();
	}
	// [end]	

	public boolean isUseActualParamName() {
		return true;
	}
	
	public ObjectFactory getObjectFactory() {
		return null;
	}
	
	public MetaObject newMetaObject(Object object) {
		return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
	}
	
	protected static class StrictMap<V> extends HashMap<String, V> {
		
		private static final long serialVersionUID = -7417525687823583926L;
		private final String name;
		
		public StrictMap(String name, int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
			this.name = name;
		}
		
		public StrictMap(String name, int initialCapacity) {
			super(initialCapacity);
			this.name = name;
		}
		
		public StrictMap(String name) {
			super();
			this.name = name;
		}
		
		public StrictMap(String name, Map<String, ? extends V> m) {
			super(m);
			this.name = name;
		}
		
		// [特别注意]: 对于带'\\'或'.'的key，这里实际上put了两次
		@SuppressWarnings("unchecked")
		public V put(String key, V value) {
			if (containsKey(key)) {   // 如果包含了该key，则直接返回异常
				throw new IllegalArgumentException(name + " already contains value for " + key);
			}
			if (key.contains(".")) {
				/**
				 * 按照"."将key切分成数组，并将数组的最后一项作为shortKey
				 * 加入shortKey在Map中能找到，证明存在二义性
				 * eg:
				 * 	 com.haha.UserMapper、com.hehe.UserMapper
				 * 的shortKey都是"UserMapper"，后插入的Mapper插入
				 * shortKey和映射的对象时就会存在二义性，这里插入一个Ambiguity
				 * 对象表示这种情况
				 */
				final String shortKey = getShortName(key);
				if (super.get(shortKey) == null) {
					super.put(shortKey, value);
				} else {
					super.put(shortKey, (V) new Ambiguity(shortKey));
				}
			}
			return super.put(key, value);
		}
		
		private String getShortName(String key) {
			final String[] keyParts = key.split("\\.");
			return keyParts[keyParts.length - 1];
		}
		
		// Ambiguity表示的是存在二义性的键值对，subject字段记录了存在二义性的key
		protected static class Ambiguity {
			final private String subject;
			
			public Ambiguity(String subject) {
				this.subject = subject;
			}
			
			public String getSubject() {
				return subject;
			}
		}
	}
}
