package org.apache.ibatis.cache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 弱引用版本装饰器
 * 作用: 弱引用的对象会在GC时自动被回收
 * 数据结构: LinkedList、ReferenceQueue，分别存储强引用对象和弱引用对象被回收时的通知引用队列
 */
public class WeakCache implements Cache {
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	private final ReferenceQueue<Object> queueOgGarbageCollectedEntries;
	private final Cache delegate;
	private int numberOfHardLinks;
	
	public WeakCache(Cache delegate) {
		this.delegate = delegate;
		this.numberOfHardLinks = 256;
		this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
		this.queueOgGarbageCollectedEntries = new ReferenceQueue<Object>();
	}	

	@Override
	public String getId() {		
		return delegate.getId();
	}

	@Override
	public int getSize() {
		removeGarbageCollectionItems();
		return delegate.getSize();
	}	
	
	public void setSize(int size) {
		this.numberOfHardLinks = size;
	}
	
	@Override
	public void putObject(Object key, Object value) {
		removeGarbageCollectionItems();
		delegate.putObject(key, new WeakEntry(key, value, queueOgGarbageCollectedEntries));
	}

	@Override
	public Object getObject(Object key) {
		Object result = null;
		@SuppressWarnings("unchecked")
		WeakReference<Object> weakReference = (WeakReference<Object>) delegate.getObject(key);
		if (weakReference != null) {
			result = weakReference.get();
			if (result == null) {
				delegate.removeObject(key);
			} else {
				// 为什么SoftCache那里就用了锁，这里却不用？
				hardLinksToAvoidGarbageCollection.addFirst(result);
				if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) { 
					hardLinksToAvoidGarbageCollection.removeLast();
				}				
			}
		}
		return result;
	}

	@Override
	public Object removeObject(Object key) {
		removeGarbageCollectionItems();
		return delegate.getObject(key);
	}

	@Override
	public void clear() {
		hardLinksToAvoidGarbageCollection.clear();
		removeGarbageCollectionItems();
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	} 
	
	private void removeGarbageCollectionItems() {
		WeakEntry sv;
		while ((sv = (WeakEntry) queueOgGarbageCollectedEntries.poll()) != null) {
			delegate.removeObject(sv.key);
		}
	}
	
	// 实现弱引用的核心类
	private static class WeakEntry extends WeakReference<Object> {
		private final Object key;
		
		public WeakEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			super(value, garbageCollectionQueue);
			this.key = key;
		}
		
	}	
}
