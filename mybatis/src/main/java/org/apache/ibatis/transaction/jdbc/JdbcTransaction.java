package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;

/**
 * JDBC事务
 */
public class JdbcTransaction implements Transaction {
	
	private static final Log log = LogFactory.getLog(JdbcTransaction.class);
	
	protected Connection connection;            // 事务对应的数据库连接
	protected DataSource dataSource;            // 数据库连接所属的DataSource
	protected TransactionIsolationLevel level;  // 事务隔离级别
	protected boolean autoCommit;               // 是否自动提交
	
	public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
		dataSource = ds;
		level = desiredLevel;
		autoCommit = desiredAutoCommit;
	}
	
	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			openConnection();
		}
		return connection;
	}

	@Override
	public void commit() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			connection.commit();
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Rolling back JDBC Connection [" + connection + "]");
			}
			connection.rollback();
		}	
	}

	@Override
	public void close() throws SQLException {
		if (connection != null) {
			resetAutoCommit();
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + connection + "]");
			}
			connection.close();
		}	
	}

	@Override
	public Integer getTimeout() throws SQLException {
		return null;
	}

	// 打开数据库连接
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		connection = dataSource.getConnection();
		if (level != null) {
			connection.setTransactionIsolation(level.getLevel());
		}
		setDesiredAutoCommit(autoCommit);
	}
	
	// 设置数据库连接是否自动提交
	protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
		try {
			if (connection.getAutoCommit() != desiredAutoCommit) {
				if (log.isDebugEnabled()) {
					log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(desiredAutoCommit);
			}
		} catch (SQLException e) {
			throw new TransactionException("Error configuring AutoCommit.  " 
					+ "Your driver may not support getAutoCommit() or setAutoCommit(). " 
					+ "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
		}
	}
	
	// 重置连接提交方式为自动提交
	private void resetAutoCommit() {
		try {
			if (!connection.getAutoCommit()) {
				// 如果只执行了选择的方法，MyBatis不会在一个连接里调用commit/rollback方法
				// 有些数据库使用了select语句启动事务，并且要求在关闭连接之前执行commit/rollback
				// 解决方法是在数据库连接关闭之前将自动提交设置为true
				// Sybase会在这里抛出一个异常
				if (log.isDebugEnabled()) {
					log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(true);
			}
		} catch (SQLException e) {
			if (log.isDebugEnabled()) {
				log.debug("Error resetting autocommit to true " 
						+ "before closing the connection.  Cause: " + e);
			}
		}
	}	
}
