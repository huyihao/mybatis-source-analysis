package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultReflectorFactory implements ReflectorFactory {

	private boolean classCacheEnabled = true;
	private final ConcurrentHashMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<Class<?>, Reflector>(); // 使用ConcurrentMap集合实现对Reflector对象缓存
	
	public DefaultReflectorFactory() {}
	
	@Override
	public boolean isClassCacheEnabled() {
		return classCacheEnabled;
	}

	@Override
	public void setClassCacheEnabled(boolean classCacheEnabled) {
		this.classCacheEnabled = classCacheEnabled;
	}

	@Override
	public Reflector findForClass(Class<?> type) {
		if (classCacheEnabled) {
			// 如果有设置了缓存，则先查缓存中是否有类对应的Reflector数据，有就取出来，没有就先创建再缓存
			Reflector cached = reflectorMap.get(type);
			if (cached == null) {
				cached = new Reflector(type);
				reflectorMap.put(type, cached);
			}
			return cached;
		} else {
			return new Reflector(type);
		}
	}

}
