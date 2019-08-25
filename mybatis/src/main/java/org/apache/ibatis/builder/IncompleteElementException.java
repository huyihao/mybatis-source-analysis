package org.apache.ibatis.builder;

import org.apache.ibatis.exceptions.PersistenceException;

public class IncompleteElementException extends PersistenceException {
	
	private static final long serialVersionUID = 8582566336317812097L;

	public IncompleteElementException() {
		super();
	}

	public IncompleteElementException(String message, Throwable cause) {
	    super(message, cause);
	}

	public IncompleteElementException(String message) {
	    super(message);
	}

	public IncompleteElementException(Throwable cause) {
	    super(cause);
	}
}
