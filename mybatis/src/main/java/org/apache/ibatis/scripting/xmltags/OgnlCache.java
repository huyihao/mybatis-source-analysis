package org.apache.ibatis.scripting.xmltags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.builder.BuilderException;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * 对OGNL进行封装，包装了一层缓存，提高解析表达式的效率
 */
public final class OgnlCache {
	// 缓存
	private static final Map<String, Object> expressionCache = new ConcurrentHashMap<String, Object>();
	
	private OgnlCache() {}
	
	public static Object getValue(String expression, Object root) {
		try {
			@SuppressWarnings("unchecked")
			Map<Object, OgnlClassResolver> context = Ognl.createDefaultContext(root, new OgnlClassResolver());
			return Ognl.getValue(parseExpression(expression), context, root);
		} catch (OgnlException e) {
			throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
		}
	}
	
	private static Object parseExpression(String expression) throws OgnlException {
		Object node = expressionCache.get(expression);
		if (node == null) {
			node = Ognl.parseExpression(expression);
			expressionCache.put(expression, node);
		}
		return node;
	}
}
