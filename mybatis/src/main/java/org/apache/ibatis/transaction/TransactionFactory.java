package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * 事务工厂
 */
public interface TransactionFactory {
	void setProperties(Properties props);
	
	Transaction newTransaction(Connection conn);
	
	Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);
}
