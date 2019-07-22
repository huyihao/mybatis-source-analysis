package org.apache.ibatis.mapping;

/**
 * JDBC Statement的类型
 * 分别表示Statement、PreparedStatement、CallableStatement
 */
public enum StatementType {
	STATEMENT, PREPARED, CALLABLE
}
