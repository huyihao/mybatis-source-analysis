package xml.study.dom.common;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DocumentParser {
	/**
	 * 显示已经被dtd校验过的元素信息
	 * @param element
	 */
	public static void showValidateElement(Element element) {
		printElementInfo(element);
		NodeList children = element.getChildNodes();
		if (children.getLength() != 0) {
			System.out.println("【" + element.getTagName() + "子节点信息如下】");
		}
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (!(node instanceof CharacterData)) {
				Element child = (Element) node;
				if (child.getChildNodes().getLength() == 1 && (child.getChildNodes().item(0) instanceof CharacterData)) {
					printElementInfo(child);
				} else {
					showValidateElement(child);
				}
			}
		}
	}
	
	/**
	 * 显示元素信息，给没使用dtd验证会产生文本节点的元素使用
	 * @param element          要显示信息的元素
	 * @param ignoreTextNode   是否忽略文本节点
	 */
	public static void showElement(Element element, boolean ignoreTextNode) {
		printElementInfo(element);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElement = (Element) child;
				printElementInfo(childElement);			
				Text textNode = (Text) childElement.getFirstChild();
				String text = textNode.getData().trim();
				String tagName = childElement.getTagName();
				if (tagName.equals("name")) {
					System.out.println("字体类型: " + text);
				} else if(tagName.equals("size")) {
					System.out.println("字体大小: " + Integer.valueOf(text));
				}
			} else {
				if (!ignoreTextNode)
					printNodeInfo(child);
			}
		}		
	} 
	
	// 打印元素信息
	public static void printElementInfo(Element element) {
		try {
			System.out.println("【" + element.getNodeName() + "】");
			System.out.println("元素标签名: " + element.getTagName());
			System.out.println("元素内容: '" + element.getTextContent() + "'");
			
			System.out.println("元素名: " + element.getNodeName());
			System.out.println("元素类型: " + element.getNodeType()+  " (" + getNodeType(element.getNodeType()) + ")");  // 节点类型的定义在接口Node中
			System.out.println("元素值: " + element.getNodeValue());		
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 打印节点信息
	public static void printNodeInfo(Node node) {
		try {
			System.out.println("【" + node.getNodeName() + "】");
			System.out.println("节点名: " + node.getNodeName());
			System.out.println("节点类型: " + node.getNodeType() + " (" + getNodeType(node.getNodeType()) + ")");
			System.out.println("节点值: '" + node.getNodeValue() + "'");
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 显示最后一个子节点
	public static void showLastChild(Element element) {
		System.out.println("【" + element.getTagName() + "】 最后一个节点详情:");
		Node node = element.getLastChild();
		printNodeInfo(node);
	}
	
	// 打印节点属性信息
	public static void showAttributes(Element element) {		
		NamedNodeMap attributes = element.getAttributes();
		if (element.getAttribute("unit") != null) {
			System.out.println("元素【" + element.getTagName() + "】存在属性【unit】");
		}
		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				Node attribute = attributes.item(i);
				System.out.println("属性节点类型: " + attribute.getNodeType() + " (" + getNodeType(element.getNodeType()) + ")");
				System.out.println("属性节点名: " + attribute.getNodeName());
				System.out.println("属性节点值: " + attribute.getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getNodeType(short type) throws Exception {
		if (type < 1 || type > 12) {
			throw new Exception("Node Type must between 1 ~ 12!");			
		}
		String typeStr = null;
		switch(type) {
			case 1:
				typeStr = "Node.ELEMENT_NODE";
				break;
			case 2:
				typeStr = "Node.ATTRIBUTE_NODE";
				break;
			case 3:
				typeStr = "Node.TEXT_NODE";
				break;
			case 4:
				typeStr = "Node.CDATA_SECTION_NODE";
				break;
			case 5:
				typeStr = "Node.ENTITY_REFERENCE_NODE";
				break;
			case 6:
				typeStr = "Node.ENTITY_NODE";
				break;
			case 7:
				typeStr = "Node.PROCESSING_INSTRUCTION_NODE";
				break;
			case 8:
				typeStr = "Node.COMMENT_NODE";
				break;
			case 9:
				typeStr = "Node.DOCUMENT_NODE";
				break;
			case 10:
				typeStr = "Node.DOCUMENT_TYPE_NODE";
				break;
			case 11:
				typeStr = "Node.DOCUMENT_FRAGMENT_NODE";
				break;
			default:
				typeStr = "Node.NOTATION_NODE";
				break;			
		}
		return typeStr;
	}
}
