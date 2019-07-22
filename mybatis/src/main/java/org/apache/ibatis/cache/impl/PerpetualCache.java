package org.apache.ibatis.cache.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * 装饰器模式的基础具体Cache组件实现类
 * 主要是用一个HashMap简单缓存对象，本类是所有缓存装饰器类的基础类
 */
public class PerpetualCache implements Cache {

	private String id;   // Cache对象的唯一标识
	private Map<Object, Object> cache = new HashMap<Object, Object>();  // 用于记录缓存项的Map对象
	
	public PerpetualCache(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void putObject(Object key, Object value) {
		cache.put(key, value);
	}

	@Override
	public Object getObject(Object key) {
		return cache.get(key);
	}

	@Override
	public Object removeObject(Object key) {
		return cache.remove(key);
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public int getSize() {
		return cache.size();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cache)) {
			return false;
		}
		
		Cache otherCache = (Cache) o;
		return getId().equals(otherCache.getId());
	}

	@Override
	public int hashCode() {
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		return getId().hashCode();
	}
}
