package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * Mapper代理对象创建工厂，作用:
 * 1) 管理一个接口中有多个方法，一对多的映射关系
 * 2) Mapper方法Method对象跟MapperMethod的映射关系缓存
 */
public class MapperProxyFactory<T> {

	// 当前MapperProxyFactory对象可以创建实现了mapperInterface接口的代理对象，实际执行SQL时由代理对象进行处理
	private final Class<T> mapperInterface;
	
	// 缓存，key是mapperInterface接口中某方法对应的Method对象，value是对应的MapperMethod对象
	private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();
	
	public MapperProxyFactory(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	public Class<T> getMapperInterface() {
		return mapperInterface;
	}
	
	public Map<Method, MapperMethod> getMethodCache() {
		return methodCache;
	}	
	
	public T newInstance(SqlSession sqlSession) {
		final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
		return newInstance(mapperProxy);
	}
	
	@SuppressWarnings("unchecked")
	protected T newInstance(MapperProxy<T> mapperProxy) {
		return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
	}
}
