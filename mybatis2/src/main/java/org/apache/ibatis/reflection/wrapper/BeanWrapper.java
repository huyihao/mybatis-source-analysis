package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

// 普通JavaBean对象的一个包装类
// 作用: 解析JavaBean对象的属性表达式，获取和设置对象的属性值
// ObjectWrapper接口定义了所有对象包装器应该有的功能
// BaseWrapper抽象类实现了当所有对象包装器都会遇到的解析[含类型为数组或集合的子属性的表达式]时，需要获取设置数组或集合中指定下标的元素的值的功能，所以它被所有非抽象可实例化使用的对象包装器所继承
// 对象包装器本质上还是依赖于MetaClass类的功能，其他的都是大量的递归解析属性表达式的过程
public class BeanWrapper extends BaseWrapper {

	private Object object;           // JavaBean对象
	private MetaClass metaClass;     // JavaBean类对应的MetaClass对象
	
	public BeanWrapper(MetaObject metaObject, Object object) {
		super(metaObject);
		this.object = object;
		this.metaClass = MetaClass.forClass(object.getClass(), metaObject.getReflectorFactory());
	}

	// MetaObject会保证传进来的prop.hasNext() = false
	@Override
	public Object get(PropertyTokenizer prop) {
		// 存在索引信息，则表示属性表达式中的indexname部分为集合或数组，表明想根据索引获取集合或数组中某个元素，eg: "list[1]"
		if (prop.getIndex() != null) {
			// 通过MetaObject.getValue()方法获取Object对象中的指定集合属性
			Object collection = resolveCollection(prop, object);
			return getCollectionValue(prop, collection);
		} else {
			// 不存在索引信息，则indexname部分为普通对象，查找并调用Invoker相关方法获取属性，eg: "username"
			return getBeanProperty(prop, object);
		}
	}

	@Override
	public void set(PropertyTokenizer prop, Object value) {
		if (prop.getIndex() != null) {
			Object collection = resolveCollection(prop, object);
			setCollectionValue(prop, collection, value);
		} else {
			setBeanProperty(prop, object, value);
		}		
	}

	@Override
	public String findProperty(String name, boolean useCamelCaseMapping) {
		return metaClass.findProperty(name, useCamelCaseMapping);
	}

	@Override
	public String[] getGetterNames() {
		return metaClass.getGetterNames();
	}

	@Override
	public String[] getSetterNames() {
		return metaClass.getSetterNames();
	}

	@Override
	public Class<?> getSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			// 属性表达式对应的子属性值为空，则创建的MetaObejct就是一个空值的SystemMetaObject.NULL_META_OBJECT
			// 这时候表达式还有children，没办法解析空值对象的子属性并为其创建MetaObject，只能通过MetaClass获取类的元信息
			// 来获取children的表达式表示的属性类型
			// eg: "order[0].item"
			//     indexName部分为 "order[0]"，children部分为 "item"
			// 假如当前BeanWrapper对应的Object对象中的order属性是空的，则 "order[0]" 对象的MetaObject对象就是
			// SystemMetaObject.NULL_META_OBJECT，这时其objectWrapper属性为DefaultObjectWrapperFactory类型
			// 无法创建对应的ObjectWrapper对象，由于MetaObject.getSetterType()依赖于ObjectWrapper，所以无法获取 "item" 的属性类型
			//
			// [思考] 既然获取的是属性类型，这是属于类的元信息，并且MetaObject实际上也是依赖MetaClass去解析属性类型，
			//       MetaClass也支持多层的属性表达式，为何不直接调用MetaClass类的方法？递归绕这么一大圈，感觉没啥卵用
			MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return metaClass.getSetterType(name);    
			} else {
				return metaValue.getSetterType(prop.getChildren());
			}
		} else {
			return metaClass.getSetterType(name);
		}
	}

	@Override
	public Class<?> getGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return metaClass.getGetterType(name);
			} else {
				return metaValue.getGetterType(prop.getChildren());
			}
		} else {
			return metaClass.getGetterType(name);
		}
	}

	@Override
	public boolean hasSetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (metaClass.hasSetter(prop.getIndexedName())) {
				MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
				if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
					return metaClass.hasSetter(name);
				} else {
					return metaValue.hasSetter(prop.getChildren());
				}
			} else {
				return false;
			}
		} else {
			return metaClass.hasSetter(name);
		}
	}

	@Override
	public boolean hasGetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (metaClass.hasGetter(prop.getIndexedName())) {
				MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
				if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
					return metaClass.hasGetter(name);
				} else {
					return metaValue.hasGetter(prop.getChildren());
				}
			} else {
				return false;
			}
		} else {
			return metaClass.hasGetter(name);
		}
	}

	@Override
	public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
		MetaObject metaValue = null;
		Class<?> clazz = getSetterType(prop.getName());
		try {
			Object newObject = objectFactory.create(clazz);
			metaValue = MetaObject.forObject(newObject, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
			set(prop, newObject);
		} catch (Exception e) {
			throw new ReflectionException("Cannot set value if property '" + name + "' because '" + name + "' is null and cannot be instantiated on instance of " + clazz.getName() + ". Cause:" + e.toString(), e);
		}
		return metaValue;
	}

	// 对于BeanWrapper来说，这几个重写的方法是冗余的，因为对JavaBean对象的处理是以这一整个对象来的，
	// 肯定不是数组，自然也就没有添加元素列表之类的说法
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

	private Object getBeanProperty(PropertyTokenizer prop, Object object) {
		try {
			Invoker method = metaClass.getGetInvoker(prop.getName());
			try {
				return method.invoke(object, NO_ARGUMENTS);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			} 
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			// 如果一个方法抛出一个非运行时异常，则会要求调用代码必须捕获异常，这里捕获非运行异常并抛出运行异常
			throw new ReflectionException("Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
		}
	}
	
	private void setBeanProperty(PropertyTokenizer prop, Object object, Object value) {
		try {
			Invoker method = metaClass.getSetInvoker(prop.getName());
			try {
				method.invoke(object, new Object[] { value });
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new ReflectionException("Could not set property '" + prop.getName() + "' with value '" + value + "' Cause: " + t.toString(), t);
		}
	}
}
