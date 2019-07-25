package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

public class XMLConfigBuilder extends BaseBuilder {

	private boolean parsed;       // 标识是否已经解析过mybatis-config.xml配置文件
	private XPathParser parser;   // 用于解析mybatis-config.xml配置文件的XPathParser对象
	private String environment;   // 标识<environment>配置的名称，默认读取<environment>标签的default属性
	private ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();   // ReflectorFactory负责创建和缓存Reflector对象
	
	public XMLConfigBuilder(Reader reader) {
		this(reader, null, null);
	}
	
	public XMLConfigBuilder(Reader reader, String environment) {
		this(reader, environment, null);
	}
	
	public XMLConfigBuilder(Reader reader, String environment, Properties props) {
		this(new XPathParser(reader, false, props, new XMLMapperEntityResolver()), environment, props);
	}
	
	public XMLConfigBuilder(InputStream inputStream) {
		this(inputStream, null, null);
	}
	
	public XMLConfigBuilder(InputStream inputStream, String environment) {
		this(inputStream, environment, null);
	}
	
	public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
		this(new XPathParser(inputStream, false, props, new XMLMapperEntityResolver()), environment, props);
	}
	
	public XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
		super(new Configuration());
		this.configuration.setVariables(props);
		this.parsed = false;
		this.environment = environment;
		this.parser = parser;
	}

	public Configuration parse() {
		// 避免重复解析mybatis-config.xml配置文件，该文件只在mybatis启动时解析一次
		if (parsed) {
			throw new BuilderException("Each XMLConfigBuilder can only be used once.");
		}
		parsed = true;
		parseConfiguration(parser.evalNode("/configuration"));
		return configuration;
	}
	
	private void parseConfiguration(XNode root) {
		try {
			// 示例:
			// 这里通过root.evalNode("settings")获取到了settings节点
			// eg: 
			// <configuration>
			//	  <settings>
			//     	 <setting name="aaa" value="bbb"/>
			//     	 <setting name="ccc" value="ddd"/>			
			//    </settings>
			// </configuration>		
			
			// 解析 <settings> 节点
			Properties settings = settingAsPropertiess(root.evalNode("settings"));
			// 解析 <properties> 节点
			propertiesElement(root.evalNode("properties"));
			loadCustomVfs(settings);  // 设置Configuration的vfsImpl字段
			// 解析 <typeAliases> 节点
			typeAliasesElement(root.evalNode("typeAliases"));
			// 解析 <plugins> 节点
			pluginElement(root.evalNode("plugins"));
			// 解析 <objectFactory> 节点
			objectFactoryElement(root.evalNode("objectFactory"));
			// 解析 <objectWrapperFactory> 节点
			objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
			// 解析 <reflectorFactory> 节点
			reflectorFactoryElement(root.evalNode("reflectorFactory"));
			settingsElement(settings);  // 将settings值设置到Configuration中
			// 解析 <environments> 节点
			environmentsElement(root.evalNode("environments"));
			// 解析 <databaseIdProvider> 节点
			databaseIdProviderElement(root.evalNode("databaseIdProvider"));
			// 解析 <typeHandlers> 节点
			typeHandlerElement(root.evalNode("typeHandlers"));
			// 解析 <mappers> 节点
			mapperElement(root.evalNode("mappers"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
		}
	}
	
	/**
	 * 返回所有<setting>标签的属性，eg:
	 * { 
	 *   jdbcTypeForNull=OTHER, 
	 *   cacheEnabled=true, 
	 *   defaultFetchSize=100, 
	 *   lazyLoadingEnabled=true,
 	 *   multipleResultSetsEnabled=true, 
 	 *   autoMappingBehavior=PARTIAL, 
 	 *   safeRowBoundsEnabled=false,
 	 *   autoMappingUnknowColumnBehavior=WARING, 
 	 *   defaultStatementTimeout=25,
 	 *   lazyLoadTriggerMethods=equals,clone,hashCode,toString, 
 	 *   localCacheScope=SESSION,
     *   defaultExecutorType=SIMPLE, useGeneratedKeys=false, useColumnLabel=true 
     * }
     * 
     * 配置示例:
     * 	<!-- 全量settings的配置样例 -->
	 *  <settings>
     *		<setting name="cacheEnabled" value="true"/>                      <!-- 映射器中配置缓存的全局开关 -->
	 *  	<setting name="lazyLoadingEnabled" value="true"/>                <!-- 延迟加载的全局开关 -->
	 *		<setting name="multipleResultSetsEnabled" value="true"/>         <!-- 是否允许单一语句返回多结果集 -->
	 * 		<setting name="useColumnLabel" value="true"/>                    <!-- 使用列标签代替列名 -->
	 * 		<setting name="useGeneratedKeys" value="false"/>                 <!-- 允许JDBC支持自动生成主键，需要驱动兼容 -->
	 *		<setting name="autoMappingBehavior" value="PARTIAL"/>            <!-- 指定MyBatis应如何自动映射到字段或属性 -->
	 *		<setting name="autoMappingUnknowColumnBehavior" value="WARING"/> <!-- 指定自动映射当中未知列时的行为 -->
	 *		<setting name="defaultExecutorType" value="SIMPLE"/>             <!-- 配置默认的执行器 -->
	 *		<setting name="defaultStatementTimeout" value="25"/>             <!-- 设置超时时间，它决定驱动等待数据库响应的秒数 -->
	 *		<setting name="defaultFetchSize" value="100"/>                   <!-- 设置数据库驱动程序默认返回的条数限制 -->
	 *		<setting name="safeRowBoundsEnabled" value="false"/>             <!-- 允许在嵌套语句中使用分页 -->
	 *		<setting name="localCacheScope" value="SESSION"/>                <!-- MyBatis利用本地缓存机制防止循环引用和加速重复嵌套查询 -->
	 *		<setting name="jdbcTypeForNull" value="OTHER"/>                  <!-- 当没有为参数提供特定的JDBC类型时，为空值指定JDBC类型 -->
	 *		<setting name="lazyLoadTriggerMethods" value="equals,clone, hashCode,toString"/> <!-- 指定哪个对象的方法触发一次延迟加载 -->
	 *  </settings>	
     */
	private Properties settingAsPropertiess(XNode context) {
		if (context == null) {
			return new Properties();
		}
		Properties props = context.getChildrenAsProperties();
		// 检查setting中定义的属性是否是在configuration中定义的，只要有一个属性名定义有误，就会抛出异常，初始化中断
		MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
		for (Object key : props.keySet()) {
			if (!metaConfig.hasSetter(String.valueOf(key))) {
				throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive)");
			}
		}
		return props;
	}
	
	/**
	 * [属性解析]
	 * 解析<Properties>:
	 * (1) 使用Property子元素的用法
	 * 	<properties>
	 *  	<property name="database.driver" value="com.mysql.jdbc.Driver"/>
	 *		<property name="database.url" value="jdbc:mysql://localhost:3306/chapter3?useSSL=false"/>
	 *   	<property name="database.username" value="root"/>
	 *		<property name="database.password" value="root"/>
	 *	</properties>
	 *
	 * (2) 使用引用资源文件的用法
	 * 	<properties resource="properties-file/jdbc.encrypted.properties"></properties>
	 *  <properties url="properties-file/jdbc.encrypted.properties"></properties>  <!-- 使用网络路径或盘路径时用url -->
	 * 
	 * 这里也存在一个问题，如果在创建XMLConfigBuilder对象时传入了Properties对象，而xml中又定义了Properties标签
	 * 这时传入的Properties会被xml中的定义覆盖掉，当然如果不同时使用没有什么问题
	 */
	private void propertiesElement(XNode context) throws Exception {
		if (context != null) {
			// 获取<Properties>的子节点的属性并转换为属性对象Properties
			Properties defaults = context.getChildrenAsProperties();
			// 获取标签属性，在通过属性取加载资源文件
			String resource = context.getStringAttribute("resource");
			String url = context.getStringAttribute("url");
			// resource和url属性不能同时存在，只能用一个去获取资源
			if (resource != null && url != null) {
				throw new BuilderException("The properties element connot specify both a URL and a resource based property file reference.  Please specify one or th other.");
			}
			if (resource != null) {
				defaults.putAll(Resources.getResourceAsProperties(resource));
			} else if (url != null) {
				defaults.putAll(Resources.getUrlAsProperties(url));
			}
			Properties vars = configuration.getVariables();
			if (vars != null) {
				defaults.putAll(vars);
			}
			parser.setVariables(defaults);
			configuration.setVariables(defaults);
		}
	}	

	/**
	 * [自定义VFS注册]
	 * 指定VFS的自定义实现类是通过<settings>中<setting name="vfsImpl" value="vfs1,vfs2"/>的设置来配置的
	 */
	private void loadCustomVfs(Properties props) throws ClassNotFoundException {
		String value = props.getProperty("vfsImpl");
		if (value != null) {
			String[] clazzes = value.split(",");
			for (String clazz : clazzes) {
				if (!clazz.isEmpty()) {
					@SuppressWarnings("unchecked")
					Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
					// 这里的逻辑有点问题，configuration中存在的是一个vfsImpl Class而不是一个Class数组，
					// 这样configuration只存放了自定义设置的VFS的最后一个
					configuration.setVfsImpl(vfsImpl);  
				}
			}
		}
	}
	
	/**
	 * [类别名注册]
	 * 类别名注册有两种方式: (1) 包扫描自动注册   (2) 为每个类单独注册
	 * 	<typeAliases> 
	 *		<!-- 别名 -->
	 *		<typeAlias alias="role" type="com.learn.ssm.chapter4.pojo.User"/>
	 *	
	 *		<!-- 使用包的方式扫描自动添加别名，别名为类名（首字母变小写） -->
	 *		<package name="com.learn.ssm.chapter4.pojo"/>
	 *  </typeAliases>
	 *  
	 *  从代码来看，支持两种方式混合使用
	 */
	private void typeAliasesElement(XNode parent) {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {   // 处理<package>节点
					String typeAliasPackage = child.getStringAttribute("name");
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					String alias = child.getStringAttribute("alias");  // 注册的类别名
					String type = child.getStringAttribute("type");    // 注册的类全限定名
					try {
						Class<?> clazz = Resources.classForName(type);
						if (alias == null) {
							// 如果没有显式指定别名，则扫描该类是否使用了@Alias注解，有则使用注解值作为别名，否则默认别名的class.getSimpleName();
							typeAliasRegistry.registerAlias(clazz);  
						} else {
							typeAliasRegistry.registerAlias(alias, clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new BuilderException("Error registering typeAlias for '" + alias + ". Cause: " + e, e);
					}
				}
			}
		}
	}
	
	/**
	 * [插件]
	 */
	private void pluginElement(XNode parent) {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				//String interceptor = child.getStringAttribute("interceptor");
				//Properties properties = child.getChildrenAsProperties();
				// 暂时留空
			}
		}
	}
	
	/**
	 * [对象工厂]
	 * 主要用于MetaObject解析属性表达式为Object设置值时使用
	 * 
	 * 配置示例:
	 * 	<!-- 对象工厂 -->
	 *  <objectFactory type="com.learn.ssm.chapter4.objectFactory.MyObjectFactory">
	 *		<property name="prop1" value="value1"/>
	 *      <property name="prop2" value="value2"/>
	 *	</objectFactory>
	 */
	private void objectFactoryElement(XNode context) throws Exception {
		if (context != null) {
			// 获取 <objectFactory> 节点的type属性
			String type = context.getStringAttribute("type");
			// 获取 <objectFactory> 节点下配置的信息，并形成Properties对象
			Properties properties = context.getChildrenAsProperties();
			// 进行别名解析后，实例化自定义的ObjectFactory实现
			ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
			// 设置自定义的ObjectFactory 的属性，完成初始化的相关操作
			factory.setProperties(properties);
			// 将自定义 ObjectFactory 对象记录到Configuration对象的objectFactory 字段中，待后续使用
			configuration.setObjectFactory(factory);
		}
	}
	
	/**
	 * [对象包装工厂]
	 * 除非有特殊需求，一般没有提供自定义对象包装工厂的需要
	 */
	private void objectWrapperFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
			configuration.setObjectWrapperFactory(factory);
		}
	}
	
	/**
	 * [反射器工厂]
	 * 缓存类和解析存储类的元信息的反射器之间的映射关系
	 * 除非有特殊需求，一般没有提供自定义反射器工厂的需要
	 */
	private void reflectorFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
			configuration.setReflectorFactory(factory);
		}
	}
	
	/**
	 * <settings>中的每个配置在Configuration中都有对应的属性，逐一解析设值
	 */
	private void settingsElement(Properties props) {
		configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
		configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
		configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
		configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
	    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
	    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), true));
	    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
	    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
	    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
	    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
	    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
	    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
	    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
	    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));	
	    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
	    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
	    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
	    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
	    //configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
	    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
	    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), false));
	    configuration.setLogPrefix(props.getProperty("logPrefix"));
	    @SuppressWarnings("unchecked")
	    Class<? extends Log> logImpl = (Class<? extends Log>) resolveClass(props.getProperty("logImpl"));
	    configuration.setLogImpl(logImpl);
	    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
	}
	
	/**
	 * [多环境配置支持]
	 * 在实际生产中，同一项目可能分为开发、测试和生产多个不同的环境，每个环境的配置可能也不尽相同。
	 * MyBatis可以配置多个<environment>节点，配个节点对应一个环境，通过<environments>
	 * 节点的default属性来指定要使用哪一套配置
	 * 
	 * 实例配置(只配置了一套环境):
	 * 	<environments default="development">
	 *		<environment id="development">
	 *			<transactionManager type="JDBC"/>
	 *			<dataSource type="POOLED">
	 *				<property name="driver" value="${database.driver}"/>
	 *				<property name="url" value="${database.url}"/>
	 *				<property name="username" value="${database.username}"/>
	 *				<property name="password" value="${database.password}"/>
	 *			</dataSource>
	 *		</environment>
	 *  </environments>	
	 */
	private void environmentsElement(XNode context) throws Exception {
		if (context != null) {
			if (environment == null) {
				environment = context.getStringAttribute("default");
			}
			for (XNode child : context.getChildren()) {
				String id = child.getStringAttribute("id");
				if (isSpecifiedEnvironment(id)) {
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					DataSource dataSource = dsFactory.getDataSource();
					Environment.Builder environmentBuilder = new Environment.Builder(id)
							.transactionFactory(txFactory)
							.dataSource(dataSource);
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}
	
	// 解析<environment>节点下的<transactionManager>节点，得到事务工厂
	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}
	
	// // 解析<environment>节点下的<dataSource>节点，得到数据源工厂，数据源由得到的工厂提供
	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}
	
	private boolean isSpecifiedEnvironment(String id) {
		if (environment == null) {
			throw new BuilderException("No environment specified.");
		} else if (id == null) {
			throw new BuilderException("Environment requires an id attribute.");
		} else if (environment.equals(id)) {
			return true;
		} 
		return false;
	}
	
	// 不是重要内容，暂时留空
	private void databaseIdProviderElement(XNode context) {
		//
	}
	
	/**
	 * [类型处理器注册]
	 * 
	 * 配置示例:
	 * 	<!-- 类型处理器 -->
	 *  <typeHandlers>
	 *		<!-- 逐个定义 -->
	 *		<typeHandler jdbcType="VARCHAR" javaType="string" handler="com.learn.ssm.chapter4.typeHandler.MyTypeHandler"/>
	 *	
	 *		<!-- 使用包扫描的方式加载 -->
	 *		<package name="com.learn.ssm.chapter4.typeHandler"/>
	 * </typeHandlers>
	 * @param parent
	 */
	private void typeHandlerElement(XNode parent) {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				// 扫描加载包中的所有类型处理器
				if ("package".equals(child.getName())) {
					String typeHandlerPackage = child.getStringAttribute("name");
					typeHandlerRegistry.register(typeHandlerPackage);
				}
				// 单个类型处理器注册
				else {
					String javaTypeName = child.getStringAttribute("javaType");
					String jdbcTypeName = child.getStringAttribute("jdbcType");
					String handlerTypeName = child.getStringAttribute("handler");
					Class<?> javaTypeClass = resolveClass(javaTypeName);
					JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
					Class<?> typeHandlerClass = resolveClass(handlerTypeName);
					if (javaTypeClass != null) {
						if (jdbcType == null) {
							typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
						} else {
							typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
						}
					} else {
						typeHandlerRegistry.register(typeHandlerClass);
					}
				}
			}
		}
	}
	
	/**
	 * [mapper映射配置]
	 * 
	 * 配置示例(四种配置方式):
	 *  <!-- 映射文件 -->    
     *  <mappers>
     *		<!-- (1) 用文件路径引入映射器 -->
     *		<mapper resource="com/learn/ssm/chapter4/mapper/FileMapper.xml"/>
     *		<mapper resource="com/learn/ssm/chapter4/mapper/RoleMapper.xml"/>
     *		<mapper resource="com/learn/ssm/chapter4/mapper/UserMapper.xml"/>
     *		<mapper resource="com/learn/ssm/chapter4/mapper/UserMapper2.xml"/>
     *	
     *		<!-- (2) 用包名引入映射器(xml必须跟interface在同一个包内) -->
     *		<package name="com.learn.ssm.chapter4.mapper"/>
     *	
     *		<!-- (3) 用类注册引入映射器 -->
     *		<mapper class="com.learn.ssm.chapter4.mapperInterface.FileMapper"/>
     *		<mapper class="com.learn.ssm.chapter4.mapperInterface.RoleMapper"/>
     *		<mapper class="com.learn.ssm.chapter4.mapperInterface.UserMapper"/>
     *		<mapper class="com.learn.ssm.chapter4.mapperInterface.UserMapper2"/>
     *	
     *		<!-- (4) 使用文件的绝对路径引入映射器 -->
     *		<!-- mapper url="F:\mybatis\workspace\mybatis-chapter4\src\main\java\com\learn\ssm\chapter4\mapper\FileMapper.xml"/>
     *		<mapper url="F:\mybatis\workspace\mybatis-chapter4\src\main\java\com\learn\ssm\chapter4\mapper\RoleMapper.xml"/>
     *		<mapper url="F:\mybatis\workspace\mybatis-chapter4\src\main\java\com\learn\ssm\chapter4\mapper\UserMapper.xml"/>
     *		<mapper url="F:\mybatis\workspace\mybatis-chapter4\src\main\java\com\learn\ssm\chapter4\mapper\UserMapper2.xml"/>
     *  </mappers>
	 * @param parent
	 */
	private void mapperElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					// 扫描指定的包，并向MapperRegistry注册Mapper接口
					String mapperPackage = child.getStringAttribute("name");
					// configuration.addMappers(mapperPackage);
				} else {
					// 获取<mapper>节点的resource、url、class属性，这三个属性互斥
					String resource = child.getStringAttribute("resource");
					String url = child.getStringAttribute("url");
					String mapperClass = child.getStringAttribute("class");
					if (resource != null && url == null && mapperClass == null) {
						InputStream inputStream = Resources.getResourceAsStream(resource);
					} else if (resource == null && url != null && mapperClass == null) {
						InputStream inputStream = Resources.getUrlAsStream(url);
					} else if (resource == null && url == null && mapperClass != null) {
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						
					} else {
						// 假如三个属性都没有，或者同时设了两个属性，那么都会走到这里抛出异常
						throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}
 }