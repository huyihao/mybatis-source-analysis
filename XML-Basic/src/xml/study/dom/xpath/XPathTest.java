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
 * 使用XPath解析XML
 * 
 * |-------------------------------------------------------------------|
 * | 表达式                  | 含  义                                                                                                                                                        |
 * |-------------------------------------------------------------------|
 * | nodename  | 选取指定节点的所有子节点                                                                                                                  |
 * |-------------------------------------------------------------------|
 * | /         | 从根节点选取指定节点                                                                                                                          |
 * |-------------------------------------------------------------------|
 * | //        | 根据指定的表达式，在整个文档选取匹配的节点，这里并不会考虑匹配节点在文档中的位置  |
 * |-------------------------------------------------------------------|
 * | .         | 选取当前节点                                                                                                                                          |
 * |-------------------------------------------------------------------|
 * | ..        | 选取当前节点的父节点                                         						   |
 * |-------------------------------------------------------------------|
 * | @         | 选取属性      											   |
 * |-------------------------------------------------------------------|
 * | *         | 匹配任何元素节点      										   |
 * |-------------------------------------------------------------------|
 * | @*        | 匹配任何属性节点											   |
 * |-------------------------------------------------------------------|
 * | node()    | 匹配任何类型的节点										   |
 * |-------------------------------------------------------------------|
 * | text()    | 匹配文本节点											   |
 * |-------------------------------------------------------------------|
 * | |         | 选取若干个路径											   |
 * |-------------------------------------------------------------------|
 * | []        | 指定某个条件，用于查找某个特定节点或包含某个指定值的节点                                                  |
 * |-------------------------------------------------------------------|
 */
public class XPathTest {
	public static void main(String[] args) throws Exception {
		// 1. 创建DocumentBuilder工厂类
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		// 2. 创建DocumentBuilder对象并设置错误处理器
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
		
		// 3. 加载文档生成Document对象
		Document doc = builder.parse("src/xml/study/dom/xpath/inventory.xml");
		
		// 4. 创建XPath工厂类
		XPathFactory xFactory = XPathFactory.newInstance();
		
		// 5. 创建XPath对象
		XPath xpath = xFactory.newXPath();
		
		// 6. 编译XPath表达式
		// 通过XPath表达式得到结果，第一个参数指定了XPath表达式进行查询的上下文节点，也就是在指定节点下查找符合XPath的节点
		// 本例中的上下文节点时整个文档；第二个参数指定了XPath表达式的返回类型		
		XPathExpression expr = xpath.compile("//book[author='Neal Stephenson']/title/text()");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		
		System.out.println("【查询作者为Neal Stephenson的图书的标题】");
		NodeList nodes = (NodeList) result;   // 强制类型转换，至于转换后的类型要看XPathExpression.evaluate方法returnType的设置
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());			
		}		
		System.out.println();
		
		System.out.println("【查询1997年之后的图书的标题】");
		nodes = (NodeList) xpath.evaluate("//book[@year>1997]/title/text()", doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());
		}		
		System.out.println();
		
		System.out.println("【查询1997年之后的图书的年份和标题】");
		nodes = (NodeList) xpath.evaluate("//book[@year>1997]/@*|//book[@year>1997]/title/text()", doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			System.out.println(nodes.item(i).getNodeValue());
		}			
		System.out.println();
		
		System.out.println("【查询2005年之后出版的图书的价格】");
		Double price = (Double) xpath.evaluate("//book[@year=2005]/price/text()", doc, XPathConstants.NUMBER);
		System.out.println(price);	
	}
}
