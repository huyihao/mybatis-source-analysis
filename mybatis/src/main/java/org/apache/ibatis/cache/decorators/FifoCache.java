package org.apache.ibatis.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 先入先出版本缓存装饰器
 * 作用: 缓存不可能无限制存储，需要有一定的控制，为了控制缓存的大小
 *      系统需要按照一定的规则清理缓存
 * 数据结构: 基于队列
 */
public class FifoCache implements Cache {

	private final Cache delegate;
	private Deque<Object> keyList;  	// 用于记录进入缓存的先后顺序，使用的是LinkedList<Object>类型的集合对象
										// 相当于一个注册表，不在注册表内的缓存会被删除
	private int size;                   // 记录了缓存项的上线，超过该值，则需要清理最老的缓存项
	
	public FifoCache(Cache delegate) {
		this.delegate = delegate;
		this.keyList = new LinkedList<Object>();
		this.size = 1024;   // 默认缓存上限为1024
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public void putObject(Object key, Object value) {
		cycleKeyList(key);
		delegate.putObject(key, value);
	}

	@Override
	public Object getObject(Object key) {
		return delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		delegate.clear();
		keyList.clear();
	}

	@Override
	public int getSize() {
		return delegate.getSize();
	}
	
	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	// 检测缓存空间是否已超限，如果溢出则清除最旧的缓存
	private void cycleKeyList(Object key) {
		keyList.addLast(key);
		if (keyList.size() > size) {
			Object oldestKey = keyList.getLast();
			delegate.removeObject(oldestKey);
		}
	}
}
