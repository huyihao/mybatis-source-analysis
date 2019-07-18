package org.apache.ibatis.reflection.wrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

// Map对象的一个包装类
// 作用: 解析Map对象的属性表达式，获取和设置对象的属性值
public class MapWrapper extends BaseWrapper {

	private Map<String, Object> map;
	
	public MapWrapper(MetaObject metaObject, Map<String, Object> map) {
		super(metaObject);
		this.map = map;
	}

	// 如何Map对象的Object又嵌套了Map则会走到第一个分支，其余情况会走第二个分支
	@Override
	public Object get(PropertyTokenizer prop) {
		if (prop.getIndex() != null) {
			Object collection = resolveCollection(prop, map);
			return getCollectionValue(prop, collection);
		} else {
			return map.get(prop.getName());
		}
	}

	@Override
	public void set(PropertyTokenizer prop, Object value) {
		if (prop.getIndex() != null) {
			Object collection = resolveCollection(prop, map);
			setCollectionValue(prop, collection, value);
		} else {
			map.put(prop.getName(), value);
		}		
	}

	// ? 为啥不是去Map的keySet里找有没有这个属性？
	// 再想想，其实可以理解，Map随时可以设值，没有限制条件，这个方法其实对于MapWrapper来说并没有什么意义
	// 为了实现BaseWrapper不得不实现的方法
	@Override
	public String findProperty(String name, boolean useCamelCaseMapping) {
		return name;
	}

	@Override
	public String[] getGetterNames() {
		return map.keySet().toArray(new String[map.keySet().size()]);
	}

	@Override
	public String[] getSetterNames() {
		return map.keySet().toArray(new String[map.keySet().size()]);
	}

	@Override
	public Class<?> getSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return Object.class;
			} else {
				return metaValue.getSetterType(prop.getChildren());
			}
		} else {
			if (map.get(name) != null) {
				return map.get(name).getClass();
			} else {
				return Object.class;
			}
		}
	}

	@Override
	public Class<?> getGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return Object.class;
			} else {
				return metaValue.getGetterType(prop.getChildren());
			}
		} else {
			if (map.get(name) != null) {
				return map.get(name).getClass();
			} else {
				return Object.class;
			}
		}
	}

	@Override
	public boolean hasSetter(String name) {
		return true;
	}

	@Override
	public boolean hasGetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (map.containsKey(prop.getIndexedName())) {
				MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
				if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
					return true;
				} else {
					return metaValue.hasGetter(prop.getChildren());
				}
			} else {
				return false;
			}
		} else {
			return map.containsKey(prop.getName());
		}
	}

	@Override
	public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
		Map<String, Object> map = new HashMap<String, Object>();
		set(prop, map);
		return MetaObject.forObject(map, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public void add(Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <E> void addAll(List<E> element) {
		throw new UnsupportedOperationException();		
	}

}
