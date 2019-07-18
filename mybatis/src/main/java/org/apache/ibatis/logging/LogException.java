package org.apache.ibatis.logging;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 定制日志异常，用于使用日志适配器出错时抛异常
 */
public class LogException extends PersistenceException {
	
	private static final long serialVersionUID = 3470153563433090316L;

	public LogException() {
		super();
	}
	
	public LogException(String message) {
		super(message);
	}
	
	public LogException(Throwable cause) {
		super(cause);
	}
	
	public LogException(String message, Throwable cause) {
		super(message, cause);
	}	
}
