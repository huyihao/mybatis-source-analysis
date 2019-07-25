package org.apache.ibatis.parsing.test;

import java.io.InputStream;

import org.apache.ibatis.parsing.XPathParser;

public class XPathParserTest {
	public static void main(String[] args) {
		// 如果XML文件直接放在源目录(Source Directory)下，则可以直接解析
		// 如果放在源目录下的子目录中，则必须加上相对工程路径
		// InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/apache/ibatis/parsing/test/mybatis-config.xml");
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("mybatis-config.xml");
		XPathParser xPathParser = new XPathParser(inputStream, false, null, null);
		System.out.println(xPathParser);
	}
}
