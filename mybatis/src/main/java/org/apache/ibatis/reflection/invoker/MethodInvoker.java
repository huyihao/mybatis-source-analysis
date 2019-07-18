package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 通过反射调用类的方法，实现了Invoker接口
 */
public class MethodInvoker implements Invoker {
	private Method method;
	
	public MethodInvoker(Method method) {
		this.method = method;
	}
	
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return method.invoke(target, args);
	}

	@Override
	public Class<?> getType() {
		return method.getReturnType();
	}

}
