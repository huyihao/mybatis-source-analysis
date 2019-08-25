package org.apache.ibatis.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 缓存对象构造器
 */
public class CacheBuilder {
	private String id;                                // Cache对象的唯一标识，一般情况下对应映射文件中的配置namespace
	private Class<? extends Cache> implementation;    // Cache接口的真正实现类，默认值是前面介绍的PerpetualCache
	private List<Class<? extends Cache>> decorators;  // 装饰器集合，默认值包含LruCache.class
	private Integer size;                             // Cache大小
	private Long clearInterval;                       // 清理时间周期
	private boolean readWrite;                        // 是否可读写
	private Properties properties;                    // 其他配置信息
	private boolean blocking;                         // 是否阻塞
	
	public CacheBuilder(String id) {
		this.id = id;
		this.decorators = new ArrayList<Class<? extends Cache>>();
	}

	public CacheBuilder implementation(Class<? extends Cache> implementation) {
		this.implementation = implementation;
		return this;
	}

	public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
		if (decorator != null) {
			this.decorators.add(decorator);
		}
		return this;
	}

	public CacheBuilder size(Integer size) {
		this.size = size;
		return this;
	}

	public CacheBuilder clearInterval(Long clearInterval) {
		this.clearInterval = clearInterval;
		return this;
	}

	public CacheBuilder readWrite(boolean readWrite) {
		this.readWrite = readWrite;
		return this;
	}

	public CacheBuilder blocking(boolean blocking) {
		this.blocking = blocking;
		return this;
	}
  
	public CacheBuilder properties(Properties properties) {
		this.properties = properties;
		return this;
	}	
	
	public Cache build() {
		// 如果 implementation 字段和 decorators 集合为空，则为其设置默认值，implementation 默认
		// 值是 PerpetualCache.class， decorators 集合默认只包含 LruCache.class
		setDefaultImplementations();
		// 根据 implementation 指定的类型，通过反射获取参数为String类型的构造方法
		// 并通过该构造方法创建Cache对象
		Cache cache = newBaseCacheInstance(implementation, id);
		// 根据 <cache> 节点下配置的<property> 信息，初始化Cache对象
		setCacheProperties(cache);
		
		// 检测cache对象的类型，如果是PerpetualCache类型，则为其添加decorators集合中
		// 的装饰器；如果是自定义类型的Cache接口实现，则不添加decorators集合中的装饰器
		if (PerpetualCache.class.equals(cache.getClass())) {
			for (Class<? extends Cache> decorator : decorators) {
				// 通过反射获取参数为Cache类型的构造方法，并通过该构造方法创建装饰器
				cache = newCacheDecoratorInstance(decorator, cache);
				setCacheProperties(cache);   // 配置Cache对象的属性						  
			}
			// 添加MyBatis中提供的标准装饰器
			setStandardDecorators(cache);
		} else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
			// 如果不是LoggingCache的子类，则添加LoggingCache装饰器
			cache = new LoggingCache(cache);
		}
		return cache;
	}
	
	// 设置默认的缓存实现类和装饰器
	private void setDefaultImplementations() {
		if (implementation == null) {
			implementation = PerpetualCache.class;
			if (decorators.isEmpty()) {
				decorators.add(LruCache.class);
			}
		}
	}
	
	// 获取基础缓存实现类的实例
	private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
		Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
		try {
			return cacheConstructor.newInstance(id);
		} catch (Exception e) {
			throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
		}
	}
	
	// 获取基础缓存实现类的构造器
	private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
		try {
			return cacheClass.getConstructor(String.class);
		} catch (Exception e) {
		    throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  " +
		              "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
		} 
	}
	
	// 通过反射根据传入的属性值设置Cache属性
	// 只支持简单类型及其包装类型，不支持其他类型属性值设置
	private void setCacheProperties(Cache cache) {
		if (properties != null) {
			MetaObject metaCache = SystemMetaObject.forObject(cache);
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				String name = (String) entry.getKey();    // 获取属性名
				String value = (String) entry.getValue(); // 获取属性类型
				// 先判断对象中是否含有该属性，有则将设置的属性值转化为相应的类型
				if (metaCache.hasSetter(name)) {
					Class<?> type = metaCache.getSetterType(name);
					if (String.class == type) {
						metaCache.setValue(name, value);
					} else if (int.class == type || Integer.class == type) {
						metaCache.setValue(name, Integer.valueOf(value));
					} else if (long.class == type || Long.class == type) {
						metaCache.setValue(name, Long.valueOf(value));
					} else if (short.class == type || Short.class == type) {
			            metaCache.setValue(name, Short.valueOf(value));
			        } else if (byte.class == type || Byte.class == type) {
			            metaCache.setValue(name, Byte.valueOf(value));
			        } else if (float.class == type || Float.class == type) {
			            metaCache.setValue(name, Float.valueOf(value));
			        } else if (boolean.class == type || Boolean.class == type) {
			            metaCache.setValue(name, Boolean.valueOf(value));
			        } else if (double.class == type || Double.class == type) {
			            metaCache.setValue(name, Double.valueOf(value));
			        } else {
			            throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
			        }
				}
			}
		}
	}
	
	// 获取缓存装饰器实例
	private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
		Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
		try {
			return cacheConstructor.newInstance(base);
		} catch (Exception e) {
			throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
		}
	}
	
	// 获取缓存装饰器构造器
	private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
		try {
			return cacheClass.getConstructor(Cache.class);
		} catch (Exception e) {
		    throw new CacheException("Invalid cache decorator (" + cacheClass + ").  " +
		              "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
		}
	}
	
	// 根据CacheBuilder构造方法传入的<Cache>节点的属性值，添加相应的装饰器
	private Cache setStandardDecorators(Cache cache) {
		try {
			MetaObject metaCache = SystemMetaObject.forObject(cache);
			if (size != null && metaCache.hasSetter("size")) {
				metaCache.setValue("size", size);
			}
			if (clearInterval != null) {
				cache = new ScheduledCache(cache);
				((ScheduledCache) cache).setClearInterval(clearInterval);
			}
			if (readWrite) {
				cache = new SerializedCache(cache);
			}
			cache = new LoggingCache(cache);
			cache = new SynchronizedCache(cache);   // 同步跟阻塞加在一起了，是不是没有必要？
			if (blocking) {
				cache = new BlockingCache(cache);
			}
			return cache;
		} catch (Exception e) {
			throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
		}
	}
}
