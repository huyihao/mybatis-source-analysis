package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * 使用动态代理，功能实现依赖于MapeprMethod
 * 维护 Method 和其对应的 MapperMethod 之间的映射关系，并对其做了缓存
 * MapperProxy主要是对Mapper接口定义的方法进行了拦截，拦截到了之后的实际处理交给MapperMethod负责
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = 5407795792297704674L;
	private final SqlSession sqlSession;      // 记录了关联的SqlSession对象
	private final Class<T> mapperInterface;   // Mapper接口对应的Class对象
	private final Map<Method, MapperMethod> methodCache; 	// 用于缓存MapperMethod对象，其中key是Mapper接口中方法对应的Method对象，value是对应的
															// MapperMethod对象。MapperMethod对象会完成参数转换以及SQL语句的执行功能
															// 需要注意的是，MapperMethod中并不记录任何状态相关的信息，所以可以在多个代理对象之间共享
	
	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 如果调用的目标方法继承自Object，则直接调用，类似的用法在反射器Reflector、BaseJdbcLogger等地方已经见过很多次了
		// 目的就是代理拦截到真正想拦截的Mapper接口中定义的方法
		if (Object.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		}
		final MapperMethod mapperMethod = cachedMapperMethod(method);		
		return mapperMethod.execute(sqlSession, args);
	}

	// 从cache缓存中获取Method对应的MapperMethod对象，缓存中没有则创建，并缓存到cache中
	private MapperMethod cachedMapperMethod(Method method) {
		MapperMethod mapperMethod = methodCache.get(method);
		if (mapperMethod == null) {
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			methodCache.put(method, mapperMethod);
		}
		return mapperMethod;
	}
}
