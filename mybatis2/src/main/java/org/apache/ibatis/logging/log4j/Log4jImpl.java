package org.apache.ibatis.logging.log4j;

import org.apache.ibatis.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Log4j日志框架适配器
 */
public class Log4jImpl implements Log {
  
	private static final String FQCN = Log4jImpl.class.getName();

	private Logger log;

	public Log4jImpl(String clazz) {
		log = Logger.getLogger(clazz);
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	@Override
	public void error(String s, Throwable e) {
		log.log(FQCN, Level.ERROR, s, e);
	}

	@Override
	public void error(String s) {
		log.log(FQCN, Level.ERROR, s, null);
	}

	@Override
	public void debug(String s) {
		log.log(FQCN, Level.DEBUG, s, null);
	}

	@Override
	public void trace(String s) {
		log.log(FQCN, Level.TRACE, s, null);
	}

	@Override
	public void warn(String s) {
		log.log(FQCN, Level.WARN, s, null);
	}
}