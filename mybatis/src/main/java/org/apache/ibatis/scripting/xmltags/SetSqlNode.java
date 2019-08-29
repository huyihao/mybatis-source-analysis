package org.apache.ibatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * 处理动态SQL中的 <set> 节点
 */
public class SetSqlNode extends TrimSqlNode {
	
	private static List<String> suffixList = Arrays.asList(",");
	
	public SetSqlNode(Configuration configuration, SqlNode contents) {
		// WhereSqlNode会去除子节点SQL中的 "," 后缀，不处理前缀
		super(configuration, contents, "SET", null, null, suffixList);
	}
}
