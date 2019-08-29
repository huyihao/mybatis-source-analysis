package org.apache.ibatis.scripting.defaults;

import java.util.HashMap;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

public class RawSqlSource implements SqlSource {

	private final SqlSource sqlSource;	    // StaticSqlSource对象
	
	public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
		// 调用getSql()方法，完成SQL语句的拼装和初步解析
		this(configuration, getSql(configuration, rootSqlNode), parameterType);
	}
	
	public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
		// 通过SqlSourceBuilder完成占位符的解析和替换操作
		SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
		Class<?> clazz = parameterType == null ? Object.class : parameterType;
		sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<String, Object>());
	}
	
	@Override
	public BoundSql getBoundSql(Object parameterObject) {
		return sqlSource.getBoundSql(parameterObject);
	}
	
	private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
		DynamicContext context = new DynamicContext(configuration, null);
		rootSqlNode.apply(context);
		return context.getSql();
	}

}
