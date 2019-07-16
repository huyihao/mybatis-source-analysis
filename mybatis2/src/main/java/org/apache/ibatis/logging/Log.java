package org.apache.ibatis.logging;

/**
 * 日志模块适配器接口
 * 在mybatis中，不管底层调用的是哪个日志框架，业务代码直接调用的形式是适配器接口
 */
public interface Log {
	
	boolean isDebugEnabled();
	
	boolean isTraceEnabled();
	
	// 一般的日志框架都有error、debug、trace、warn等几个日志级别
	// debug一般用于测试环境调试，生产环境关闭
	// error、trace、warn是生产上常用打开的日志级别，分别用于开启打印错误、正常、警告级别日志
    void error(String s, Throwable e);

    void error(String s);

    void debug(String s);

    void trace(String s);

    void warn(String s);
}
