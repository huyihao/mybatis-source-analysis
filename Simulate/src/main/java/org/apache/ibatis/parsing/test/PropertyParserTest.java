package org.apache.ibatis.parsing.test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.upd.PropertyParserUpd;

/**
 * 属性解析器测试案例
 */
public class PropertyParserTest {
	public static void main(String[] args) throws Exception {
		// 1. 加载properties文件到Properties对象中
		InputStream inputStream = new BufferedInputStream(new FileInputStream("src/main/java/org/apache/ibatis/parsing/test/jdbc.properties"));
		Properties variables = new Properties();
		variables.load(inputStream);
		System.out.println(variables.getProperty("database.username"));
		System.out.println(variables.getProperty("database.password"));
		System.out.println(variables.getProperty("database.username}"));
		System.out.println();
		
		System.out.println("【testPropertyParser】");
		testPropertyParser(variables);
		System.out.println();
		System.out.println("【testPropertyParserUpd】");
		testPropertyParserUpd(variables);
	}
	
	public static void testPropertyParser(Properties variables) {
		System.out.println(PropertyParser.parse("${database.username}", variables));
		System.out.println(PropertyParser.parse("${database.password}", variables));
		System.out.println(PropertyParser.parse("aaa${database.username}bbb", variables));
		System.out.println(PropertyParser.parse("aaa\\${database.username}bbb", variables));
		// aaa${database.username\\}bbb || aaa\\${database.username\\}bbb
		// 解析结果: aaa${database.username\}bbb, 个人觉得应该是aaa${database.username}bbb
		System.out.println(PropertyParser.parse("aaa${database.username\\}bbb", variables));    
		System.out.println(PropertyParser.parse("aaa\\${database.username\\}bbb", variables));
		System.out.println(PropertyParser.parse("${database.username}/${database.password}", variables));		
	}
	
	public static void testPropertyParserUpd(Properties variables) {
		System.out.println(PropertyParserUpd.parse("${database.username}", variables));
		System.out.println(PropertyParserUpd.parse("${database.password}", variables));
		System.out.println(PropertyParserUpd.parse("aaa${database.username}bbb", variables));
		System.out.println(PropertyParserUpd.parse("aaa\\${database.username}bbb", variables));
		System.out.println(PropertyParserUpd.parse("aaa${database.username\\}}bbb", variables));
		// aaa${database.username\\}bbb
		// 解析结果: aaa${database.username\}bbb, 个人觉得应该是aaa${database.username}bbb
		System.out.println(PropertyParserUpd.parse("aaa${database.username\\}bbb", variables));    
		System.out.println(PropertyParserUpd.parse("aaa\\${database.username\\}bbb", variables));
		System.out.println(PropertyParserUpd.parse("${database.username}/${database.password}", variables));		
	}
}
