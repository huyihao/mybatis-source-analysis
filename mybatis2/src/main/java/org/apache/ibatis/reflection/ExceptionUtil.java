package org.apache.ibatis.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionUtil {
	
	private ExceptionUtil() {}
	
	public static Throwable unwrapThrowable(Throwable wrapped) {
		Throwable unwrapper = wrapped;
		while (true) {
			if (unwrapper instanceof InvocationTargetException) {
				unwrapper = ((InvocationTargetException) unwrapper).getTargetException();   // 获取抛出的目标异常
			} else if (unwrapper instanceof UndeclaredThrowableException) {
				unwrapper = ((UndeclaredThrowableException) unwrapper).getUndeclaredThrowable();  // 获取抛出的经过检查的未声明异常
			} else {
				return wrapped;
			}
		}
	}
}
