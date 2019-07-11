package org.apache.ibatis.type;

import org.apache.ibatis.exceptions.PersistenceException;

public class TypeException extends PersistenceException {
	
	private static final long serialVersionUID = -4795995491531742495L;

	public TypeException() {
		super();
	}
	
	public TypeException(String message) {
		super(message);
	}
	
	public TypeException(Throwable cause) {
		super(cause);
	}
	
	public TypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
