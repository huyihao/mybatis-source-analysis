package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * JDBC 事务工厂
 */
public class JdbcTransactionFactory implements TransactionFactory {

	@Override
	public void setProperties(Properties props) {
	}

	@Override
	public Transaction newTransaction(Connection conn) {
		return new JdbcTransaction(conn);
	}

	@Override
	public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
		return new JdbcTransaction(dataSource, level, autoCommit);
	}

}
