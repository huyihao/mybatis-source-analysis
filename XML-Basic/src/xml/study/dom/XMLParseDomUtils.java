package xml.study.dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLParseDomUtils {
	public static Document getXmlDocument(String xmlPath) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		
		documentBuilderFactory.setValidating(true);                         // �Ƿ�ָ���˴������ɵĽ����������ĵ�����ʱ��֤�ĵ�
		documentBuilderFactory.setNamespaceAware(false);                    // �Ƿ�ָ���˴������ɵĽ�������ΪXML�����ռ��ṩ֧��
		documentBuilderFactory.setIgnoringComments(true);                   // �Ƿ�ָ���˴������ɵĽ�����������ע��
		documentBuilderFactory.setIgnoringElementContentWhitespace(false);  // �Ƿ�ָ���˹��������Ľ����������ڽ���XML�ĵ�ʱ����Ԫ�������еĿո���ʱ��Ϊ���ɺ��ԵĿհס���
		documentBuilderFactory.setCoalescing(false);                        // �Ƿ�ָ���˴������ɵĽ�������CDATA�ڵ�ת��Ϊ�ı��ڵ㲢���丽�ӵ����ڣ�����еĻ����ı��ڵ�
		documentBuilderFactory.setExpandEntityReferences(true);		        // �Ƿ�ָ���˴������ɵĽ���������չʵ�����ýڵ�
		
		// ����DocumentBuilder
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();		
		// �����쳣�������
		builder.setErrorHandler(new ErrorHandler() {
			
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void fatalError(SAXParseException exception) throws SAXException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void error(SAXParseException exception) throws SAXException {
				// TODO Auto-generated method stub
				
			}
		});		
		
		//Document doc = getDocumentByClassLoader(builder);
		//System.out.println(doc);
		return builder.parse(xmlPath);
	} 
	
	// 1. ֱ��ͨ���ļ�·����ȡ�ļ�
	public static Document getDocumentByPath(DocumentBuilder builder) throws SAXException, IOException {
		Document document =  builder.parse("src/xml/study/dom/inventory.xml");
		return document;
	}
	
	// 2. ͨ��File��ȡ�ļ�
	public static Document getDocumentByFile(DocumentBuilder builder) throws SAXException, IOException {
		File file = new File("src/xml/study/dom/inventory.xml");
		Document document = builder.parse(file);
		return document;
	}
	
	// 3. ͨ��InputStream��ȡ�ļ�
	public static Document getDocumentByFileInputStream(DocumentBuilder builder) throws SAXException, IOException {
		File file = new File("src/xml/study/dom/inventory.xml");
		InputStream inputStream = new FileInputStream(file);
		
		Document document = builder.parse(inputStream);
		return document;
	}
	
	// 4. ͨ��ClassLoader��ȡ�ļ�(ָ��source folder�µ����Ŀ¼����)
	public static Document getDocumentByClassLoader(DocumentBuilder builder) throws SAXException, IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("xml/study/dom/inventory.xml");
		
		Document document = builder.parse(inputStream);
		return document;
	}
}
