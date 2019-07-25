package org.apache.ibatis.builder;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 使用构造者模式初始化MyBatis出现的异常
 */
public class BuilderException extends PersistenceException {

	private static final long serialVersionUID = 6139410479211117508L;

	public BuilderException() {
		super();
	}
	
	public BuilderException(String message) {
		super(message);
	}
	
	public BuilderException(Throwable cause) {
		super(cause);
	}
	
	public BuilderException(String message, Throwable cause) {
		super(message, cause);
	}
}
