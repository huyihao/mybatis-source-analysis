package xml.study.dom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * 读取XML文件并转化为Document对象的几种方式
 * eg: 
 * <?xml version="1.0" encoding="UTF-8"?>
 * <!DOCTYPE font>
 * <font>
 *	<name>Helvetica</name>
 *	<size unit="pt">36</size>
 * </font>
 * @author ahao
 *
 */
public class Demo {
	public static void main(String[] args) throws Exception {
		Document doc = XMLParseDomUtils.getXmlDocument("src/xml/study/dom/font.xml");
		Element root = doc.getDocumentElement();
		//showElement(root, true);
		//showLastChild(root);
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				String tagName = ((Element) child).getTagName();
				if ("size".equals(tagName)) {
					showAttributes((Element) child);
					break;
				}				
			}
		}
	}
		
	// 显示元素信息
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
