package org.apache.ibatis.session;

import org.apache.ibatis.exceptions.PersistenceException;

public class SqlSessionException extends PersistenceException {

	private static final long serialVersionUID = -2368123464606620536L;

	public SqlSessionException() {
		super();
	}
	
	public SqlSessionException(String message) {
		super(message);
	}	
	
	public SqlSessionException(Throwable cause) {
		super(cause);
	}
	
	public SqlSessionException(String message, Throwable cause) {
		super(message, cause);
	}
}
