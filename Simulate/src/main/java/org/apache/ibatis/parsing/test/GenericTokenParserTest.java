package org.apache.ibatis.parsing.test;

import java.io.InputStream;
import java.util.Properties;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.parsing.upd.GenericTokenParserUpd;

public class GenericTokenParserTest {
	public static void main(String[] args) throws Exception {
		// 1. 加载Properties文件生成Properties对象
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jdbc.properties");
		Properties variables = new Properties();
		variables.load(inputStream);
		
		// 2. 解析占位符
		VariableTokenHandler handler = new VariableTokenHandler(variables);
		GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
		// 常规的情况，通常我们也都是这么用的
		System.out.println(parser.parse("${database.username}"));
		// 起始占位符前有常规字符串，结束占位符后又常规字符串
		System.out.println(parser.parse("haha${database.username}hehe"));
		// 演示对转义起始占位符的处理
		System.out.println(parser.parse("haha\\${database.username}hehe"));
		// 演示对转义结束占位符的处理
		System.out.println(parser.parse("haha${database.username\\}hehe"));
		// 演示对起始结束占位符都转义的处理
		System.out.println(parser.parse("haha\\${database.username\\}hehe"));
		// 演示有多对匹配的占位符的处理
		System.out.println(parser.parse("username:${database.username},password:${database.password}"));
		System.out.println();
		
		// 3. 测试修改后的代码
		GenericTokenParserUpd parser2 = new GenericTokenParserUpd("${", "}", handler);
		// 常规的情况，通常我们也都是这么用的
		System.out.println(parser2.parse("${database.username}"));
		// 起始占位符前有常规字符串，结束占位符后又常规字符串
		System.out.println(parser2.parse("haha${database.username}hehe"));
		// 演示对转义起始占位符的处理
		System.out.println(parser2.parse("haha\\${database.username}hehe"));
		// 演示对转义结束占位符的处理
		System.out.println(parser2.parse("haha${database.username\\}hehe"));
		// 演示对起始结束占位符都转义的处理
		System.out.println(parser2.parse("haha\\${database.username\\}hehe"));
		// 演示有多对匹配的占位符的处理
		System.out.println(parser2.parse("username:${database.username},password:${database.password}"));
	}
	
	// 为了方便测试，将私有静态内部类定义在测试案例中
	private static class VariableTokenHandler implements TokenHandler {
		private Properties variables;
		
		public VariableTokenHandler(Properties variables) {
			this.variables = variables;
		}
		
		@Override
		public String handleToken(String context) {
			if (variables != null && variables.containsKey(context)) {
				return variables.getProperty(context);
			}
			
			return "${" + context + "}";
		}
		
	}
}
