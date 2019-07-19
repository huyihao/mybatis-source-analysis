package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * 事务隔离级别
 */
public enum TransactionIsolationLevel {
	NONE(Connection.TRANSACTION_NONE),
	READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),  // 脏读
	READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),      // 幻读
	REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),    // 可重复读
	SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);          // 序列化
	
	private final int level;
	
	private TransactionIsolationLevel(int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return level;
	}
}
