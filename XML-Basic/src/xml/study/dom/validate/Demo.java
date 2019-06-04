package xml.study.dom.validate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import xml.study.dom.common.DocumentParser;

/**
 * ��ʾ���ʹ��DTD��XML Schema��У��xml�ĵ���ʽ
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
	
	// ��������SYSTEM��ǵ���ҪУ���font.xml
	public static Document parseXMLWithSystemID() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);                        // �Ƿ���ҪУ��XML�ĵ�
		factory.setNamespaceAware(false);
		factory.setIgnoringComments(true);                  // ��Ҫdtd��XML Schema��ϲ�����Ч
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
	
	// ��������PUBLIC��ǵ���ҪУ���font-public.xml
	public static Document parseXMLWithPublicID() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);                        // �Ƿ���ҪУ��XML�ĵ�
		factory.setNamespaceAware(false);
		factory.setIgnoringComments(true);                  // ��Ҫdtd��XML Schema��ϲ�����Ч
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
