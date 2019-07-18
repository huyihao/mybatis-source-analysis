package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;

/**
 * 日志工厂
 * 使用了适配器模式，将多个日志框架的使用聚合在一起，mybatis统一使用Log接口形式来调用日志功能
 * static静态块中初始化加载多个日志框架适配器，如果加载某个日志适配器成功，则使用它，后面的日志适配器不用
 * 为了避免加载时覆盖，对每个加载日志适配器的方法都进行加锁同步，保证加载串行
 */
public class LogFactory {
	
	public static final String MARKER = "MYBATIS";
	  
	// 记录当前使用的第三方日志组件所对应的适配器的构造方法
	private static Constructor<? extends Log> logConstructor;
	
	static {
		// 针对每种日志组件调用tryImplementation()方法进行尝试加载
		// 组件加载的顺序：
		// 		org.slf4j.Logger -->
		// 		org.apache.commons.logging.Log -->
		//      org.apache.logging.log4j.Logger -->
		//      org.apache.log4j.Logger -->
		//      java.util.logging.Logger -->
		// 		NoLoggingImpl		
		tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useSlf4jLogging();
	        }
	    });
	    tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useCommonsLogging();
	        }
	    });
	    tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useLog4J2Logging();
	        }
	    });
	    tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useLog4JLogging();
	        }
	    });
	    tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useJdkLogging();
	        }
	    });
	    tryImplementation(new Runnable() {
	        @Override
	        public void run() {
	          useNoLogging();
	        }
	    });		
	}
	
	private LogFactory() {}  // 只提供静态方法，不允许实例化日志工厂类
	
	public static Log getLog(Class<?> aClass) {
		return getLog(aClass.getName());
	}
	
	public static Log getLog(String logger) {
		try {
			return logConstructor.newInstance(logger);
		} catch (Throwable t) {
			throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
		}
	}
	
	// 如何要适配更多的其他日志框架，可指定自定义的Log适配器适配
	public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
		setImplementation(clazz);
	}
	
	// 使用Slf4j日志框架
	public static synchronized void useSlf4jLogging() {
		setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
	}
	
	// 使用Commons-logging日志框架
	public static synchronized void useCommonsLogging() {
		setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
	}
	
	// 使用Log4j日志框架
	public static synchronized void useLog4JLogging() {
		setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
	}
	
	public static synchronized void useLog4J2Logging() {
	    setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
	}	
	
	// 使用JDK logger
	public static synchronized void useJdkLogging() {
	    setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
	}
	
	// 使用Java 系统标准输出
	public static synchronized void useStdOutLogging() {
	    setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
	}	
	
	// 当没有使用任何日志框架时，使用NoLoggingImpl，这是一个对Log接口的一个空实现
	public static synchronized void useNoLogging() {
	    setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
	}	
	
	private static void tryImplementation(Runnable runnable) {
		if (logConstructor == null) {
			try {
				runnable.run();
			} catch (Throwable t) {
			}
		}
	}
	
	private static void setImplementation(Class<? extends Log> implClass) {
		try {
			// 获取指定的适配器的构造方法
			Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
			Log log = candidate.newInstance(LogFactory.class.getName());
			if (log.isDebugEnabled()) {
				log.debug("Logging initialized using '" + implClass + "' adapter.");
			}
			logConstructor = candidate;
		} catch (Throwable t) {
			throw new LogException("Error setting Log implementation.  Cause: " + t, t);
		}
	}
}
