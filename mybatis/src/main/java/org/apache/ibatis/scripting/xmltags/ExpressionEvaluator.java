package org.apache.ibatis.scripting.xmltags;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BuilderException;

public class ExpressionEvaluator {
	
	// 用于<if test="expression"> 中，根据表达式结果布尔值决定是否要拼接if中的动态sql
	public boolean evaluateBoolean(String expression, Object parameterObject) {
		// 通过OGNL解析表达式的值
		Object value = OgnlCache.getValue(expression, parameterObject);
		if (value instanceof Boolean) {
			return (Boolean) value;
		} 
		if (value instanceof Number) {
			return !new BigDecimal(String.valueOf(value)).equals(BigDecimal.ZERO);
		}
		return value != null;
	}
	
	// 用于<foreach> 节点中，遍历集合，返回集合，表达式的值应该是个集合，或者将其转化为集合
	public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
		Object value = OgnlCache.getValue(expression, parameterObject);
		if (value == null) {
			throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
		}
		if (value instanceof Iterable) {
			return (Iterable<?>) value;
		}
		if (value.getClass().isArray()) {
			int size = Array.getLength(value);
			List<Object> answer = new ArrayList<Object>();
			for (int i = 0; i < size; i++) {
				Object o = Array.get(value, i);
				answer.add(o);
			}
			return answer;
		}
		if (value instanceof Map) {
			return ((Map) value).entrySet();
		}
		throw new BuilderException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
	}
}
