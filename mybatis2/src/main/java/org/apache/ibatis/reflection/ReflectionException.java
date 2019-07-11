package org.apache.ibatis.reflection;

import org.apache.ibatis.exceptions.PersistenceException;

// 反射异常
public class ReflectionException extends PersistenceException {

	private static final long serialVersionUID = 6434672459698787198L;

	public ReflectionException() {
		super();
	}
	
	public ReflectionException(String message) {
		super(message);
	}
	
	public ReflectionException(Throwable cause) {
		super(cause);
	}
	
	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
