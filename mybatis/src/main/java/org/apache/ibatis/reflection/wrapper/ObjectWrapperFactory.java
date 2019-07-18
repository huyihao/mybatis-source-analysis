package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;

// 创建ObjectWrapper对象的工厂接口
public interface ObjectWrapperFactory {
	  boolean hasWrapperFor(Object object);
	  
	  ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);
}
