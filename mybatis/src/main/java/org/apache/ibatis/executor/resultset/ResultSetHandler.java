package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;

/**
 * ResultHandler负责映射select查询得到的结果集，处理存储过程执行后的输出参数
 */
public interface ResultSetHandler {
	// 处理结果集，生成相应的结果对象集合
	<E> List<E> handleResultSets(Statement stmt) throws SQLException;
	
	// 处理结果集，返回相应的游标对象
	<E> Cursor<E> handleCursorResuleSets(Statement stmt) throws SQLException;
	
	// 处理存储过程的输出参数
	void handleOutputParameters(CallableStatement cs) throws SQLException;
}
