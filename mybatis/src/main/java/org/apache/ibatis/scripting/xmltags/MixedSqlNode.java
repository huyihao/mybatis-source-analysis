package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * 树枝节点，管理多个叶子节点
 */
public class MixedSqlNode implements SqlNode {

	private List<SqlNode> contents;
	
	public MixedSqlNode(List<SqlNode> contents) {
		this.contents = contents;
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		for (SqlNode sqlNode : contents) {
			sqlNode.apply(context);
		}
		return true;
	}

}
