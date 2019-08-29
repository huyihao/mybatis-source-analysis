package org.apache.ibatis.mapping;

public interface SqlSource {
	// 通过解析得到BoundSql对象，BoundSql封装了包含"?"占位符的SQL语句，以及绑定的实参
	BoundSql getBoundSql(Object parameterObject);
}
