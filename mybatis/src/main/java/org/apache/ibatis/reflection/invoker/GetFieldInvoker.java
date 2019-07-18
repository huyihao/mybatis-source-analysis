package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * 通过反射获取类的属性值，实现了Invoker接口
 */
public class GetFieldInvoker implements Invoker {
	private Field field;
	
	public GetFieldInvoker(Field field) {
		this.field = field;
	}
	
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return field.get(target);
	}

	@Override
	public Class<?> getType() {
		return field.getType();
	}

}
