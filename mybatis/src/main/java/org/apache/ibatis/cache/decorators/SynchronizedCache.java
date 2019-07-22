package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;


/**
 * 同步版本的缓存装饰器
 * 作用: 为Cache提供了同步功能
 * 数据结构: 使用 synchronized
 */
public class SynchronizedCache implements Cache {

	private Cache delegate;
	
	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public synchronized void putObject(Object key, Object value) {
		delegate.putObject(key, value);
	}

	@Override
	public synchronized Object getObject(Object key) {
		return delegate.getObject(key);
	}

	@Override
	public synchronized Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	@Override
	public synchronized void clear() {
		delegate.clear();
	}

	@Override
	public synchronized int getSize() {
		return delegate.getSize();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
	
	@Override	
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

}
