package org.apache.ibatis.logging.stdout;

import org.apache.ibatis.logging.Log;

/**
 * 使用标准输出日志的适配器
 */
public class StdOutImpl implements Log {

	public StdOutImpl(String clazz) {}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public void error(String s, Throwable e) {
		System.err.println(s);
		e.printStackTrace(System.err);
	}

	@Override
	public void error(String s) {
		System.err.println(s);
	}

	@Override
	public void debug(String s) {
		System.out.println(s);
	}

	@Override
	public void trace(String s) {
		System.out.println(s);
	}

	@Override
	public void warn(String s) {
		System.out.println(s);
	}
}