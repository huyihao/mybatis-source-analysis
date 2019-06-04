package xml.study.dom.validate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import xml.study.dom.common.DocumentParser;

/**
 * 演示如何使用DTD或XML Schema来校验xml文档格式
 * @author ahao
 *
 */
public class Demo {
	public static void main(String[] args) throws Exception {
		//Document doc1 = parseXMLWithSystemID();
		//System.out.println(doc1);
		
		Document doc2 = parseXMLWithPublicID();
		//System.out.println(doc2);
		DocumentParser.showValidateElement(doc2.getDocumentElement());
	}
	
	// 解析带有SYSTEM标记的需要校验的font.xml
	public static Document parseXMLWithSystemID() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);                        // 是否需要校验XML文档
		factory.setNamespaceAware(false);
		factory.setIgnoringComments(true);                  // 需要dtd或XML Schema配合才能生效
		factory.setIgnoringElementContentWhitespace(false);
		factory.setCoalescing(false);
		factory.setExpandEntityReferences(true);
		
		DocumentBuilder builder = factory.newDocumentBuilder();	
		builder.setErrorHandler(new ErrorHandler() {
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
		
		Document doc = builder.parse("src/xml/study/dom/validate/font.xml");
		return doc;
	}
	
	// 解析带有PUBLIC标记的需要校验的font-public.xml
	public static Document parseXMLWithPublicID() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);                        // 是否需要校验XML文档
		factory.setNamespaceAware(false);
		factory.setIgnoringComments(true);                  // 需要dtd或XML Schema配合才能生效
		factory.setIgnoringElementContentWhitespace(true);
		factory.setCoalescing(false);
		factory.setExpandEntityReferences(true);
		
		DocumentBuilder builder = factory.newDocumentBuilder();	
		builder.setEntityResolver(new MyEntityResolver());
		builder.setErrorHandler(new ErrorHandler() {
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
		
		Document doc = builder.parse("src/xml/study/dom/validate/font-public.xml");	
		return doc;
	}
}
