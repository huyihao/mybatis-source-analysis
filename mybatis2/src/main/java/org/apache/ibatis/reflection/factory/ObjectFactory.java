package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

public interface ObjectFactory {
	void setProperties(Properties properties);
	
	// 使用默认构造器创建一个新对象
	<T> T create(Class<T> type);
	
	// 使用带指定参数类型的构造器用指定参数创建新对象
	<T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);
	
	<T> boolean isCollection(Class<?> type);
}
