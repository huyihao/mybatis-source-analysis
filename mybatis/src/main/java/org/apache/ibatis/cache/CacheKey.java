package org.apache.ibatis.cache;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * CacheKey必须包含多个影响缓存项的因素，不能简单地用String类型表示
 * 这里计算hashCode的原理没有看懂？
 */
public class CacheKey implements Cloneable, Serializable {

	private static final long serialVersionUID = -819053198771289308L;
	
	public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();
	
	private static final int DEFAULT_MULTIPLYER = 37;
	private static final int DEFAULT_HASHCODE = 17;
	
	private int multiplier;           // 参与计算hashcode，默认值是37
	private int hashcode;             // CacheKey对象的hashcode， 默认值是17
	private long checksum;            // 校验和
	private int count;                // updataList集合的个数
	private List<Object> updateList;  // 由该集合中的所有对象共同决定两个CacheKey是否相同
	
	public CacheKey() {
		this.hashcode = DEFAULT_HASHCODE;
		this.multiplier = DEFAULT_MULTIPLYER;
		this.count = 0;
		this.updateList = new ArrayList<Object>();
	}
	
	public CacheKey(Object[] objects) {
		this();
		updateAll(objects);
	}
	
	public void updateAll(Object[] objects) {
		for (Object o : objects) {
			update(o);
		}
	}
	
	public void update(Object object) {
		// 添加数组或集合类型
		if (object != null && object.getClass().isArray()) {
			int length = Array.getLength(object);
			for (int i = 0; i < length; i++) {
				Object element = Array.get(object, i);
				doUpdate(element);
			}
		} else {
			doUpdate(object);
		}
	}
	
	private void doUpdate(Object object) {
		int baseHashCode = object == null ? 1 : object.hashCode();
		// 重新计算count、checksum和hashcode的值
		count++;
		checksum += baseHashCode;
		baseHashCode *= count;
		
		hashcode = multiplier * hashcode + baseHashCode;
		// 将object添加到updateList集合中
		updateList.add(object);
	}
	
	public int getUpdateCount() {
		return updateList.size();
	}
	
	// 判断两个Cache是否相等，必须满足各种计算值相同，且每个updateList中的对象都相等
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		// 对比的对象不是CacheKey，肯定不相等
		if (!(object instanceof CacheKey)) {
			return false;
		}
		
		final CacheKey cacheKey = (CacheKey) object;
		
		if (hashcode != cacheKey.hashcode) {
			return false;
		}
		if (checksum != cacheKey.checksum) {
			return false;
		}
		if (count != cacheKey.count) {
			return false;
		}
		
		for (int i = 0; i < updateList.size(); i++) {
			Object thisObject = updateList.get(i);
			Object thatObject = cacheKey.updateList.get(i);
			if (thisObject == null) {
				if (thatObject != null) {
					return false;
				}
			} else {
				if (!thatObject.equals(thisObject)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return hashcode;
	}
	
	@Override
	public String toString() {
		StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
		for (Object object : updateList) {
			returnValue.append(':').append(object);
		}
		return returnValue.toString();
	}
	
	@Override
	public CacheKey clone() throws CloneNotSupportedException {
		CacheKey clonedCacheKey = (CacheKey) super.clone();
		clonedCacheKey.updateList = new ArrayList<Object>(updateList);
		return clonedCacheKey;
	}
}
