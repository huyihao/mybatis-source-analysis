package org.apache.ibatis.binding;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * Mapper接口和mapper.xml的绑定异常
 */
public class BindingException extends PersistenceException {
	
	private static final long serialVersionUID = -2667911485914347792L;

	public BindingException() {
		super();
	}
	
	public BindingException(String message) {
		super(message);
	}
	
	public BindingException(Throwable cause) {
		super(cause);
	}
	
	public BindingException(String message, Throwable cause) {
		super(message, cause);
	}
}
