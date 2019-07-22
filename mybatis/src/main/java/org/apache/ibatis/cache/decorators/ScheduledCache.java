package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 周期性清理缓存的装饰器
 * 作用: 默认每隔1个小时即清理缓存，也可自定义缓存清理周期
 */
public class ScheduledCache implements Cache {

	private Cache delegate;        // 底层被装饰的Cache对象
	protected long clearInterval;  // 两次缓存清理之间的时间间隔
	protected long lastClear;      // 最近一次清理的时间戳	
	
	public ScheduledCache(Cache delegate) {
		this.delegate = delegate;
		clearInterval = 60 * 60 * 1000;
		lastClear = System.currentTimeMillis();
	}
	
	public void setClearInterval(long clearInterval) {
		this.clearInterval = clearInterval;
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}
	
	@Override
	public int getSize() {
		clearWhenStale();
		return delegate.getSize();
	}

	@Override
	public void putObject(Object key, Object value) {
		clearWhenStale();
		delegate.putObject(key, value);
	}

	@Override
	public Object getObject(Object key) {		
		return clearWhenStale() ? null : delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		clearWhenStale();
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		lastClear = System.currentTimeMillis();
		delegate.clear();
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

	// 判断是否已到清理周期，是则清空所有缓存
	private boolean clearWhenStale() {
		if (System.currentTimeMillis() - lastClear > clearInterval) {
			clear();
			return true;
		}
		return false;
	}	
}
