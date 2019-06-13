package org.apache.ibatis.parsing.test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;

enum Font { Aharoni, Aldhabi, Algerian }

public class XNodeTest {
	public static void main(String[] args) throws Exception {
		// 1. 加载XML文件
		InputStream inputStream = new BufferedInputStream(new FileInputStream("src/main/java/org/apache/ibatis/parsing/test/XNodeTest.xml"));
		
		// 2. 生成XPathParser对象
		Properties variables = new Properties();
		variables.put("evalString2", "evalString2_text");
		XPathParser parser = new XPathParser(inputStream, false, variables, null);
		
		// 3. 生成XNode对象
		XNode configuration = parser.evalNode("/configuration");
		System.out.println("getName(): " + configuration.getName());
		System.out.println("toSting(): " + configuration);
		
		// 4. 测试XNode的eval*()系列方法
		System.out.println("【testEvalType】");
		testEvalType(configuration);
		
		//  5. 节点路径、父节点
		System.out.println("【getPath()、getParent()】");
		XNode evalNode =  configuration.evalNode("evalType/evalNode");
		System.out.println("getPath(): " + evalNode.getPath());
		XNode evalNodeParent = evalNode.getParent();
		System.out.println("getParent(): " + evalNodeParent);
		
		// 6. 节点识别码
		System.out.println("【getValueBasedIdentifier(): id > value > property】");
		XNode evalNode2 =  configuration.evalNode("evalType/evalNode2");
		XNode evalNode3 =  configuration.evalNode("evalType/evalNode3");
		XNode evalNode4 =  configuration.evalNode("evalType/evalNode4");
		System.out.println("getValueBasedIdentifier(): [property] " + evalNode4.getValueBasedIdentifier());
		System.out.println("getValueBasedIdentifier(): [value > property] " + evalNode3.getValueBasedIdentifier());
		System.out.println("getValueBasedIdentifier(): [id > value > property] " + evalNode2.getValueBasedIdentifier());
		System.out.println();
		
		// 7. 测试get*Body()系列方法
		System.out.println("【testEvalBody】");
		testGetTypeBody(configuration.evalNode("evalType"));
		System.out.println();
		
		// 8. 测试get*Attribute()系列方法
		System.out.println("【testGetTypeAttribute】");
		testGetTypeAttribute(configuration.evalNode("getTypeAttribute"));
		System.out.println();
		
		// 9. getChildren()
		System.out.println("【getChildren()】");
		List<XNode> children = configuration.getChildren();
		System.out.println("children num is " + children.size());
		for (XNode node : children) {
			System.out.println(node.getPath());			
		}
		System.out.println();
		
		// 10. getChildrenAsProperties()
		System.out.println("【getChildrenAsProperties】");
		Properties properties = configuration.getChildrenAsProperties();
		System.out.println(properties);
		System.out.println();
		
		// 11. parseAttributes()
		System.out.println("【parseAttributes()】");
		System.out.println(configuration.getStringAttribute("attr1"));
		System.out.println(configuration.getStringAttribute("attr2"));
	}
	
	public static void testEvalType(XNode root) {
		String evalString1 = root.evalString("evalType/evalString1/text()");
		String evalString2 = root.evalString("evalType/evalString2/text()");
		Boolean evalBoolean = root.evalBoolean("evalType/evalBoolean/text()");
		Double evalDouble = root.evalDouble("evalType/evalDouble/text()");
		XNode evalNode =  root.evalNode("evalType/evalNode");
		List<XNode> evalNodes = root.evalNodes("evalType/*");
		
		System.out.println("evalString(expr): " + evalString1);
		System.out.println("evalString(expr) with var: " + evalString2);
		System.out.println("evalBoolean(expr): " + evalBoolean);
		System.out.println("evalDouble(expr): " + evalDouble);
		System.out.println("evalNode(expr): " + evalNode);
		System.out.println("evalNodes(expr): nodeList size is " + evalNodes.size());
		for (int i = 0; i < evalNodes.size(); i++) {
			System.out.println("nodeList[" + i + "] = " + evalNodes.get(i));
		}
	}
	
	public static void testGetTypeBody(XNode root) {		
		XNode node = root.evalNode("evalString1/text()");
		XNode node1 = root.evalNode("evalBoolean/text()");
		XNode node2 = root.evalNode("evalInt/text()");
		XNode node3 = root.evalNode("evalLong/text()");
		XNode node4 = root.evalNode("evalDouble/text()");
		XNode node5 = root.evalNode("evalFloat/text()");
		String getStringBody = node.getStringBody();
		Boolean getBooleanBody = node1.getBooleanBody();
		Integer getIntBody = node2.getIntBody();
		Long getLongBody = node3.getLongBody();
		Double getDoubleBody = node4.getDoubleBody();
		Float getFloatBody = node5.getFloatBody();
		
		System.out.println("getStringBody(): " + getStringBody);
		System.out.println("getBooleanBody(): " + getBooleanBody);
		System.out.println("getIntBody(): " + getIntBody);
		System.out.println("getLongBody(): " + getLongBody);
		System.out.println("getDoubleBody(): " + getDoubleBody);
		System.out.println("getFloatBody(): " + getFloatBody);
	}
	
	public static void testGetTypeAttribute(XNode root) {
		XNode node = root.evalNode("getStringAttribute");
		XNode node1 = root.evalNode("getBooleanAttribute");
		XNode node2 = root.evalNode("getIntAttribute");
		XNode node3 = root.evalNode("getLongAttribute");
		XNode node4 = root.evalNode("getDoubleAttribute");
		XNode node5 = root.evalNode("getFloatAttribute");
		XNode node6 = root.evalNode("style");
		
		System.out.println("getStringAttribute(name): " + node.getStringAttribute("value"));
		System.out.println("getBooleanAttribute(name): " + node1.getBooleanAttribute("value"));
		System.out.println("getIntAttribute(name): " + node2.getIntAttribute("value"));
		System.out.println("getLongAttribute(name): " + node3.getLongAttribute("value"));
		System.out.println("getDoubleAttribute(name): " + node4.getDoubleAttribute("value"));
		System.out.println("getFloatAttribute(name): " + node5.getFloatAttribute("value"));
		System.out.println("getEnumAttribute(Class, name): " + node6.getEnumAttribute(Font.class, "font"));
	}
}
