package org.apache.ibatis.scripting.xmltags;

/**
 * 处理动态SQL中的 <bind> 节点
 * 
 * [用法] 用于模糊匹配，兼容不同数据库模糊查询的语法
 * [配置示例]
 * <select id="selectBlogsLike" resultType="Blog">
 *     <bind name="pattern" value="'%' + _parameter.getTitle() + '%'" />
 * 	   SELECT * FROM BLOG WHERE title LIKE #{pattern}
 * </select>
 */
public class VarDeclSqlNode implements SqlNode {
	private final String name;          // 对应 <bind> 节点的name属性
	private final String expression;    // 对应 <bind> 节点的value属性
	
	public VarDeclSqlNode(String var, String exp) {
		name = var;
		expression = exp;
	}

	@Override
	public boolean apply(DynamicContext context) {
		final Object value = OgnlCache.getValue(expression, context.getBindings());
		context.bind(name, value);
		return true;
	}
	
}
