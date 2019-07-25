package org.apache.ibatis.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
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
	protected Class<?> configurationFactory;
	
	protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
	protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
	
	public Configuration() {
		
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
	// [end]

	public MappedStatement getMappedStatement(String id) {
		return null;
	}	
	
	// 根据SQL语句的名称，检测配置是否有加载该SQL
	public boolean hasStatement(String statementName) {
		return true;
	}
	
	public boolean isUseActualParamName() {
		return true;
	}
	
	public ObjectFactory getObjectFactory() {
		return null;
	}
	
	public MetaObject newMetaObject(Object object) {
		return null;
	}
}
