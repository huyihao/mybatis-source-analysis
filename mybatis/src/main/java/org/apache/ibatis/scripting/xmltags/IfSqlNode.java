package org.apache.ibatis.scripting.xmltags;

/**
 * 对应的动态SQL节点的 <if> 节点
 * [示例]
 *  <if test="username != null">
 *  	username = #{username}
 *  </if>
 */
public class IfSqlNode implements SqlNode {
	private ExpressionEvaluator evaluator;  // ExpressionEvaluator对象用于解析<if>节点的test表达式的值
	private String test;                    // 记录了<if>节点中的test表达式
	private SqlNode contents;               // 记录了<if>节点的子表达式
	
	public IfSqlNode(SqlNode contents, String test) {
		this.test = test;
		this.contents = contents;
		this.evaluator = new ExpressionEvaluator();
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		// 检测test属性中记录的表达式
		if (evaluator.evaluateBoolean(test, context.getBindings())) {
			contents.apply(context);   // test表达式为true，则执行子节点的apply()方法
			return true;
		}
		return false;  // 返回值表示test表达式的结果是否为true
	}

}
