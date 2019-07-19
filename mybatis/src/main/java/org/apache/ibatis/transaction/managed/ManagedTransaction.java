package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

/**
 * 让容器来管理整个事务的声明周期的 {@link Transaction}
 * 事务的提交和回滚都是依靠容器管理
 *
 */
public class ManagedTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(ManagedTransaction.class);
	
	private Connection connection;            // 事务对应的数据库连接
	private DataSource dataSource;            // 数据库连接所属的DataSource
	private TransactionIsolationLevel level;  // 事务隔离级别	
	private boolean closeConnection;          // 控制数据库连接关闭标识
	
	public ManagedTransaction(Connection connection, boolean closeConnection) {
		this.connection = connection;
		this.closeConnection = closeConnection;
	}
	
	public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
		this.dataSource = ds;
		this.level = level;
		this.closeConnection = closeConnection;
	}	
	
	@Override
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			openConnection();
		}
		return connection;
	}

	@Override
	public void commit() throws SQLException {}

	@Override
	public void rollback() throws SQLException {}

	@Override
	public void close() throws SQLException {
		if (this.closeConnection && this.connection != null) {
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + this.connection + "]");
			}
			this.connection.close();
		}
	}

	@Override
	public Integer getTimeout() throws SQLException {
		return null;
	}

	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		connection = dataSource.getConnection();
		if (level != null) {
			connection.setTransactionIsolation(level.getLevel());
		}
	}	
}
