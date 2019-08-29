package org.apache.ibatis.scripting.xmltags;

import java.util.regex.Pattern;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;

/**
 * 解析动态SQL节点中的 "${}"，将其从变量占位解析成对应的变量值
 */
public class TextSqlNode implements SqlNode {

	private String text;
	private Pattern injectionFilter;
	
	public TextSqlNode(String text) {
		this(text, null);
	}
	
	public TextSqlNode(String text, Pattern injectionFilter) {
		this.text = text;
		this.injectionFilter = injectionFilter;
	}
	
	public boolean isDynamic() {
		DynamicCheckTokenParser checker = new DynamicCheckTokenParser();
		GenericTokenParser parser = createParser(checker);
		parser.parse(text);
		return checker.isDynamic();
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
		// 将解析后的SQL片段添加到DynamicContext中
		context.appendSql(parser.parse(text));
		return true;
	}
	
	private GenericTokenParser createParser(TokenHandler handler) {		
		return new GenericTokenParser("${", "}", handler);    // 解析的是 "${}" 占位符
	}
	
	// 根据DynamicContext集合中的信息解析SQL语句节点中的 "${}" 占位符
	private static class BindingTokenParser implements TokenHandler {

		private DynamicContext context;
		private Pattern injectionFilter;
		
		public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
			this.context = context;
			this.injectionFilter = injectionFilter;
		}
		
		@Override
		public String handleToken(String content) {
			// 获取用户提供的实参
			Object parameter = context.getBindings().get(DynamicContext.PARAMETER_OBJECT_KEY);
			if (parameter == null) {
				context.getBindings().put("value", null);
			}
			// 通过OGNL解析content的值
			Object value = OgnlCache.getValue(content, context.getBindings());
			String srtValue = (value == null ? "" : String.valueOf(value));
			checkInjection(srtValue);  // 检测合法性
			return srtValue;
		}
		
		// 检查字符串是否符合设置的正则表达式
		private void checkInjection(String value) {
			if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
				throw new ScriptingException("Invalid input. Please confirm to regex" + injectionFilter.pattern());				
			}
		}
	}

	private static class DynamicCheckTokenParser implements TokenHandler {

		private boolean isDynamic;
		
		public DynamicCheckTokenParser() {}
		
		public boolean isDynamic() {
			return isDynamic;
		}
		
		// 这里没有实际去处理 "${}" 占位符，只是当GenericTokenParser
		// 解析到占位符时会调用本方法，当handleToken()方法被调用到的，证明本TextSqlNode实际上是个动态SQL
		@Override
		public String handleToken(String context) {
			this.isDynamic = true;
			return null;
		}
		
	}
}
