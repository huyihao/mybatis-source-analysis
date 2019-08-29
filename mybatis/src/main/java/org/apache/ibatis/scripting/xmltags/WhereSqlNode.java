package org.apache.ibatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * 处理动态SQL中的 <where> 节点
 */
public class WhereSqlNode extends TrimSqlNode {

	private static List<String> prefixList = Arrays.asList("AND", "OR", "AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");
	
	public WhereSqlNode(Configuration configuration, SqlNode contents) {
		// WhereSqlNode会去除子节点SQL中的AND、OR前缀，不处理后缀
		super(configuration, contents, "WHERE", prefixList, null, null);
	}

}
