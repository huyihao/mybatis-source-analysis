package org.apache.ibatis.builder.xml;

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * 负责解析SQL相关节点的构造器
 *
 */
public class XMLStatementBuilder extends BaseBuilder {

	private MapperBuilderAssistant builderAssistant;
	private XNode context;
	private String requiredDatabaseId;
	
	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
		this(configuration, builderAssistant, context, null);
	}

	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
		super(configuration);
		this.builderAssistant = builderAssistant;
		this.context = context;
		this.requiredDatabaseId = databaseId;
	}
	
	/**
	 * [配置示例]
	 * <select id="selectPerson"             // 在命名空间中唯一的标识符，可以被用来引用这条语句。
	 *		   parameterType="int"           // 将会传入这条语句的参数类的完全限定名或别名。这个属性是可选的，因为 MyBatis 可以通过类型处理器（TypeHandler） 推断出具体传入语句的参数，默认值为未设置（unset）。
	 *		   parameterMap="deprecated"     // 这是引用外部 parameterMap 的已经被废弃的方法。请使用内联参数映射和 parameterType 属性。
	 *		   resultType="hashmap"          // 从这条语句中返回的期望类型的类的完全限定名或别名。 注意如果返回的是集合，那应该设置为集合包含的类型，而不是集合本身。可以使用 resultType 或 resultMap，但不能同时使用。
	 *		   resultMap="personResultMap"   // 外部 resultMap 的命名引用。结果集的映射是 MyBatis 最强大的特性，如果你对其理解透彻，许多复杂映射的情形都能迎刃而解。可以使用 resultMap 或 resultType，但不能同时使用。
	 *		   flushCache="false"            // 将其设置为 true 后，只要语句被调用，都会导致本地缓存和二级缓存被清空，默认值：false。
	 *		   useCache="true"               // 将其设置为 true 后，将会导致本条语句的结果被二级缓存缓存起来，默认值：对 select 元素为 true。
	 *         timeout="10"                  // 这个设置是在抛出异常之前，驱动程序等待数据库返回请求结果的秒数。默认值为未设置（unset）（依赖驱动）。
	 *		   fetchSize="256"               // 这是一个给驱动的提示，尝试让驱动程序每次批量返回的结果行数和这个设置值相等。 默认值为未设置（unset）（依赖驱动）。
	 *		   statementType="PREPARED"      // STATEMENT，PREPARED 或 CALLABLE 中的一个。这会让 MyBatis 分别使用 Statement，PreparedStatement 或 CallableStatement，默认值：PREPARED。
	 *		   resultSetType="FORWARD_ONLY"  // FORWARD_ONLY，SCROLL_SENSITIVE, SCROLL_INSENSITIVE 或 DEFAULT（等价于 unset） 中的一个，默认值为 unset （依赖驱动）。
	 *         databaseId="MYSQL"            // 如果配置了数据库厂商标识（databaseIdProvider），MyBatis 会加载所有的不带 databaseId 或匹配当前 databaseId 的语句；如果带或者不带的语句都有，则不带的会被忽略。
	 *         resultOrdered="false"         // 这个设置仅针对嵌套结果 select 语句适用：如果为 true，就是假设包含了嵌套结果集或是分组，这样的话当返回一个主结果行的时候，就不会发生有对前面结果集的引用的情况。 这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
	 *         resultSets=""                 // 这个设置仅对多结果集的情况适用。它将列出语句执行后返回的结果集并给每个结果集一个名称，名称是逗号分隔的。
	 * </select>
	 */
	public void parseStatementNode() {
		// 获取SQL节点的id以及databaseId属性，若其databaseId属性值与当前使用的数据库不匹配，则不加载该SQL节点；
		// 若存在相同id且databaseId不为空的SQL节点，则不再加载该SQL节点
		String id = context.getStringAttribute("id");
		String databaseId = context.getStringAttribute("databaseId");
		
		if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
			return;
		}

		// 获取SQL节点的多种属性，例如：fetchSize、timeout、parameterMap、parameterType、resultMap、
		// resultType、lang、resultSetType、flushCache、useCache等
		Integer fetchSize = context.getIntAttribute("fetchSize");
		Integer timeout = context.getIntAttribute("timeout");
		String parameterMap = context.getStringAttribute("parameterMap");
		String parameterType = context.getStringAttribute("parameterType");
		Class<?> parameterTypeClass = resolveClass(parameterType);
		String resultMap = context.getStringAttribute("resultMap");
		String resultType = context.getStringAttribute("resultType");
		String lang = context.getStringAttribute("lang");
		LanguageDriver langDriver = getLanguageDriver(lang);	
		
	    Class<?> resultTypeClass = resolveClass(resultType);
	    String resultSetType = context.getStringAttribute("resultSetType");
	    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
	    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);		
	    
	    // 根据SQL节点名称解析Sql指令类型
	    String nodeName = context.getNode().getNodeName();
	    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
	    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
	    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
	    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
	    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);
		
	    // 在解析SQL语句之前，先处理其中的<include>节点
	    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
	    includeParser.applyIncludes(context.getNode());
	    
	    // 在解析完<include>节点后解析<selectKey>节点
	    processSelectKeyNodes(id, parameterTypeClass, langDriver);
	    
	    // 经过上面两步的解析，<include>节点和<selectKey>节点以及被解析并删除掉了，开始正式解析SQL
	    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
	}
	
	// 获取并解析mapper.xml中的全部 <selectKey> 节点
	private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
		// 获取全部的<selectKey>节点
		List<XNode> selectKeyNodes = context.evalNodes("selectKey");
		// 解析<selectKey>节点
		if (configuration.getDatabaseId() != null) {
			parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
		}
		parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
	}
	
	private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
		for (XNode nodeToHandle : list) {
			String id = parentId;
			String databaseId = nodeToHandle.getStringAttribute("databaseId");
			if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
				parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
			}
		}
	}
	
	/**
	 * [配置示例]
	 * <selectKey keyProperty="id"           // selectKey 语句结果应该被设置的目标属性。如果希望得到多个生成的列，也可以是逗号分隔的属性名称列表。
	 *            keyColumn="col1,col2"      // 匹配属性的返回结果集中的列名称。如果希望得到多个生成的列，也可以是逗号分隔的属性名称列表。
  	 *            resultType="int"           // 结果的类型。MyBatis 通常可以推断出来，但是为了更加精确，写上也不会有什么问题。MyBatis 允许将任何简单类型用作主键的类型，包括字符串。如果希望作用于多个生成的列，则可以使用一个包含期望属性的 Object 或一个 Map。
     *            order="BEFORE"             // 这可以被设置为 BEFORE 或 AFTER。如果设置为 BEFORE，那么它会首先生成主键，设置 keyProperty 然后执行插入语句。如果设置为 AFTER，那么先执行插入语句，然后是 selectKey 中的语句 - 这和 Oracle 数据库的行为相似，在插入语句内部可能有嵌入索引调用。
     * 			  statementType="PREPARED">  // 与前面相同，MyBatis 支持 STATEMENT，PREPARED 和 CALLABLE 语句的映射类型，分别代表 PreparedStatement 和 CallableStatement 类型。
	 */
	private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
		// 获取<selectKey>节点的resultType、statementType、ketProperty、keyColumn、order等属性值
		String resultType = nodeToHandle.getStringAttribute("resultType");
		Class<?> resultTypeClass = resolveClass(resultType);
		StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
		String keyProperty = nodeToHandle.getStringAttribute("ketProperty");
		String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
		boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));
		
		// 设置一系列MappedStatement对象需要的默认配置值
	    boolean useCache = false;
	    boolean resultOrdered = false;
	    KeyGenerator keyGenerator = null;
	    Integer fetchSize = null;
	    Integer timeout = null;
	    boolean flushCache = false;
	    String parameterMap = null;
	    String resultMap = null;
	    ResultSetType resultSetTypeEnum = null;
	    
	    // 通过LanguageDriver.createSqlSource()方法生成SqlSource
	    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
	    // <selectKey>节点中只能配置select语句
	    SqlCommandType sqlCommandType = SqlCommandType.SELECT;
	    
	    // 通过MapperBuilderAssistant创建MappedStatement对象，并添加到Configuration.mappedStatements集合中保存
	    // 该集合为StrictMap<MappedStatement>类型
	    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
	    		fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
	            resultSetTypeEnum, flushCache, useCache, resultOrdered,
	            keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);	
	    id = builderAssistant.applyCurrentNamespace(id, false);
	    
	    // 创建<selectKey>节点对应的 KeyGenerator，添加到Configuration.keyGenerators集合中
	    // 保存，Configuration.keyGenerators字段是StrictMap<KeyGenerator>类型的对象
	    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
	    //configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
	}
	
	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			if (databaseId != null) {
				return false;
			}
			id = builderAssistant.applyCurrentNamespace(id, false);
			if (this.configuration.hasStatement(id, false)) {
				MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
				if (previous.getDatabaseId() != null) {
					return false;
				}
			}
		}
		return true;
	}
	
	private LanguageDriver getLanguageDriver(String lang) {
	    Class<?> langClass = null;
	    if (lang != null) {
	      langClass = resolveClass(lang);
	    }
		return builderAssistant.getLanguageDriver(langClass);
	}
}
