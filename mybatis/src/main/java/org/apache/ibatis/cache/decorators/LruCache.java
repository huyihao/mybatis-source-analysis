package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * LruCache: 根据最少使用算法 (Least Recently Used, LRU) 进行缓存清理的装饰器
 * 数据结构: 基于LinkedHashMap的removeEldestEntry()方法，LRU 算法实际上是 LinkedHashMap 中实现的
 */
public class LruCache implements Cache {

	private final Cache delegate;
	private Map<Object, Object> keyMap;
	private Object eldestKey;
	
	public LruCache(Cache delegate) {
		this.delegate = delegate;
		setSize(1024);
	}
	
	@Override
	public String getId() {		
		return delegate.getId();
	}
	
	@Override
	public int getSize() {		
		return delegate.getSize();
	}
	
	public void setSize(final int size) {
		// 注意LinkedHashMap构造函数的第三个参数，true表示该LinkedHashMap记录的顺序是
		// access-order，也就是说LinkedHashMap.get()方法会改变其记录的顺序		
		keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
			private static final long serialVersionUID = 4267176411845948333L;

			// 当调用LinkedHashMap.put()方法时，会调用该方法
			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
				boolean tooBig = size() > size;
				if (tooBig) {
					eldestKey = eldest.getKey();
				}
				return tooBig;
			}
		};
	}

	@Override
	public void putObject(Object key, Object value) {
		delegate.putObject(key, value);
		cycleKeyList(key);
	}

	@Override
	public Object getObject(Object key) {
		keyMap.get(key);   // 触发 LinkedHashMap 中的LRU算法，会对记录的顺序做调整
		return delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		// Q: 为什么移除缓存不同步移除对应的keyList中的key？
		// A: 因为LinkedHashMap查询块，删除慢，借助LRU算法慢慢淘汰不被使用的key即可
		// keyMap.remove(key);
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		delegate.clear();
		keyMap.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	// 每次新增一个缓存，就会对应地往key缓存列表中添加缓存，触发LinkedHashMap.removeEldestEntry()
	// 方法对缓存key列表是否空间已满的检测，如果是的话就会给eldestKey赋值
	private void cycleKeyList(Object key) {
		keyMap.put(key, key);
		if (eldestKey != null) {
			delegate.removeObject(eldestKey);
			eldestKey = null;
		}
	}
}
