package org.apache.ibatis.scripting.xmltags;

/**
 * 记录了非动态SQL语句节点
 */
public class StaticTextSqlNode implements SqlNode {

	private String text;
	
	public StaticTextSqlNode(String text) {
		this.text = text;
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		context.appendSql(text);
		return true;
	}

}
