package org.apache.ibatis.parsing;

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
	private String body;              // 节点内容，只处理文本节点
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
		if (current != null && current instanceof Element) {
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
	
	public String getName() {
		return name;
	}
	
	public String getStringAttribute(String name) {
		return getStringAttribute(name, null);
	}
	
	public String getStringAttribute(String name, String def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return value;
		}
	}
	
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
		return null;
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
	
	private Properties parseAttributes(Node node) {
		Properties attributes = new Properties();
		NamedNodeMap attributeNodes = node.getAttributes();
		if (attributeNodes != null) {
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				Node attribute = attributeNodes.item(i);
				String value = PropertyParser.parse(attribute.getNodeValue(), variables);
				attributes.put(attribute.getNodeValue(), value);
			}
		}
		return attributes;
	}
}
