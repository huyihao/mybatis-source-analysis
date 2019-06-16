package org.apache.ibatis.exceptions;

// 反射异常
public class ReflectionException extends PersistenceException {
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
