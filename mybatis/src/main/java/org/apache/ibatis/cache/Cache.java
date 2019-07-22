package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * MyBatis缓存模块核心接口
 */
public interface Cache {
	String getId();                            // 缓存对象的id
	
	void putObject(Object key, Object value);  // 向缓存对象中添加数据，一般情况下，key是CacheKey，value是查询结果
	
	Object getObject(Object key);              // 根据指定的key，在缓存中查找对应的结果对象
	
	Object removeObject(Object key);           // 删除key对应的缓存项
	
	void clear();                              // 清除缓存
	
	int getSize();                             // 缓存项的个数，该方法不会被MyBatis核心代码使用，所以可提供空实现
	
	ReadWriteLock getReadWriteLock();  		   // 获取读写锁，该方法不会被MyBatis核心代码使用，所以可提供空实现
}
