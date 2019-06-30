package org.apache.ibatis.reflection.wrapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.exceptions.ReflectionException;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * BaseWrapper是一个实现了ObjectWrappr接口的抽象类，封装了MateObject对象，并提供了三个
 * 方法供其子类使用
 */
public abstract class BaseWrapper implements ObjectWrapper {
	protected static final Object[] NO_ARGUMENTS = new Object[0];
	protected MetaObject metaObject;
	
	protected BaseWrapper(MetaObject metaObject) {
		this.metaObject = metaObject;
	}
	
	// 解析属性表达式并获取指定的属性
	protected Object resolveCollection(PropertyTokenizer prop, Object object) {
		if ("".equals(prop.getName())) {
			return object;
		} else {
			return metaObject.getValue(prop.getName());
		}
	}
	
	/**
	 *  解析属性表达式的索引信息，然后获取对应的属性值
	 *  
	 * @param prop
	 * @param collection   必须是是一个数组或者列表对象
	 * @return
	 */
	protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
		if (collection instanceof Map) {
			return ((Map) collection).get(prop.getName());
		} else {
			int i = Integer.valueOf(prop.getIndex());
			if (collection instanceof List) {
				return ((List) collection).get(i);
			} else if (collection instanceof Object[]) {
				return ((Object[]) collection)[i];
			} else if (collection instanceof char[]) {
				return ((char[]) collection)[i];
			} else if (collection instanceof boolean[]) {
				return ((boolean[]) collection)[i];
			} else if (collection instanceof byte[]) {
				return ((byte[]) collection)[i];
			} else if (collection instanceof short[]) {
				return ((short[]) collection)[i];
			} else if (collection instanceof int[]) {
				return ((int[]) collection)[i];
			} else if (collection instanceof long[]) {
				return ((long[]) collection)[i];
			} else if (collection instanceof float[]) {
				return ((float[]) collection)[i];
			} else if (collection instanceof double[]) {
				return ((double[]) collection)[i];
			} else {
				throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
			}
		}
	}
	
	// 解析属性表达式的索引信息，然后设置对应的属性值
	protected void setCollectionValue(PropertyTokenizer prop, Object collection, Object value) {
		if (collection instanceof Map) {
			((Map) collection).put(prop.getIndex(), value);
		} else {
			int i = Integer.valueOf(prop.getIndex());
			if (collection instanceof List) {
				((List) collection).set(i, value);
			} else if (collection instanceof Object[]) {
				((Object[]) collection)[i] = value;
			} else if (collection instanceof char[]) {
				((char[]) collection)[i] = (Character) value;
			} else if (collection instanceof boolean[]) {
				((boolean[]) collection)[i] = (Boolean) value;
			} else if (collection instanceof byte[]) {
				((byte[]) collection)[i] = (Byte) value;
			} else if (collection instanceof short[]) {
				((short[]) collection)[i] = (Short) value;
			} else if (collection instanceof int[]) {
				((int[]) collection)[i] = (Integer) value;
			} else if (collection instanceof long[]) {
				((long[]) collection)[i] = (Long) value;
			} else if (collection instanceof float[]) {
				((float[]) collection)[i] = (Float) value;
			} else if (collection instanceof double[]) {
				((double[]) collection)[i] = (Double) value;
			} else {
				throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
			}
		}
	}
}
