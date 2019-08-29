package org.apache.ibatis.scripting.xmltags;

public interface SqlNode {
	/**
	 * apply() 是 SqlNode 接口中定义的唯一方法，该方法会根据用户传入的实参，参数解析该 SqlNode 
	 * 所记录的动态 SQL 节点，并调用 DynamicContext.appendSql() 方法将解析后的 SQL 片段追加到
	 * DynamicContext.sqlBuilder 中保存
	 * 当 SQL 节点下的所有SqlNode 完成解析后，就可以从DynamicContext 中获取一条动态生成的、完整的SQL语句
	 */
	boolean apply(DynamicContext context);
}
