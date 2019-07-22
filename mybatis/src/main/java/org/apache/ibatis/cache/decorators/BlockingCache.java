package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * 阻塞版本的缓存装饰器: 保证只有一个线程到数据库中查找指定key对应的数据
 * 数据结构: 基于ConcurrentHashMap
 */
public class BlockingCache implements Cache {
	
	private long timeout;   // 阻塞超时时长
	private final Cache delegate;   // 被装饰的底层Cache对象
	private final ConcurrentHashMap<Object, ReentrantLock> locks;  // 每个key都有对应的ReentrantLock
	
	public BlockingCache(Cache delegate) {
		this.delegate = delegate;
		this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	
	// 如果是添加或更新缓存，则key对应的锁应该被释放掉
	// 为什么putObject不用加锁？
	// 因为我们查询缓存一般都是先查询，如果没有则用锁阻塞其他线程，去数据库或者其他数据源获取（一般这个过程比较慢）
	// 获取到之后再写入缓存，写入之后再释放锁
	@Override
	public void putObject(Object key, Object value) {
		try {
			delegate.putObject(key, value);
		} finally {
			releaseLock(key);			
		}		
	}

	@Override
	public Object getObject(Object key) {
		// 如果锁的超时时间未过，或者锁的当前线程已结束，都会抛出异常
		acquireLock(key);  // 获取该key对应的锁
		Object value = delegate.getObject(key);  // 查询key
		if (value != null) {   // 缓存有key对应的缓存项，释放锁，否则继续持有锁
			releaseLock(key);
		}
		return value;   // 如果value是空，锁会继续保持，阻塞掉其他来获取key对应缓存值的线程
	}

	// 为什么这里只是释放锁，而没有真正移除缓存？
	@Override
	public Object removeObject(Object key) {
		releaseLock(key);
		return null;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public int getSize() {
		return delegate.getSize();
	}

	// 为什么这里也没有返回真正的锁？
	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}
	
	// 根据key获取对应缓存的锁
	private ReentrantLock getLockForKey(Object key) {
		// 创建一个新的锁，如果锁缓存中能不到对应key的锁，则添加
		ReentrantLock lock = new ReentrantLock();
		// 如果locks中存在key对应的锁则取用，否则插入新创建的锁
		ReentrantLock previous = locks.putIfAbsent(key, lock);
		return previous == null ? lock : previous;
	}
	
	// 加锁
	private void acquireLock(Object key) {
		Lock lock = getLockForKey(key);
		if (timeout > 0) {
			try {
				// 如果在给定的等待时间内，锁已经自由并且当前线程未中断，则返回true
				boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
				if (!acquired) {  // 超时，抛出异常
					throw new CacheException("Could'n get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
				}
			} catch (InterruptedException e) {   // 线程被中断，抛出异常
				throw new CacheException("Got interrupted while trying to acqiure lock for key " + key, e);
			}
		} else {
			lock.lock();
		}
	}
	
	// 释放锁
	private void releaseLock(Object key) {
		ReentrantLock lock = locks.get(key);
		// 锁是否被当前线程持有
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
