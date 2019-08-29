package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * BaseBuilder的子类
 * 负责解析Mapper.xml映射配置文件
 * 一个XMLMapperBuilder的解析过程就是对一个Mapper.xml的解析过程
 */
public class XMLMapperBuilder extends BaseBuilder {

	private XPathParser parser;
	private MapperBuilderAssistant builderAssistant;
	private Map<String, XNode> sqlFragments;         // 可重用<sql>节点id和XNode映射集合
	private String resource;
	
	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
	    this(reader, configuration, resource, sqlFragments);
	    this.builderAssistant.setCurrentNamespace(namespace);
	}

	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
	    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
	        configuration, resource, sqlFragments);
	}	
	
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
	    this(inputStream, configuration, resource, sqlFragments);
	    this.builderAssistant.setCurrentNamespace(namespace);
	}	
	
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()), 
			 configuration, resource, sqlFragments);
	}
	
	public XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		super(configuration);
		this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
		this.parser = parser;
		this.sqlFragments = sqlFragments;
		this.resource = resource;
	}

	public void parse() {
		// 判断是否已经加载过该映射文件
		if (!configuration.isResourceLoaded(resource)) {
			configurationElement(parser.evalNode("/mapper"));  // 处理<Mapper>节点
			// 将resource添加到Configuration.loadedResources集合中保存，它是HashSet<String>
			// 类型的集合，其中记录了已经加载过的映射文件，避免重复加载
			configuration.addLoadedResource(resource);
		}
	}
	
	/**
	 * 这里的context是mybatis-config.xml中的<mapper>标签配置的xml文件
	 * eg:
	 * <mapper resource="com/learn/ssm/chapter5/mapper/RoleMapper.xml"/>
	 */
	private void configurationElement(XNode context) {
		try {
			// 获取<mapper>节点对应的mapper.xml中的namespace属性，这是必设的，所以不设会抛出异常
			// 一般namespace是Mapper接口的全限定名
			String namespace = context.getStringAttribute("namespace");
			if (namespace == null || namespace.equals("")) {
				throw new BuilderException("Mapper's namespace cannot be empty");
			}
			// 设置MapperBuilderAssistant的currentNamespace字段，记录当前命名空间
			builderAssistant.setCurrentNamespace(namespace);
			// 解析 <cache-ref> 节点
			cacheRefElement(context.evalNode("cache-ref"));
			// 解析 <cache> 节点
			cacheElement(context.evalNode("cache"));
			// 解析 <parameterMap> 节点（官方声明已作废！）
			parameterMapElement(context.evalNodes("/mapper/parameterMap"));
			// 解析 <resultMap> 节点
			resultMapElements(context.evalNodes("/mapper/resultMap"));
			// 解析 <sql> 节点
			sqlElement(context.evalNodes("/mapper/sql"));
			// 解析 <select>、<insert>、<update>、<delete> 等SQL节点
			buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing Mapper XML. Cause: " + e, e);
		}
	}
	
	/**
	 * [缓存配置引用]
	 * 	如果某个mapper.xml想用跟另外一个mapper.xml相同的缓存配置，则使用<cache-ref>标签
	 * 
	 * 示例配置:
	 * 	<cache-ref namespace="com.ssm.chapter5.mapper.RoleMapper">
	 */
	private void cacheRefElement(XNode context) {
		if (context != null) {
			// 将当前Mapper配置文件的namespace与被引用的Cache所在的namespace之间的对应关系
			// 记录到Configuration.cacheRefMap集合中
			configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
			// 创建CacheRefResolver对象
			CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
			try {
				// 解析Cache引用，该过程主要是设置MapperBuilderAssistant中的currentCache和unresolvedCacheRef字段
				cacheRefResolver.resolveCacheRef();
			} catch (IncompleteElementException e) {
				// 如果解析不到引用的namespace的缓存，会抛出异常，将异常的CacheRefResolver加入configuration的incompleteCacheRefs集合中
				configuration.addIncompleteCacheRef(cacheRefResolver);
			}
		}
	}
	
	/**
	 * [缓存配置]
	 * 
	 * MyBatis有二级缓存机制，默认只开启一级缓存即SqlSession级别的缓存，二级缓存是SqlSessionFactory级别的缓存
	 * Q: 区别?
	 * A: 一级缓存是针对同一个SQL会话级别的，比如在同一个SqlSession中使用相同的条件执行了一次查询操作，第二次查询时则直接从缓存中获取，而不去实际连接数据库查询
	 *    二级缓存是针对SqlSessionFactory级别的，让不同的SqlSession之间可以共享缓存，在A SqlSession执行了一次查询，B SqlSessionz执行相同的查询时直接去二级缓存中获取
	 *    
	 * <cache>元素的配置属性项列举如下:
	 *  -------------------------------------------------------------------------------------------------------------
 	 * |   属性                         |                说        明                                                   |        取         值                         |    备    注                                                                       |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | blocking     | 是否使用阻塞性缓存，在读/写 时它会加入JNI的锁进行操作   | true|false,默认值false | 可保证读写安全性，但加锁后性能不佳                        |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | readOnly     | 缓存内容是否只读                                                                                | true|false,默认值false | 如果为只读，则不会因为多个线程读写造成不一致性 |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | eviction     | 缓存策略，分为:                           |                      |                                |
	 * |              | LRU 最近最少使用：移除最长时间不被使用的对象                    |                      |                                |
	 * |              | FIFO 先进先出：按对象进入缓存的顺序来移除它们                 |       默认值是LRU       |            ———                 |
	 * |              | SOFT 软引用：移除基于垃圾回收器状态和软引用规则的对象 |                      |                                |
	 * |              | WEAK 弱引用：更积极移除基于垃圾收集器状态和弱引用规则 |                      |                                |
	 * |              |           的对象                                                                      |                      |                                |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | flushInteval | 这是一个整数，它以毫秒未单位，比如1分钟刷新一次，则配置 |                      | 超过整数后缓存失效，不再读取缓存，而执行SQL |
	 * |              | 60000。默认为null，也就是没有刷新时间，只有当执行     |        正整数                              | 取回数据                                                                            |
	 * |              | update时，insert和delete语句才会刷新                      |                      |                                |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | type         | 自定义缓存类。要求实现接口                                                            | 用于自定义缓存类                               |            ————                |
	 * |              | org.apache.ibatis.cache.Cache        |                      |                                |
	 * |-------------------------------------------------------------------------------------------------------------|
	 * | size         | 缓存对象个数                                                                                       | 正整数，默认值是1024       |            ————               |
	 *  -------------------------------------------------------------------------------------------------------------
	 *  
	 *  配置示例:
	 *  <cache type="org.apache.ibatis.cache.Cache"
	 *      blocking="false" readOnly="false" eviction="LRU" flushInteval="" size="1024">
	 *  	<property name="xxx" value="yyy"/>
	 *  </cache>
	 */
	private void cacheElement(XNode context) throws Exception {
		if (context != null) {
			// 获取<cache> 节点的type属性，默认值是PERPETUAL
			String type = context.getStringAttribute("type", "PERPETUAL");
			// 查找type属性对应的Cache接口实现
			Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
			// 获取<cache> 节点的eviction属性，默认值是LRU
			String eviction = context.getStringAttribute("eviction", "LRU");
			// 解析eviction属性指定的Cache装饰器类型
			Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
			// 获取<cache> 节点的flushInterval属性，默认值是null
			Long flushInterval = context.getLongAttribute("flushInterval");
			// 获取<cache> 节点的size属性，默认值是null
			Integer size = context.getIntAttribute("size");
			// 获取<cache> 节点的readOnly属性，默认值是false
			boolean readWrite = !context.getBooleanAttribute("readOnly", false);
			// 获取<cache> 节点的blocking属性，默认值是false
			boolean blocking = context.getBooleanAttribute("blocking", false);
			// 获取<cache> 节点下的子节点，用于初始化二级缓存
			Properties props = context.getChildrenAsProperties();
			// 通过MapperBuilderAssistant创建Cache对象，并添加到Configuration.caches集合中保存
			builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
		}
	}

	/**
	 * [解析parameterMap节点]
	 * <parameterMap id="" type="">
	 *     <parameter property="" javaType="" jdbcType="" resultMap="" mode="" typeHandler="" numericScale=""></parameter>
	 *     <parameter></parameter>
	 * </parameterMap>
	 */
	private void parameterMapElement(List<XNode> list) {
		for (XNode parameterMapNode : list) {
			String id = parameterMapNode.getStringAttribute("id");
			String type = parameterMapNode.getStringAttribute("type");
			Class<?> parameterClass = resolveClass(type);
			List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			for (XNode parameterNode : parameterNodes) {
				String property = parameterNode.getStringAttribute("property");
				String javaType = parameterNode.getStringAttribute("javaType");
				String jdbcType = parameterNode.getStringAttribute("jdbcType");
				String resultMap = parameterNode.getStringAttribute("resultMap");
		        String mode = parameterNode.getStringAttribute("mode");
		        String typeHandler = parameterNode.getStringAttribute("typeHandler");
		        Integer numericScale = parameterNode.getIntAttribute("numericScale");
		        ParameterMode modeEnum = resolveParameterMode(mode);
		        Class<?> javaTypeClass = resolveClass(javaType);
		        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		        @SuppressWarnings("unchecked")
		        Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
		        parameterMappings.add(parameterMapping);
			}
			builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
		}
	}
	
	// 同个mapper.xml中可能存在多个 <resultMap> 节点，所以这里的参数是一个List
	private void resultMapElements(List<XNode> list) throws Exception {
		for (XNode resultMapNode : list) {
			try {
				resultMapElement(resultMapNode);
			} catch (IncompleteElementException e) {
			}
		}
	}
	
	private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
		return resultMapElement(resultMapNode, Collections.<ResultMapping>emptyList());
	}
	
	/**
	 * [解析一个 <ResultMap> 节点]
	 * 
	 * 元素组成:
	 * 	<resultMap>
	 * 		<constructor>
	 * 			<idArg/>
	 *          <arg/>
	 *      </constructor>
	 *      <id/>
	 *      <result/>
	 *      <association/>
	 *      <collection/>
	 *      <discriminator>
	 *      	<case/>
	 *      </discriminator>
	 *  </resultMap>
	 * 
	 * 配置示例: (包含了一对一级联、一对多级联、鉴别器)
	 *  <mapper namespace="com.learn.ssm.chapter5.mapper.EmployeeMapper">
	 * 	<resultMap type="employee" id="employee">
	 *		<id column="id" property="id"/>
	 *		<result column="real_name" property="realName"/>
	 *		<result column="sex" property="sex" typeHandler="com.learn.ssm.chapter5.typeHandler.SexTypeHandler"/>
	 *		<result column="birthday" property="birthday"/>
	 *		<result column="mobile" property="mobile"/>
	 *		<result column="email" property="email"/>
	 *		<result column="position" property="position"/>
	 *		<result column="note" property="note"/>
	 *		<!-- 一对一级联 -->
	 *		<association column="id" property="workCard" select="com.learn.ssm.chapter5.mapper.WorkCardMapper.getWorkCardByEmpId"/>
	 *		<!-- 一对多级联 -->
	 *		<collection column="id" property="employeeTaskList" fetchType="eager" select="com.learn.ssm.chapter5.mapper.EmployeeTaskMapper.getEmployeeTaskByEmpid"/>
	 *		<!-- 鉴别器 --
	 *		<discriminator javaType="long" column="sex">
	 *			<case value="1" resultMap="maleHealthFormMapper" />
	 *			<case value="0" resultMap="femaleHealthFormMapper" />			
	 *		</discriminator>
	 *  </resultMap>
	 *  </mapper>
	 */
	private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
		// 一般resultMap都会设置id，如果没有id，则用节点唯一标识字符串，比如上面的唯一标识为: mapper_resultMap[employee]
		String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
		// 获取 <resultMap> 节点的 type 属性，表示结果集将被映射成 type 指定类型的对象
		// 取值优先级为: type > ofType > returnType > javaType
		String type = resultMapNode.getStringAttribute("type", 
						resultMapNode.getStringAttribute("ofType", 
						  resultMapNode.getStringAttribute("returnType", 
							resultMapNode.getStringAttribute("javaType"))));
		// 获取 <resultMap> 节点的 extends 属性，该属性指定了该 <resultMap> 节点的继承关系
		// 比如有个类A，对应的<resultMap id="a">，类B继承了A，在写B的<resultMap extends="a"> 时用extends属性指明该<resultMap> 继承了哪个id="a"的<resultMap>
		String extend = resultMapNode.getStringAttribute("extends");
		// 读取 <resultMap> 节点的 autoMapping 属性，并将该属性设置为true，则启动自动映射功能
		// 即自动查找与列名同名的属性名，并调用setter方法。而设置为false后，则需要在 <resultMap>
		// 节点内明确注明映射关系才会调用对应的setter方法
		Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
		Class<?> typeClass = resolveClass(type);  // 解析type类型
		Discriminator discriminator = null;
		// 该集合用于记录解析的结果
		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
		resultMappings.addAll(additionalResultMappings);
		// 处理 <resultMap> 的子节点得到ResultMapping集合
		List<XNode> resultChildren = resultMapNode.getChildren();
		for (XNode resultChild : resultChildren) {
			// <constructor> 元素用来映射resultMap对应的type的类中的构造函数
			if ("constructor".equals(resultChild.getName())) {
				// 处理<construct> 节点
				processConstructorElement(resultChild, typeClass, resultMappings);
			} else if ("discriminator".equals(resultChild.getName())) {
				// 处理 <discriminator> 子节点
				discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
			} else {
				// 处理 <id>、<result>、<association>、<collection>等节点
				List<ResultFlag> flags = new ArrayList<ResultFlag>();
				if ("id".equals(resultChild.getName())) {
					flags.add(ResultFlag.ID);
				}
				resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
			}
		}
		ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
		try {
			return resultMapResolver.resolve();
		} catch (IncompleteElementException e) {
			//configuration.addIncompleteCacheRef(incompleteCacheRef);
			throw e;
		}
	}
	
	/**
	 * 解析<constructor> 节点
	 * 
	 * 元素组成:
	 * 	   <constructor>
	 * 			<idArg/>
	 *          <arg/>
	 *     </constructor>
	 * 
	 * 示例配置:
	 * 	   <constructor>
	 * 	 	  <idArg column="blog_id" javaType="int"/>
	 *     </constructor>
	 */
	private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		List<XNode> argChildren = resultChild.getChildren();   // 获取<constructor> 节点的子节点
		for (XNode argChild : argChildren) {
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			flags.add(ResultFlag.CONSTRUCTOR);                 // 添加CONSTRUCTOR标志
			if ("idArg".equals(argChild.getName())) {
				flags.add(ResultFlag.ID);                      // 对于<idArg>节点，添加ID标志
			}
			// 为 <idArg> 或 <arg> 创建对应的ResultMapping对象，并添加到resultMappings集合中
			resultMappings.add(buildResultMappingFromContext(resultChild, resultType, flags));
		}
	}
    
	/**
	 * 解析<discriminator> 鉴别器节点
	 * 
	 * 配置示例:
	 *  <discriminator javaType="long" column="sex">
	 *		<case value="1" resultMap="maleHealthFormMapper" />
	 *		<case value="0" resultMap="femaleHealthFormMapper" />			
	 *	</discriminator>
	 */
	private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String typeHandler = context.getStringAttribute("typeHandler");
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		Map<String, String> discriminatorMap = new HashMap<String, String>();
		for (XNode caseChild : context.getChildren()) {
			String value = caseChild.getStringAttribute("value");
			String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
			discriminatorMap.put(value, resultMap);
		}
		return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
	}
	
	// 解析<sql>节点
	private void sqlElement(List<XNode> list) throws Exception {
		if (configuration.getDatabaseId() != null) {
			sqlElement(list, configuration.getDatabaseId());
		}
		sqlElement(list, null);
	}
	
	private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {		
		for (XNode context : list) {  // 遍历<sql>节点
			// 获取databaseId属性
			String databaseId = context.getStringAttribute("databaseId");
			String id = context.getStringAttribute("id");   // 获取id属性
			id = builderAssistant.applyCurrentNamespace(id, false);  // 为id添加命名空间
			if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
				sqlFragments.put(id, context);
			}
		}
	}
	
	// 检车<sql>节点配置的databaseId是否与全局配置的databaseId匹配，匹配则注册使用返回true，否则返回false
	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		// Configuration配置了databaseId时，sql必须配置databaseId并与全局配置一致
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			// 如果全局配置没有配databaseId但是sql节点配了，则返回false
			if (databaseId != null) {
				return false;
			}
			if (this.sqlFragments.containsKey(id)) {
				XNode context = this.sqlFragments.get(id);
				if (context.getStringAttribute("databaseId") != null) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
     * eg:
     * 	 class A {            class B {
     * 	    String id;        	 int num;
     * 	 	String name;         double price;
     *      B b;                 String aid;
     *   }                    }
     * 类A对应Amapper.xml，类B对应Bmapper.xml，A中有类型为B的属性b，假如A和B是一对一关系
     * 在Amapper.xml中当执行一个查询操作要将查询结果映射成一个A对象时，为了将b属性也映射成B对象，
     * 需要在Amapper.xml中的<resultMap> 节点中引用类B的一些信息，具体做法有以下两种。
     * 
     * 结果映射一般有两种写法:
     * (1) 通过resultMap属性: 这种需要在同个mapper.xml中声明多个<resultMap> 节点提供引用
     * eg: 在Amapper.xml中定义类B的<resultMap> 节点，并在表示类A的<resultMap> 节点中通过id引用它
     *     <resultMap id="aaa" type="xxx.xxx.A">                        <resultMap id="bbb" type="xxx.xxx.B">
     *         <id column="id" property="id">                           	<result column="b_num" property="num"/>
     *         <result column="name" property="name"/>                      <result column="b_price" property="price"/>
     *         <association property="b" resultMap="bbb"/>                  <result column="b_aid" property="aid"/>
     *     </resultMap>                                                 </resultMap>
     * [限制要求] 一般这种写法在连表查询时才有用，查询出来的类B对应的B表的列名必须转化为<resultMap id="bbb">中的列名
     *         一般我们不用联表查询，联表查询意味着性能下降，而且将业务处理的逻辑放在DB端，降低了灵活性，增加了开发工作量，不划算
     *         
     * (2) 通过select属性: 需要在嵌套节点中写完成引用的类对应的Mapper接口方法和查询参数
     * eg: 在Amapper.xml中类A的<resultMap> 节点中，将resultMap属性改成select属性
     *     <resultMap id="aaa" type="xxx.xxx.A">
     *         <id column="id" property="id">
     *         <result column="name" property="name"/>
     *         <association column="id" property="b" select="xxx.xxx.BMapper.getBByAid"/>
     *     </resultMap>
     * [说明] 属性值是类B对应的Mapper接口方法，假设BMapper.java代码如下:
     *     public interface BMapper {
     *         public A getBByAid(Long id);   // 跟Bmapper.xml中的id="getBByAid"的SQL对应
     *     }
     * [优点] 不用在Amapper.xml中声明B的<resultMap>，查询SQL时可以不用马上查询B的数据，方便对对象B的数据查询和对象构建的懒加载
     *       开发工作量相对小，灵活性好
     */	
	private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
	    String property = context.getStringAttribute("property");
	    String column = context.getStringAttribute("column");
	    String javaType = context.getStringAttribute("javaType");
	    String jdbcType = context.getStringAttribute("jdbcType");
	    String nestedSelect = context.getStringAttribute("select");
	    // 如果未指定<association> 节点的resultMap属性，则是匿名的嵌套映射，需要通过
	    // processNestedResultMappings()方法解析该匿名的嵌套映射
	    String nestedResultMap = context.getStringAttribute("resultMap", 
	    		processNestedResultMappings(context, Collections.<ResultMapping>emptyList()));
	    String notNullColumn = context.getStringAttribute("notNullColumn");   // 是否非空列
	    String columnPrefix = context.getStringAttribute("columnPrefix");     // 列前缀
	    String typeHandler = context.getStringAttribute("typeHandler");       // 类型转换器
	    String resultSet = context.getStringAttribute("resultSet");            
	    String foreignColumn = context.getStringAttribute("foreignColumn");   // 是否为外键 
	    boolean lazy = "lazy".equals(context.getStringAttribute("fethType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));  // 是否延时加载
	    Class<?> javaTypeClass = resolveClass(javaType);
	    @SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
	    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
	    // 创建ResultMapping对象
	    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, 
	    										   notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
	}
	
	/**
	 * 解析嵌套的ResultMap，当找不到resultMap属性时，解析是否存在select属性
	 * 
	 * 示例配置:
	 * 	 <select id="xxx" resultMap="yyy">...</select>	
	 *   <association property="author" resultMap="authorResult"/>
	 *   <case value="1" resultMap="maleHealthFormMapper" />
	 */
	private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
		// 只会处理<association>、<collection>、<case>三种节点
		if ("association".equals(context.getName()) ||
			"collection".equals(context.getName()) ||
			"case".equals(context.getName())) {
			// 指定了select属性后，不会生成嵌套的ResultMap对象
			if (context.getStringAttribute("select") == null) {
				// 创建ResultMap对象，并添加到Configuration.resultMaps集合中
				ResultMap resultMap = resultMapElement(context, resultMappings);  // 递归入口
				return resultMap.getId();
			}
		} 
		return null;
	}

	// 通过mapper.xml的上下文构建Statament对象
	private void buildStatementFromContext(List<XNode> list) {
		if (configuration.getDatabaseId() != null) {
			buildStatementFromContext(list, configuration.getDatabaseId());
		}
		buildStatementFromContext(list, null);
	}
	
	private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
		for (XNode context : list) {
			final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
			try {
				statementParser.parseStatementNode();	
			} catch (IncompleteElementException e) {
				configuration.addIncompleteStatement(statementParser);
			}
		}
	}
}