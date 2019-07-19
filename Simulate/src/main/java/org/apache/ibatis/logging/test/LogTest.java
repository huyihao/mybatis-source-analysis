package org.apache.ibatis.logging.test;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

public class LogTest {

	private static final Log log = LogFactory.getLog(LogTest.class);
	
	public static void main(String[] args) {
		if (log.isDebugEnabled()) {			
			log.debug("debug msg");
		}
		if (log.isTraceEnabled()) {
			log.trace("trace msg");
		}
		log.error("error msg");
	}

}
