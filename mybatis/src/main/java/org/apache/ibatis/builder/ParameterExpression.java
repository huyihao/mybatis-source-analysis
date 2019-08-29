package org.apache.ibatis.builder;

import java.util.HashMap;

/**
 * eg: 
 * 
 *  "__frc_item_0, javaType=int, jdbcType=NUMERIC, typeHandler=MyTypeHandler"
 *  会被解析为Map: {typeHandler=MyTypeHandler, jdbcType=NUMERIC, property=__frc_item_0, javaType=int}
 *
 */
public class ParameterExpression extends HashMap<String, String> {
	
	private static final long serialVersionUID = 5546142594543577854L;

	public ParameterExpression(String expression) {
		parse(expression);
	}
	
	private void parse(String expression) {
		// 找到第一个可见字符的位置
		int p = skipWS(expression, 0);
		if (expression.charAt(p) == '(') {
			expression(expression, p + 1);
		} else {
			property(expression, p);
		}
	}
	
	// eg: "  prop : value, ..."
	private void property(String expression, int left) {
		if (left < expression.length()) {
			// 找到第一个可见字符之后，第一个','或':'字符的位置
			// 如上面的例子':'的位置是7
			int right = skipUntil(expression, left, ",:");
			// 按照上面的例子: trimmedStr("  prop : value, ...", 2, 7)返回的是"prop"
			put("property", trimmedStr(expression, left, right));
			jdbcTypeOpt(expression, right);
		}
	}
	
	private void expression(String expression, int left) {
		int match = 1;
		int right = left + 1;
		while (match > 0) {
			if (expression.charAt(right) == ')') {
				match--;
			} else if (expression.charAt(right) == '(') {
				match++;
			}
			right++;
		}
		put("expression", expression.substring(left, right - 1));
		jdbcTypeOpt(expression, right);
	}
	
	// 找到自位置p之后第一个可见字符的位置并返回
	// eg: skipWS("  aaa", 0) 返回2，第一个可见字符为'a'
	private int skipWS(String expression, int p) {
		for (int i = p; i < expression.length(); i++) {
			/**
			 * ASCII常识:
			 * 小于0x20编码的字符都是不可见字符，eg: 换行、退出
			 * 0x20 是空格的十六进制表示
			 * 大于0x20编码的字符都是可见字符
			 */
			if (expression.charAt(i) > 0x20) {
				return i;
			}
		}
		return expression.length();
	}
	
	private int skipUntil(String expression, int p, final String endChars) {
		for (int i = p; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (endChars.indexOf(c) > -1) {
				return i;
			}
		}
		return expression.length();
	}
	
	private void jdbcTypeOpt(String expression, int p) {
		p = skipWS(expression, p);
		if (p < expression.length()) {
			if (expression.charAt(p) == ':') {
				jdbcType(expression, p + 1);
			} else if (expression.charAt(p) == ',') {
				option(expression, p + 1);
			} else {
				throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
			}
		}
	}
	
	private void jdbcType(String expression, int p) {
		int left = skipWS(expression, p);
		int right = skipUntil(expression, left, ",");
		if (right > left) {
			put("jdbcType", trimmedStr(expression, left, right));
		} else {
			throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
		}
		option(expression, right + 1);
	}

	private void option(String expression, int p) {
		int left = skipWS(expression, p);
		if (left < expression.length()) {
			int right = skipUntil(expression, left, "=");
			String name = trimmedStr(expression, left, right);
			left = right + 1;
			right = skipUntil(expression, left, ",");
			String value = trimmedStr(expression, left, right);
			put(name, value);
			option(expression, right + 1);
		}
	}	
	
	// 去掉str位置start~end (不包含end)之间的空格
	private String trimmedStr(String str, int start, int end) {
		while (str.charAt(start) <= 0x20) {
			start++;
		}
		while (str.charAt(end - 1) <= 0x20) {
			end--;
		}
		return start >= end ? "" : str.substring(start, end);
	}

	public static void main(String[] args) {
		HashMap<String, String> map = new ParameterExpression("__frc_item_0, javaType=int, jdbcType=NUMERIC, typeHandler=MyTypeHandler");
		System.out.println(map);
	}
}