package org.apache.ibatis.exceptions;

// 持久化异常
@SuppressWarnings("deprecation")
public class PersistenceException extends IbatisException {
	
	private static final long serialVersionUID = 7701717202650941354L;

	public PersistenceException() {
		super();
	}

	public PersistenceException(String message) {
		super(message);
	}
	
	public PersistenceException(Throwable cause) {
		super(cause);
	}
	
	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
