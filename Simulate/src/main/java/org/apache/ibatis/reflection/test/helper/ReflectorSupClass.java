package org.apache.ibatis.reflection.test.helper;

import java.util.Map;
import java.util.Set;

public class ReflectorSupClass<K, V, S, K2, V2> {
	protected Map<K, V> map;
	protected Set<K> set;
	protected V[] values;
	private S s;
	protected Map<? extends K2, ? super V2> map2;
	
	public Map<K, V> getMap() {
		return map;
	}
	public void setMap(Map<K, V> map) {
		this.map = map;
	}
	
	public Set<K> getSet() {
		return set;
	}
	public void setSet(Set<K> set) {
		this.set = set;
	}
}
