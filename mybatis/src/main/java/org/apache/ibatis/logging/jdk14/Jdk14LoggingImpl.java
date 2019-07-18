package org.apache.ibatis.logging.jdk14;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ibatis.logging.Log;

/**
 * Jdk-logging适配器
 */
public class Jdk14LoggingImpl implements Log {

	private Logger log;

	public Jdk14LoggingImpl(String clazz) {
		log = Logger.getLogger(clazz);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isLoggable(Level.FINER);
	}

	@Override
	public void error(String s, Throwable e) {
		log.log(Level.SEVERE, s, e);
	}

	@Override
	public void error(String s) {
		log.log(Level.SEVERE, s);
	}

	@Override
	public void debug(String s) {
		log.log(Level.FINE, s);
	}

	@Override
	public void trace(String s) {
		log.log(Level.FINER, s);
	}

	@Override
	public void warn(String s) {
		log.log(Level.WARNING, s);
	}
}