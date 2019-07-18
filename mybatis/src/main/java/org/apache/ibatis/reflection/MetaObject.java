package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * MetaObject的作用:<br>
 * (1) 解析属性表达式<br>
 * (2) 根据Object对象类型创建ObjectWrapper对象<br>
 * (3) 通过调用ObjectWrapper对象<br>
 * &emsp;3.1 获取Object对象某个属性的类型<br>
 * &emsp;3.2 判断Object对象是否有某个属性<br>
 * &emsp;3.3 获取Object对象所有可读、可写属性名集合<br>
 * &emsp;3.4 根据属性表达式获取Object对象某个属性的值<br>
 * &emsp;3.5 根据属性表达式设置Object对象某个属性的值<br>
 */
public class MetaObject {
	
	private Object originalObject;                  // 原始Java对象
	private ObjectFactory objectFactory;            // 负责产生一个类的实例化对象，当根据属性表达式解析属性需要为属性创建一个MetaObject对象，而属性值又为空时，需要使用ObjectFactory创建属性对象
	private ObjectWrapper objectWrapper;            // 封装操作originalObject对象元信息的类
	private ObjectWrapperFactory objectWrapperFactory;  // 负责创建ObjectWrapper对象的工厂对象
	private ReflectorFactory reflectorFactory;      // 用于创建并缓存Reflector对象的工厂对象
	
	private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		this.originalObject = object;
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;
		
		if (object instanceof ObjectWrapper) {
			this.objectWrapper = (ObjectWrapper) objectWrapper;  // 若原始对象已经是个ObjectWrapper对象，则直接使用
		} else if (objectWrapperFactory.hasWrapperFor(object)) {
			// 若objectWrapperFactory能够为该原始对象创建对应的ObjectWrapper对象，则优先使用
			// ObjectWrapperFactory，而DefaultObjectWrapperFactory.hasWrapperFor()始终
			// 返回false。用户可以自定义ObjectWrapperFactory实现进行扩展
			this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
		} else if (object instanceof Map) {
			// 若原始对象为Map类型，则创建MapWrapper对象
			this.objectWrapper = new MapWrapper(this, (Map) object);
		} else if (object instanceof Collection) {
			// 若原始对象为Collection类型，则创建CollectionWrapper对象
			this.objectWrapper = new CollectionWrapper(this, (Collection) object);
		} else {
			// 若原始对象是普通的JavaBean对象，则创建BeanWrapper对象
			this.objectWrapper = new BeanWrapper(this, object);
		}
	}
	
	public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		if (object == null) {
			return SystemMetaObject.NULL_META_OBJECT;
		} else {
			return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
		}
	}
	
	// getters
	// [start]
	public Object getOriginalObject() {
		return originalObject;
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}
	
	public ObjectWrapper getObjectWrapper() {
		return objectWrapper;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}
	// [end]

	// 包装依赖ObjectWrapper的同名方法
	// [start]
	public String findProperty(String propName, boolean useCamelCaseMapping) {
		return objectWrapper.findProperty(propName, useCamelCaseMapping);
	}
	
	public String[] getGetterNames() {
		return objectWrapper.getGetterNames();
	}
	
	public String[] getSetterNames() {
		return objectWrapper.getSetterNames();
	}
	
	public Class<?> getSetterType(String name) {
		return objectWrapper.getSetterType(name);
	}
	
	public Class<?> getGetterType(String name) {
		return objectWrapper.getGetterType(name);
	}
	
	public boolean hasSetter(String name) {
		return objectWrapper.hasSetter(name);
	}
	
	public boolean hasGetter(String name) {
		return objectWrapper.hasGetter(name);
	}
	// [end]
	
	public MetaObject metaObjectForProperty(String name) {
		Object value = getValue(name);
		return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
	}
	
	/**
	 * eg: order[1].items[0].name     PS. 处理order属性表达式的值是在order所属的对象(假设是User)的MetaObject对象中处理的
	 * 获取值的过程:
	 * (1) 解析属性表达式的indexedName部分: order[1]，这是个数组，通过递归调用User类的MetaObject对象的getValue方法获取这个表达式的实际对象值
	 * (2) 用(1)中解析出来的Order对象，创建Order对象的MetaObejct对象
	 * (3) 用(2)中创建出来的MetaObject对象解析属性表达式的子表达式"items[0].name"
	 * (4) 同样的 ，用(1)、(2)创建Order对象的MetaObject对象的方法去获取"items[0]"的对象值并创建Items对象的MetaObject对象
	 * (5) 调用(4)中的MetaObject对象的getValue("name")方法，获取items[0]中的属性名为name的成员的值
	 */
	public Object getValue(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);    // 解析属性表达式
		if (prop.hasNext()) {    // 处理子表达式
			// 根据解析后制定的属性，创建相应的MetaObject对象
			MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				return null;
			} else {
				// 递归处理子表达式
				return metaValue.getValue(prop.getChildren());  
			}
		} else {
			// 递归的出口
			return objectWrapper.get(prop);
		}
	}
	
	/**
	 * eg: order[1].items[0].name
	 * 设置值的过程：
	 * 原理跟上面获取值的过程大同小异，关键在于假设某段表达式的indexedName代表的属性值为null后面又有子表达式怎么处理?
	 * (1) 假设order[1]是有值的，创建其MetaObject对象后，接着对子表达式"items[0].name"进行解析
	 * (2) 对"items[0].name" 获取indexedName为"items[0]",获取其值时却为null,这是返回一个空的MetaObjact对象,无法对Items对象的name属性赋值
	 * (3) 所有要求所有的ObjectWrapper的实现类里，要有一个应对(2)情况的方法instantiatePropertyValue，该方法实际上就是初始化一个类对象
	 *     比如(2)中的情况，会为它创建一个Items obj = new Items();的对象，再用MetaClass.forObject()创建对象
	 * (4) 解决完(2)、(3)描述的情况后，对对应的对象的属性调用ObjectWrapper的实现类的set方法进行设值，set方法用到了MetaClass的方法，MetaClass的方法又是
	 *     建立在Reflector的基础上，其依赖关系链：
	 *     MetaObject --> ObjectWrapper --> MetaClass --> Reflector --> TypeParameterResolver
	 *                 (Object, MetaClass)  (Invoker)
	 *                     |
	 *                     |-- BaseWrapper
	 *                     |   |-- BeanWrapper
	 *                     |   |-- MapWrapper
	 *                     |
	 *                     |-- ColleactionWrapper
	 * Utils: PropertyTokenizer、PropertyNamer、PropertyCopier
	 */	
	public void setValue(String name, Object value) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
			if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
				if (value == null && prop.getChildren() != null) {
					return ;
				} else {
					// 为指定的属性表达式创建MetaObject对象，其实就是对值为null的对象初始化一个MetaObject对象，使其能被赋值
					metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
				}
			}
			metaValue.setValue(prop.getChildren(), value);   // 递归入口
		} else {
			objectWrapper.set(prop, value);   // 递归出口
		}
	}
	
	public boolean isCollection() {
		return objectWrapper.isCollection();
	}
	
	public void add(Object element) {
		objectWrapper.add(element);
	}
	
	public <E> void addAll(List<E> list) {
		objectWrapper.addAll(list);
	} 
}
