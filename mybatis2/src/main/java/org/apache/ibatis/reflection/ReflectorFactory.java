package org.apache.ibatis.reflection;

public interface ReflectorFactory {
	// 反射器工厂类是否会缓存Reflector对象
	boolean isClassCacheEnabled();
	
	void setClassCacheEnabled(boolean classCacheEnabled);
	// 从缓存中寻找Class对应的Reflector对象，没有缓存则创建
	Reflector findForClass(Class<?> type);
}
