package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

//数据库事务接口定义
public interface Transaction {
	// 获取数据库连接
	Connection getConnection() throws SQLException;
	
	// 提交内部数据库连接
	void commit() throws SQLException;
	
	// 回滚内部数据库连接
	void rollback() throws SQLException;
	
	// 关闭内部数据库连接
	void close() throws SQLException;
	
	// 获取事务超时时间(如果设置了)
	Integer getTimeout() throws SQLException;
}
