package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 容器管理的事务工厂
 */
public class ManagedTransactionFactory implements TransactionFactory {

	private boolean closeConnection = true;
	
	@Override
	public void setProperties(Properties props) {
		if (props != null) {
			String  closeConnectionProperty = props.getProperty("closeConnection");
			if (closeConnectionProperty != null) {
				closeConnection = Boolean.valueOf(closeConnectionProperty);
			}
		}
	}

	@Override
	public Transaction newTransaction(Connection conn) {
		return new ManagedTransaction(conn, closeConnection);
	}

	@Override
	public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
		// 由容器管理事务，因此不允许自动提交，传入的autoCommit的值不用
		return new ManagedTransaction(dataSource, level, closeConnection);
	}

}
