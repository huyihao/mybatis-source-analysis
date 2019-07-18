package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * Invoker接口定义了通过反射获取设置类属性、调用类方法的形式
 */
public interface Invoker {
	Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;
	Class<?> getType();
}
