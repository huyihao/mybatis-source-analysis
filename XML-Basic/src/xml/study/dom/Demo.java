package xml.study.dom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * ��ȡXML�ļ���ת��ΪDocument����ļ��ַ�ʽ
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
		
	// ��ʾԪ����Ϣ
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
					System.out.println("��������: " + text);
				} else if(tagName.equals("size")) {
					System.out.println("�����С: " + Integer.valueOf(text));
				}
			} else {
				if (!ignoreTextNode)
					printNodeInfo(child);
			}
		}		
	} 
	
	// ��ӡԪ����Ϣ
	public static void printElementInfo(Element element) {
		try {
			System.out.println("��" + element.getNodeName() + "��");
			System.out.println("Ԫ�ر�ǩ��: " + element.getTagName());
			System.out.println("Ԫ������: '" + element.getTextContent() + "'");
			
			System.out.println("Ԫ����: " + element.getNodeName());
			System.out.println("Ԫ������: " + element.getNodeType()+  " (" + getNodeType(element.getNodeType()) + ")");  // �ڵ����͵Ķ����ڽӿ�Node��
			System.out.println("Ԫ��ֵ: " + element.getNodeValue());		
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ��ӡ�ڵ���Ϣ
	public static void printNodeInfo(Node node) {
		try {
			System.out.println("��" + node.getNodeName() + "��");
			System.out.println("�ڵ���: " + node.getNodeName());
			System.out.println("�ڵ�����: " + node.getNodeType() + " (" + getNodeType(node.getNodeType()) + ")");
			System.out.println("�ڵ�ֵ: '" + node.getNodeValue() + "'");
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ��ʾ���һ���ӽڵ�
	public static void showLastChild(Element element) {
		System.out.println("��" + element.getTagName() + "�� ���һ���ڵ�����:");
		Node node = element.getLastChild();
		printNodeInfo(node);
	}
	
	// ��ӡ�ڵ�������Ϣ
	public static void showAttributes(Element element) {		
		NamedNodeMap attributes = element.getAttributes();
		if (element.getAttribute("unit") != null) {
			System.out.println("Ԫ�ء�" + element.getTagName() + "���������ԡ�unit��");
		}
		try {
			for (int i = 0; i < attributes.getLength(); i++) {
				Node attribute = attributes.item(i);
				System.out.println("���Խڵ�����: " + attribute.getNodeType() + " (" + getNodeType(element.getNodeType()) + ")");
				System.out.println("���Խڵ���: " + attribute.getNodeName());
				System.out.println("���Խڵ�ֵ: " + attribute.getNodeValue());
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
