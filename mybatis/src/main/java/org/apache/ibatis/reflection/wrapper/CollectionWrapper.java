package org.apache.ibatis.reflection.wrapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

// Collection对象的一个包装类
// 可以看到，CollectionWrapper基本不实现ObjectWrapper接口定义的大部分方法，分析下为什么这么做
// 属于Collection的类型有
// Map、List、Set、Queue、Object[]和普通类型数组（eg: int[]）
// 1) 如果是Map类型，使用MapWrapper
// 2) 非Map类型，用CollectionWrapper，它们存储的直接就是元素值，没有根据属性名获取设置元素值的概念，所以跟属性名属性表达式相关的方法的实现全部抛出UnsupportedOperationException
public class CollectionWrapper implements ObjectWrapper {

	private Collection<Object> object;
	
	public CollectionWrapper(MetaObject metaObject, Collection<Object> object) {
		this.object = object;
	}
	
	@Override
	public Object get(PropertyTokenizer prop) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(PropertyTokenizer prop, Object value) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public String findProperty(String name, boolean useCamelCaseMapping) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getGetterNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getSetterNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getSetterType(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getGetterType(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasSetter(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasGetter(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public void add(Object element) {
		object.add(element);
	}

	@Override
	public <E> void addAll(List<E> element) {
		object.addAll(element);
	}

}
