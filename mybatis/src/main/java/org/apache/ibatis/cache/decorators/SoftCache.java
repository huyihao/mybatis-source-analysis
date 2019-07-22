package org.apache.ibatis.cache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 软引用版本缓存装饰器
 * 作用: 在内存不足时，GC会回收软引用指向的内存 
 * 数据结构: LinkedList、ReferenceQueue，分别存储强引用对象和软引用对象被回收时的通知引用队列
 */
public class SoftCache implements Cache {
	// 在SoftCache中，最近使用的一部分缓存项不会被GC回收，这就是通过将其value添加到
	// hardLinksToAvoidGarbageCollection集合中实现的（即有强引用指向其value）
	// hardLinksToAvoidGarbageCollection集合是LinkedList<Object>类型
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	
	// ReferenceQueue，引用队列，用于记录已经被GC回收的缓存项所对应的SoftEntry对象
	private final ReferenceQueue<Object> queueOgGarbageCollectedEntries;
	
	private final Cache delegate;  // 被装饰的底层cache对象
	
	private int numberOfHardLinks;  // 强连接的个数，默认值是256
	
	public SoftCache(Cache delegate) {
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
		removeGarbageCollectionItems();    // 清楚已经被GC回收的缓存项
		// 注意，这里不是简单缓存value，而是包装了key、value的SoftEntry对象
		delegate.putObject(key, new SoftEntry(key, value, queueOgGarbageCollectedEntries));
	}

	@Override
	public Object getObject(Object key) {
		Object result = null;
		// 从缓存中查找对应的缓存项
		@SuppressWarnings("unchecked")
		SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
		if (softReference != null) {        // 检测缓存中是否有对应的缓存项
			result = softReference.get();   // 获取SoftReference引用的value
			if (result == null) {           // 已经被GC回收
				delegate.removeObject(key); // 从缓存中清除对应的缓存项 
			} else { // 未被GC回收
				synchronized (hardLinksToAvoidGarbageCollection) {    // 缓存项的value添加到hardLinksToAvoidGarbageCollection集合中保存
					hardLinksToAvoidGarbageCollection.addFirst(result);
					if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) { 
						hardLinksToAvoidGarbageCollection.removeLast();
					} // 超过numberOfHardLinks，则将最老的缓存从hardLinksToAvoidGarbageCollection集合中清除，有点类似于先进先出队列
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
		synchronized (hardLinksToAvoidGarbageCollection) {   // 清理强引用集合
			hardLinksToAvoidGarbageCollection.clear();
		}
		removeGarbageCollectionItems();                      // 清理被GC回收的缓存项
		delegate.clear();                                    // 清理底层delegate缓存中的缓存项
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}
	
	private void removeGarbageCollectionItems() {
		SoftEntry sv;
		// 将已经被GC回收的value对象对应的缓存项清除
		// 当value被回收时，JVM会将其添加到关联的引用队列中，检测这个引用队列就知道
		// 哪些SoftEntry对象的value已经被回收了，达到一个检测通知信息的效果		
		while ((sv = (SoftEntry) queueOgGarbageCollectedEntries.poll()) != null) {
			delegate.removeObject(sv.key);
		}
	}
	
	// 实现软引用缓存的核心类
	private static class SoftEntry extends SoftReference<Object> {
		private final Object key;
		
		// 如果value被GC回收了，SoftEntry对象会关联到garbageCollectionQueue中
		SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			super(value, garbageCollectionQueue); // 指向value的引用是软引用，并关联了引用队列
			this.key = key;  // 强引用
		}
	}	
}
