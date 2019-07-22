package org.apache.ibatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

/**
 * MapperRegistry是 Mapper 接口及其对应的代理对象工厂的注册中心
 */
public class MapperRegistry {
	
	// Configuration对象，MyBatis全局唯一的配置对象，其中包含了所有配置信息
	private final Configuration config;
	
	// 记录了Mapper接口与对应的MapperProxyFactory之间的关系
	private final Map<Class<?>, MapperProxyFactory<?>> knowMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();
	
	public MapperRegistry(Configuration config) {
		this.config = config;
	}
	
	// 通过Mapper接口的Class对象获取对应的MapperProxy的代理对象，当要执行SQL语句时，需要先获取Mapper，调用的是本方法
	@SuppressWarnings("unchecked")
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knowMappers.get(type);
		if (mapperProxyFactory == null) {
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}
	
	public <T> boolean hasMapper(Class<T> type) {
		return knowMappers.containsKey(type);
	}
	
	// 为Mapper接口Class对象添加相应的MapperProxyFactory到knowMappers集合中
	public <T> void addMapper(Class<T> type) {
		if (type.isInterface()) {
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry");
			}
			boolean loadCompleted = false;
			try {
				knowMappers.put(type, new MapperProxyFactory<>(type));
				// MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				// parser.parse();
				loadCompleted = true;
			} finally {
				if (!loadCompleted) {
					knowMappers.remove(type);
				}
			}
		}
	}
	
	// 返回所有已经注册的Mapper接口的Class对象不可变集合
	public Collection<Class<?>> getMappers() {
		return Collections.unmodifiableCollection(knowMappers.keySet());
	}
	
	// 批量得为一个包中的Mapper接口添加MapperProxyFactory
	public void addMappers(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		for (Class<?> mapperClass : mapperSet) {
			addMapper(mapperClass);
		}
	}
	
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}
}
