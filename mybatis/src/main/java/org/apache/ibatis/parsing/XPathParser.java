package org.apache.ibatis.parsing;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XPathParser封装了以下操作:
 * 
 * (1) 创建DocumentBuilderFactory、DocumentBuilder读取各种形式的文档对象，生成Document对象
 * (2) 使用传入的EntityResolver的实现类，实现本地加载.dtd或.xsd格式校验文件
 * (3) 根据传入的validation，决定是否开启文档校验
 * (4) 加载Properties文件保存到properties对象中，供解析xml文档中的变量占位
 * (5) 提供根据传入的XPath表达式查询XML文档的功能
 *
 */
public class XPathParser {
	
	private Document document;
	private boolean validation;
	private EntityResolver entityResolver;
	private Properties variables;
	private XPath xpath;
	
	// 1. 构造器
	// (1) 只有一个参数的构造器
	// [start]
	// 默认不校验xml文档，没有属性文件需要加载，也不需要实体解析器
	public XPathParser(String xml) {
		commonConstructor(false, null, null); 
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}
	
	public XPathParser(Reader reader) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(reader));
	}
	
	public XPathParser(InputStream inputStream) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}
	
	public XPathParser(Document document) {
		commonConstructor(false, null, null);
		this.document = document;
	}
	// [end]
	
	// (2) (xx, boolean validation) 形式的构造器
	// [start]
	public XPathParser(String xml, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	} 
	
	public XPathParser(Reader reader, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(reader));
	}
	
	public XPathParser(InputStream inputStream, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}
	
	public XPathParser(Document document, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = document;
	}	
	// [end]

	// (3) (xx, boolean validation, Properties variables) 形式的构造器
	// [start]
	public XPathParser(String xml, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	} 
	
	public XPathParser(Reader reader, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(reader));
	}
	
	public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(inputStream));
	}
	
	public XPathParser(Document document, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = document;
	}		
	// [end]
	
	// (4) (xx, boolean validation, Properties variables, EntityResolver entityResolver) 形式的构造器
	// [start]
	public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	} 
	
	public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(reader));
	}
	
	public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(inputStream));
	}
	
	public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = document;
	}		
	// [end]
	
	// 2. 初始化对象成员
	private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
		this.validation = validation;
		this.variables = variables;
		this.entityResolver = entityResolver;
		XPathFactory factory = XPathFactory.newInstance();
		this.xpath = factory.newXPath();
	}
	
	// 3. 将InputSource对象转化为Document对象
	private Document createDocument(InputSource inputSource) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(validation);   // 是否要校验由外界传入的值确定
			factory.setNamespaceAware(false);    // 如果要使用mybatis的XSD Schema，此处必须设为true，但源码里是false，说明官方默认用dtd来做校验，舍弃了XSD Schema
			factory.setIgnoringComments(true);   // 默认忽略注释
			factory.setIgnoringElementContentWhitespace(false);  // 只有开启校验才能去除xml中的空白节点，但是不知是否开启校验，所以这里设为了false
			factory.setCoalescing(false);
			factory.setExpandEntityReferences(true);    // 默认开启使用扩展实体引用
			
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(entityResolver);  // 使用传入的EntityResolver对象
			builder.setErrorHandler(new ErrorHandler() {  // 定义解析xml文档的错误处理器，如果发生了错误或致命错误则直接抛出异常，如果是警告默认不做处理
				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}
				
				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;					
				}				
				
				@Override
				public void warning(SAXParseException exception) throws SAXException {}
			});
			return builder.parse(inputSource);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}
	
	// 4. eval*() 系列方法
	// 支持String、Boolean、Short、Integer、Long、Float、Double、Node等类型的节点的解析
	// [start]
	public String evalString(String expression) {
		return evalString(document, expression);
	}
	
	// 解析节点占位变量的实际值
	// eg: ${database.password}
	public String evalString(Object root, String expression) {
		String result = (String) evaluate(expression, root, XPathConstants.STRING);
		result = PropertyParser.parse(result, variables);
		return result;
	}
	
	public Boolean evalBoolean(String expression) {
		return evalBoolean(document, expression);
	}
	
	public Boolean evalBoolean(Object root, String expression) {
		return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
	}
	
	public Short evalShort(String expression) {
		return evalShort(document, expression);
	}
	
	public Short evalShort(Object root, String expression) {
		return Short.valueOf(evalString(root, expression));
	}
	
	public Integer evalInteger(String expression) {
		return evalInteger(document, expression);
	}
	
	public Integer evalInteger(Object root, String expression) {
		return Integer.valueOf(evalString(root, expression));
	}
	
	public Long evalLong(String expression) {
		return evalLong(document, expression);
	}
	
	public Long evalLong(Object root, String expression) {
		return Long.valueOf(evalString(root, expression));
	}
	
	public Float evalFloat(String expression) {
		return evalFloat(document, expression);
	}
	
	public Float evalFloat(Object root, String expression) {
		return Float.valueOf(evalString(root, expression));				
	}
	
	public Double evalDouble(String expression) {
		return evalDouble(document, expression);
	}
	
	public Double evalDouble(Object root, String expression) {
		// 为什么不采用上面的写法？
		//return Double.valueOf(evalString(root, expression));
		return (Double) evaluate(expression, root, XPathConstants.NUMBER);
	}
	
	public List<XNode> evalNodes(String expression) {
		return evalNodes(document, expression);
	}
	
	public List<XNode> evalNodes(Object root, String expression) {
		List<XNode> xnodes = new ArrayList<XNode>();
		NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			xnodes.add(new XNode(this, nodes.item(i), variables));
		}
		return xnodes;
	}
	
	public XNode evalNode(String expression) {
		return evalNode(document, expression);
	}
	
	public XNode evalNode(Object root, String expression) {
		Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
		if (node == null) {
			return null;
		} else {
			return new XNode(this, node, variables);
		}
	}
	
	private Object evaluate(String expression, Object root, QName returnType) {
		try {
			return xpath.evaluate(expression, root, returnType);
		} catch (Exception e) {			
			e.printStackTrace();
		}
		return null;
	}
	// [end]
}
