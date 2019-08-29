package org.apache.ibatis.builder.xml;

import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XMLIncludeTransformer
 * [作用]
 *    解析SQL语句中的<include>节点，将<include>节点替换成<sql>节点中定义的SQL片段，
 *    并将其中的 “${xxx}” 占位符替换成真实的参数
 */
public class XMLIncludeTransformer {

	private final Configuration configuration;
	private final MapperBuilderAssistant builderAssistant;
	
	public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
		this.configuration = configuration;
		this.builderAssistant = builderAssistant;
	}
	
	public void applyIncludes(Node source) {
		// 获取mybatis-config.xml中<properties>节点下定义的变量集合
		Properties variablesContext = new Properties();
		Properties configurationVariables = configuration.getVariables();
		if (configurationVariables != null) {
			variablesContext.putAll(configurationVariables);
		}
		applyIncludes(source, variablesContext);
	}
	
	/**
	 * [实例演示]
	 *  <sql id="roleColsVar">
	 *		${alias}.id, ${alias}.role_name, ${alias}.note
	 *  </sql>
	 *  <select>
	 *		select 
	 *			<include refid="roleColsVar">
	 *				<property name="alias" value="r"/>				
	 *			</include>
	 *		from t_role r where id = #{id}
	 *  </select>
	 *  
	 *  applyIncludes():
	 *  [分支1]
	 *  	步骤1：
	 *  		一般<include>引用的<sql>是在同一文件中的，<sql>先于"select|insert|update|delete"节点被解析并将节点加载缓存到Configuration对象的sqlFragments集合中
	 *      	所以获取引用的<sql>节点不管是不是同个文件中，处理过程没什么差异
	 *      
	 *  	步骤2：
	 *  		按照上面的实例，经过步骤2之后newVariablesContext = {alias=r}
	 *      	fullContext = newVariablesContext + myatis-config.xml中引入或定义的Properties
	 *  
	 *  	步骤3：
	 *      	有可能出现这种情况：<sql>column1, column2, <inculde refid="sqlxx"/></sql>
	 *      	这种情况下，递归调用applyIncludes()，方法第二个分支就是处理嵌套节点的情况
	 *      
	 *  	步骤4：
	 *  		将<include>节点替换成<sql>节点
	 *  		<select>
	 *				select 
	 *  				<sql id="roleColsVar">
	 *						${alias}.id, ${alias}.role_name, ${alias}.note
	 *  				</sql>
	 *					from t_role r where id = #{id}
	 *  		</select>
	 *  		将<sql>节点中的值取出来放在<sql>节点前面，并删除<sql>节点
	 *  		<select>
	 *				select 
	 *					${alias}.id, ${alias}.role_name, ${alias}.note
	 *				from t_role r where id = #{id}
	 *  		</select>
	 *  
	 *  [分支2]
	 *      <select>、<insert>、<delete>、<update>节点有嵌套的节点，遍历递归处理
	 *      节点包括属性节点、文本节点、嵌套节点（<A attr="xx">yyy <B/></A>）
	 *  
	 *  [分支3]
	 *  	解析属性节点，就是将属性节点值中的变量替换成属性配置Properties对象中的映射的值
	 *  eg: .. attr="${xxx}" ...
	 *      Properties: {xxx=yyy, ...}
	 *      解析结果: ... attr="yyy" ...
	 *      
	 *  [分支4]
	 *      解析文本节点，将文本节点值中的变量像[分支3]中一样替换
	 *  eg: select ${alias}.id, ${alias}.role_name, ${alias}.note from t_role r where id = #{id}
	 *      Properties: {alias=r, ...}
	 *      解析结果: select r.id, r.role_name, r.note from t_role r where id = #{id}
	 */
	private void applyIncludes(Node source, final Properties variablesContext) {
		if (source.getNodeName().equals("include")) {
			Properties fullContext;
			// 1.查找refid属性指向的<sql>节点，返回的是其深克隆的Node对象
			String refid = getStringAttribute(source, "refid");
			refid = PropertyParser.parse(refid, variablesContext);
			Node toInclude = findSqlFragment(refid);
			
			// 2.解析<include>节点下的<property>节点，将得到的键值对添加到variablesContext中，并形成新的Properties对象返回，用于替换占位符
			Properties newVariablesContext = getVariablesContext(source, variablesContext);
			if (!newVariablesContext.isEmpty()) {
				fullContext = new Properties();
				fullContext.putAll(variablesContext);
				fullContext.putAll(newVariablesContext);
			} else {
				fullContext = variablesContext;
			}
			
			// 3.递归处理<include>节点，在<sql>节点中可能会使用<include>引用了其他SQL片段
			applyIncludes(toInclude, fullContext);
			
			// 如果<include>节点引用的<sql>节点不在同一个xml文件，则为当前配置文件导入该<sql>节点
			if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
				toInclude = source.getOwnerDocument().importNode(toInclude, true);
			}
			
			// 4.将<include>节点替换成<sql>节点
			source.getParentNode().replaceChild(toInclude, source);
		    while (toInclude.hasChildNodes()) {  // 将<sql>节点的子节点添加到<sql>节点前面
		        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
		    }
		    toInclude.getParentNode().removeChild(toInclude);   // 删除<sql>节点
		} else if (source.getNodeType() == Node.ELEMENT_NODE) {
			NodeList children = source.getChildNodes();   // 遍历当前SQL语句的子节点
			for (int i = 0; i < children.getLength(); i++) {
				applyIncludes(children.item(i), variablesContext);
			}
		} else if (source.getNodeType() == Node.ATTRIBUTE_NODE && !variablesContext.isEmpty()) {
			source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
		} else if (source.getNodeType() == Node.TEXT_NODE && !variablesContext.isEmpty()) {
			source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
		}
	}
	
	private Node findSqlFragment(String refid) {
		refid = builderAssistant.applyCurrentNamespace(refid, true);
		try {
			XNode nodeToInclude = configuration.getSqlFragments().get(refid);
			return nodeToInclude.getNode().cloneNode(true);
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'",
					e);
		}
	}
	
	private String getStringAttribute(Node node, String name) {
	    return node.getAttributes().getNamedItem(name).getNodeValue();
	}
	
	private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
		Properties variablesContext = new Properties();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				String name = getStringAttribute(n, "name");
				String value = getStringAttribute(n, "value");
				// Replace variables inside
				value = PropertyParser.parse(value, inheritedVariablesContext);
				// Push new value
				Object originalValue = variablesContext.put(name, value);
				if (originalValue != null) {
					throw new BuilderException("Variable " + name + " defined twice in the same include definition");
				}
			}
		}
		return variablesContext;
	}
}
