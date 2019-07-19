package org.apache.ibatis.transaction;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 事务异常
 */
public class TransactionException extends PersistenceException {

	private static final long serialVersionUID = 4929214946319469375L;

	public TransactionException() {
		super();
	}
	
	public TransactionException(String message) {
		super(message);
	}
	
	public TransactionException(Throwable cause) {
		super(cause);
	}
	
	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}	
}
