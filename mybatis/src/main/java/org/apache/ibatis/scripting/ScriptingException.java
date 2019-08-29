package org.apache.ibatis.scripting;

import org.apache.ibatis.exceptions.PersistenceException;

public class ScriptingException extends PersistenceException {

	private static final long serialVersionUID = -1301121538518190185L;

	public ScriptingException() {
		super();
	}
	
	public ScriptingException(String message) {
		super(message);
	}
	
	public ScriptingException(Throwable cause) {
		super(cause);
	}
	
	public ScriptingException(String message, Throwable cause) {
		super(message, cause);
	}
}
