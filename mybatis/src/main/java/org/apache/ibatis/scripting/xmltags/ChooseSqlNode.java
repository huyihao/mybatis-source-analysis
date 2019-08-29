package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * 处理动态SQL中的 <choose> 节点
 * 
 * [说明] 类似于Java中的switch..case..default
 * [配置示例]
 * <select id="findActiveBlogLike" resultType="Blog">
 * 	SELECT * FROM BLOG WHERE state = ‘ACTIVE’
 * 		<choose>
 *   		<when test="title != null">
 *     			AND title like #{title}
 *   		</when>
 *   		<when test="author != null and author.name != null">
 *     			AND author_name like #{author.name}
 *   		</when>
 *   		<otherwise>
 *     			AND featured = 1
 *   		</otherwise>
 * 		</choose>
 * </select>
 */
public class ChooseSqlNode implements SqlNode {
	private SqlNode defaultSqlNode;   // <otherwise>节点对应的SqlNode
	private List<SqlNode> ifSqlNodes;  // <when>节点对应的IfSqlNode集合
	
	public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
		this.ifSqlNodes = ifSqlNodes;
		this.defaultSqlNode = defaultSqlNode;
	}

	@Override
	public boolean apply(DynamicContext context) {
		// 遍历ifSqlNodes集合并调用其中SqlNode对象的apply() 方法
		for (SqlNode sqlNode : ifSqlNodes) {
			if (sqlNode.apply(context)) {
				return true;
			}
		}
		// 调用defaultSqlNode.apply()方法
		if (defaultSqlNode != null) {
			defaultSqlNode.apply(context);
			return true;
		}
		return false;
	}
	
	
}
