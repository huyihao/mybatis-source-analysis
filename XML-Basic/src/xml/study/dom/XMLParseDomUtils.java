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
		
		documentBuilderFactory.setValidating(true);                         // 是否指定此代码生成的解析器将在文档解析时验证文档
		documentBuilderFactory.setNamespaceAware(false);                    // 是否指定此代码生成的解析器将为XML命名空间提供支持
		documentBuilderFactory.setIgnoringComments(true);                   // 是否指定此代码生成的解析器将忽略注释
		documentBuilderFactory.setIgnoringElementContentWhitespace(false);  // 是否指定此工厂创建的解析器必须在解析XML文档时消除元素内容中的空格（有时称为“可忽略的空白”）
		documentBuilderFactory.setCoalescing(false);                        // 是否指定此代码生成的解析器将CDATA节点转换为文本节点并将其附加到相邻（如果有的话）文本节点
		documentBuilderFactory.setExpandEntityReferences(true);		        // 是否指定此代码生成的解析器将扩展实体引用节点
		
		// 创建DocumentBuilder
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();		
		// 设置异常处理对象
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
	
	// 1. 直接通过文件路径读取文件
	public static Document getDocumentByPath(DocumentBuilder builder) throws SAXException, IOException {
		Document document =  builder.parse("src/xml/study/dom/inventory.xml");
		return document;
	}
	
	// 2. 通过File读取文件
	public static Document getDocumentByFile(DocumentBuilder builder) throws SAXException, IOException {
		File file = new File("src/xml/study/dom/inventory.xml");
		Document document = builder.parse(file);
		return document;
	}
	
	// 3. 通过InputStream读取文件
	public static Document getDocumentByFileInputStream(DocumentBuilder builder) throws SAXException, IOException {
		File file = new File("src/xml/study/dom/inventory.xml");
		InputStream inputStream = new FileInputStream(file);
		
		Document document = builder.parse(inputStream);
		return document;
	}
	
	// 4. 通过ClassLoader读取文件(指明source folder下的相对目录即可)
	public static Document getDocumentByClassLoader(DocumentBuilder builder) throws SAXException, IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("xml/study/dom/inventory.xml");
		
		Document document = builder.parse(inputStream);
		return document;
	}
}
