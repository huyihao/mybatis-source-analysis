package org.apache.ibatis.executor.result;

import org.apache.ibatis.exceptions.PersistenceException;

public class ResultMapException extends PersistenceException {

	private static final long serialVersionUID = -3566897237818256967L;

	public ResultMapException() {
		super();
	}
	
	public ResultMapException(String message) {
		super(message);
	}
	
	public ResultMapException(Throwable cause) {
		super(cause);
	}
	
	public ResultMapException(String message, Throwable cause) {
		super(message, cause);
	}
}
