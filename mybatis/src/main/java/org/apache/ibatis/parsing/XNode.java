package org.apache.ibatis.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XNode的作用:
 * (1) 封装了Node对象
 * (2) 封装了获取节点名称，节点内容，节点属性等操作
 * (3) 传入的variables(根据解析*.properties文件获取)辅助解析节点属性的键值对并存储在attributes中
 * (4) 内嵌外部传入入的XPathParser对象，利用其解析XPath表达式
 * @author ahao
 *
 */
public class XNode {
	/**
	 * eg: <A attr1="val1" attr2="val2">body</A>
	 */	
	private Node node;                // org.w3c.dom.Node对象
	private String name;              // 节点名
	private String body;              // 节点内容，只处理文本节点，非文本节点body为空
	private Properties attributes;    // 节点属性集合
	private Properties variables;     // mybatis-config.xml配置文件中<properties>节点下定义的键值对
	private XPathParser xpathParser;  // XPath解析器，XNode对象由XPathParser对象生成
	
	public XNode(XPathParser xpathParser, Node node, Properties variables) {
		this.xpathParser = xpathParser;
		this.node = node;
		this.name = node.getNodeName();
		this.variables = variables;	
		this.body = parseBody(node);
		this.attributes = parseAttributes(node);
	}
	
	public XNode newXNode(Node node) {
		return new XNode(xpathParser, node, variables);
	}
	
	// 获取当前节点的父节点
	public XNode getParent() {
		Node parent = node.getParentNode();
		if (parent == null || !(parent instanceof Element)) {
			return null;
		} else {
			return new XNode(xpathParser, parent, variables);
		}
	}
	
	// 获取节点路径
	public String getPath() {
		StringBuilder builder = new StringBuilder();
		Node current = node;
		while (current != null && current instanceof Element) {
			if (current != node) {
				builder.insert(0, "/");
			}
			builder.insert(0, current.getNodeName());
			current = current.getParentNode();
		}
		return builder.toString();
	}
	
	// 获取节点值的识别码,优先级: id > value > property
	public String getValueBasedIdentifier() {
		StringBuilder builder = new StringBuilder();
		XNode current = this;
		while (current != null) {
			if (current != this) {
				builder.insert(0, "_");
			}
			String value = current.getStringAttribute("id", 
							current.getStringAttribute("value", 
							 current.getStringAttribute("property", null)));
			if (value != null) {
				value = value.replace('.', '_');
				builder.insert(0, "]");
				builder.insert(0, value);
				builder.insert(0, "[");
			}
			builder.insert(0, current.getName());
			current = current.getParent();
		}
		return builder.toString();
	}
	
	// eval*()系列方法: 调用XPathParser方法在当前节点下寻找符合表达式条件的节点
	// 【支持数据类型】 String、Boolean、Double、Node、List<Node>
	//            为啥没有short、int、long、float?
	// [start]
	public String evalString(String expression) {
		return xpathParser.evalString(node, expression);
	}
	
	public Boolean evalBoolean(String expression) {
		return xpathParser.evalBoolean(node, expression);
	}
	
	public Double evalDouble(String expression) {
		return xpathParser.evalDouble(node, expression);
	}
	
	public XNode evalNode(String expression) {
		return xpathParser.evalNode(node, expression);
	}
	
	public List<XNode> evalNodes(String expression) {
		return xpathParser.evalNodes(node, expression);
	}	
	// [end]
	
	public Node getNode() {
		return node;
	}
	
	public String getName() {
		return name;
	}
	
	// get*Body()系列方法: 获取文本节点内容并将其转化为指定的数据类型
	// 【支持的数据类型】 String、Boolean、Integer、Long、Double、Float
	// PS. 假如文本节点为带占位符的字符串，则body内容不会解析该字符串
	// [start]
	private String parseBody(Node node) {
		String data = getBodyData(node);
		/**
		 *  如果当前节点不是文本节点，证明该节点下还有子节点
		 *  eg:
		 *  	<A><B>body</B></A>    ==> data = "body"
		 *  如果存在多个子节点，则取第一个子节点是文本节点的子节点
		 *  eg: data = "cbody"
		 *      <A>
		 *        <B att="val"/>
		 *        <C>cbody</C>
		 *        <D>dbody</D>
		 *      </A>
		 */
		if (data == null) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				data = getBodyData(child);
				if (data != null) {
					break;
				}				
			}			
		}
		return data;
	}
	
	private String getBodyData(Node child) {
		/**
		 *  只处理文本类型的节点: Node.CDATA_SECTION_NODE、Node.TEXT_NODE
		 *   			   Node.COMMENT_NODE(不需要判断，因为XPathParser加载XML时已经设置了忽略注释)
		 */
		if (child.getNodeType() == Node.CDATA_SECTION_NODE ||
			child.getNodeType() == Node.TEXT_NODE) {
			String data = ((CharacterData) child).getData();
			data = PropertyParser.parse(data, variables);
			return data;
		}
		return null;
	}	
	
	public String getStringBody() {
		return getStringBody(null);
	}
	
	public String getStringBody(String def) {
		if (body == null) {
			return def;
		} else {
			return body;
		}
	}
	
	public Boolean getBooleanBody() {
		return getBooleanBody(null);
	}
	
	public Boolean getBooleanBody(Boolean def) {
		if (body == null) {
			return def;
		} else {
			return Boolean.valueOf(body);
		}
	}
	
	public Integer getIntBody() {
		return getIntBody(null);
	}
	
	public Integer getIntBody(Integer def) {
		if (body == null) {
			return def;
		} else {
			return Integer.parseInt(body);
		}
	}
	
	public Long getLongBody() {
		return getLongBody(null);
	}
	
	public Long getLongBody(Long def) {
		if (body == null) {
			return def;
		} else {
			return Long.parseLong(body);
		}
	}
	
	public Double getDoubleBody() {
		return getDoubleBody(null);
	}
	
	public Double getDoubleBody(Double def) {
		if (body == null) {
			return def;
		} else {
			return Double.parseDouble(body);
		}
	}
	
	public Float getFloatBody() {
		return getFloatBody(null);
	}
	
	public Float getFloatBody(Float def) {
		if (body == null) {
			return def;
		} else {
			return Float.parseFloat(body);
		}
	}
	// [end]
	
	// get*Attribute()系列方法: 获取节点指定属性的属性值并将其转化为指定的数据类型
	// 【支持的数据类型】 Enum、String、Boolean、Integer、Long、Double、Float
	// [start]
	// 解析节点属性键值对，并将其放入Properties对象中，对外提供根据属性名差属性值功能时用到
	private Properties parseAttributes(Node node) {
		Properties attributes = new Properties();
		NamedNodeMap attributeNodes = node.getAttributes();
		if (attributeNodes != null) {
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				Node attribute = attributeNodes.item(i);
				String value = PropertyParser.parse(attribute.getNodeValue(), variables);
				attributes.put(attribute.getNodeName(), value);
			}
		}
		return attributes;
	}	
	
	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
		return getEnumAttribute(enumType, name, null);
	}
	
	// 有些属性值是有字典的，如果想以枚举类的方式获取属性值，则调用本方法
	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
		String value = getStringAttribute(name);
		if (value == null) {
			return def;
		} else {
			return Enum.valueOf(enumType, value);
		}
	}
	
	// 获取属性值，如果没有返回null
	public String getStringAttribute(String name) {
		return getStringAttribute(name, null);
	}
	
	// 获取属性是，没有没有使用默认值
	public String getStringAttribute(String name, String def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return value;
		}
	}	
	
	public Boolean getBooleanAttribute(String name) {
		return getBooleanAttribute(name, null);
	}
	
	public Boolean getBooleanAttribute(String name, Boolean def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Boolean.valueOf(value);
		}
	}
	
	public Integer getIntAttribute(String name) {
		return getIntAttribute(name, null);
	}
	
	public Integer getIntAttribute(String name, Integer def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Integer.parseInt(value);
		}
	}	
	
	public Long getLongAttribute(String name) {
		return getLongAttribute(name, null);
	}
	
	public Long getLongAttribute(String name, Long def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Long.parseLong(value);
		}
	}
	
	public Double getDoubleAttribute(String name) {
		return getDoubleAttribute(name, null);
	}
	
	public Double getDoubleAttribute(String name, Double def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Double.parseDouble(value);
		}
	}
	
	public Float getFloatAttribute(String name) {
		return getFloatAttribute(name, null);
	}
	
	public Float getFloatAttribute(String name, Float def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Float.parseFloat(value);
		}
	}
	// [end]
	
	// 获取子节点，对Node.getChildNodes()做相应的封装得到List<XNode>
	public List<XNode> getChildren() {
		List<XNode> children = new ArrayList<XNode>();
		NodeList nodeList = node.getChildNodes();
		if (nodeList != null) {
			// 我的写法
			/*for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node instanceof Element) {
					children.add(new XNode(xpathParser, node, variables));
				}
			}*/
			
			// 源码的写法
			for (int i = 0, n = nodeList.getLength(); i < n; i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					children.add(new XNode(xpathParser, node, variables));
				}
			}
		}
		return children;
	}
	
	// 获取所有子节点的name、value属性键值对
	public Properties getChildrenAsProperties() {
		Properties properties = new Properties();
		for (XNode child : getChildren()) {
			String name = child.getStringAttribute("name");
			String value = child.getStringAttribute("value");
			if (name != null && value != null) {
				properties.put(name, value);
			}
		}
		return properties;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<");
		builder.append(name);
		// eg: <setting name="logImpl" value="log4j" />
		for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
			builder.append(" ");
			builder.append(entry.getKey());
			builder.append("=\"");
			builder.append(entry.getValue());
			builder.append("\"");
		}
		// eg: <A><B>bval</B><C>cval</C></A>
		List<XNode> children = getChildren();
		if (!children.isEmpty()) {
			builder.append(">\n");
			for (XNode node : children) {				
				builder.append(node.toString());   // 子节点嵌套调用XNode.toString()
			}
			builder.append("</");
			builder.append(name);
			builder.append(">");
		} 
		// 没有子元素节点，但非单标签节点(eg: <A/>)，有节点内容
		// eg: <A>val</A>
		else if (body != null) {
			builder.append(">");
			builder.append(body);
			builder.append("</");
			builder.append(name);
			builder.append(">");			
		} 
		// eg: <properties resource="jdbc.properties"/>
		else {
			builder.append("/>");
		}
		builder.append("\n");
		return builder.toString();
	}
}
