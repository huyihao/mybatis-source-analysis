package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.exceptions.ReflectionException;
import org.apache.ibatis.reflection.MetaObject;

// 默认实现了ObjectWrapperFactory接口的类，正常情况下本类不应该被调用到
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

	@Override
	public boolean hasWrapperFor(Object object) {
		return false;
	}

	@Override
	public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
		throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
	}

}
