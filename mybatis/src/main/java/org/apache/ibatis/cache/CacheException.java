package org.apache.ibatis.cache;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 缓存异常
 */
public class CacheException extends PersistenceException {

	private static final long serialVersionUID = -7159113434175572400L;

	public CacheException() {
		super();
	}
	
	public CacheException(String message) {
		super(message);
	}
	
	public CacheException(Throwable cause) {
		super(cause);
	}
	
	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}
}
