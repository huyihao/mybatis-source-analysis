package xml.study.dom.xpath;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * ʹ��XPath����XML
 * 
 * |-------------------------------------------------------------------|
 * | ���ʽ                  | ��  ��                                                                                                                                                        |
 * |-------------------------------------------------------------------|
 * | nodename  | ѡȡָ���ڵ�������ӽڵ�                                                                                                                  |
 * |-------------------------------------------------------------------|
 * | /         | �Ӹ��ڵ�ѡȡָ���ڵ�                                                                                                                          |
 * |-------------------------------------------------------------------|
 * | //        | ����ָ���ı��ʽ���������ĵ�ѡȡƥ��Ľڵ㣬���ﲢ���ῼ��ƥ��ڵ����ĵ��е�λ��  |
 * |-------------------------------------------------------------------|
 * | .         | ѡȡ��ǰ�ڵ�                                                                                                                                          |
 * |-------------------------------------------------------------------|
 * | ..        | ѡȡ��ǰ�ڵ�ĸ��ڵ�                                         						   |
 * |-------------------------------------------------------------------|
 * | @         | ѡȡ����      											   |
 * |-------------------------------------------------------------------|
 * | *         | ƥ���κ�Ԫ�ؽڵ�      										   |
 * |-------------------------------------------------------------------|
 * | @*        | ƥ���κ����Խڵ�											   |
 * |-------------------------------------------------------------------|
 * | node()    | ƥ���κ����͵Ľڵ�										   |
 * |-------------------------------------------------------------------|
 * | text()    | ƥ���ı��ڵ�											   |
 * |-------------------------------------------------------------------|
 * | |         | ѡȡ���ɸ�·��											   |
 * |-------------------------------------------------------------------|
 * | []        | ָ��ĳ�����������ڲ���ĳ���ض��ڵ�����ĳ��ָ��ֵ�Ľڵ�                                                  |
 * |-------------------------------------------------------------------|
 */
public class XPathTest {
	public static void main(String[] args) throws Exception {
		// 1. ����DocumentBuilder������
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		// 2. ����DocumentBuilder�������ô�������
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new ErrorHandler() {			
			@Override
			public void warning(SAXParseException exception) throws SAXException {}			
			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				throw exception;
			}
			@Override
			public void error(SAXParseException exception) throws SAXException {
				throw exception;				
			}
		});
		
		// 3. �����ĵ�����Document����
		Document doc = builder.parse("src/xml/study/dom/xpath/inventory.xml");
		
		// 4. ����XPath������
		XPathFactory xFactory = XPathFactory.newInstance();
		
		// 5. ����XPath����
		XPath xpath = xFactory.newXPath();
		
		// 6. ����XPath���ʽ
		// ͨ��XPath���ʽ�õ��������һ������ָ����XPath���ʽ���в�ѯ�������Ľڵ㣬Ҳ������ָ���ڵ��²��ҷ���XPath�Ľڵ�
		// �����е������Ľڵ�ʱ�����ĵ����ڶ�������ָ����XPath���ʽ�ķ�������		
		XPathExpression expr = xpath.compile("//book[author='Neal Stephenson']/title/text()");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		
		System.out.println("����ѯ����ΪNeal Stephenson��ͼ��ı��⡿");
		NodeList nodes = (NodeList) result;   // ǿ������ת��������ת���������Ҫ��XPathExpression.evaluate����returnType������
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());			
		}		
		System.out.println();
		
		System.out.println("����ѯ1997��֮���ͼ��ı��⡿");
		nodes = (NodeList) xpath.evaluate("//book[@year>1997]/title/text()", doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());
		}		
		System.out.println();
		
		System.out.println("����ѯ1997��֮���ͼ�����ݺͱ��⡿");
		nodes = (NodeList) xpath.evaluate("//book[@year>1997]/@*|//book[@year>1997]/title/text()", doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());
		}			
		System.out.println();
		
		System.out.println("����ѯ2005��֮������ͼ��ļ۸�");
		Double price = (Double) xpath.evaluate("//book[@year=2005]/price/text()", doc, XPathConstants.NUMBER);
		System.out.println(price);	
	}
}
