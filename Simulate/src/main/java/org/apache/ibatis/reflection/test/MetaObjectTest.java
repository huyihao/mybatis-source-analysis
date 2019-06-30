package org.apache.ibatis.reflection.test;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.test.helper.Item;
import org.apache.ibatis.reflection.test.helper.Order;
import org.apache.ibatis.reflection.test.helper.Tele;
import org.apache.ibatis.reflection.test.helper.User;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

public class MetaObjectTest {
	public static final ObjectFactory OBJECT_FACTORY = new DefaultObjectFactory();
	public static final ObjectWrapperFactory OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	public static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
	
	public static void main(String[] args) {
		testBeanWrapper();
	}
	
	public static void testBeanWrapper() {
		Object object = initJavaBeanObject();
		MetaObject metaObject = MetaObject.forObject(object, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
		// 1.测试findProperty()
		System.out.println("【1. public String findProperty(String propName, boolean useCamelCaseMapping)】");
		String propertyResult = metaObject.findProperty("id", false);
		System.out.println("测试查询简单属性: " + propertyResult);
		String propertyResult2 = metaObject.findProperty("tele.country", false);
		System.out.println("测试查询对象属性的属性: " + propertyResult2);
		String propertyResult3 = metaObject.findProperty("orders[1].id", false);   // 有bug
		System.out.println("测试查询集合属性元素的属性: " + propertyResult3);		
	}
	
	public static void testMapWrapper() {
		
	}
	
	public static void testCollectionWrapper() {
		
	}
	
	private static Object initJavaBeanObject() {
		User user = new User();
		user.setId("1");
		user.setTele(new Tele("China", "mobile", "18814127750"));
		
		Order order1 = new Order();
		order1.setId("T20190630232530");
		order1.addItem(new Item(1, "meta P20"));
		Order order2 = new Order();
		order2.setId("T20190630232559");
		order2.addItem(new Item(2, "mi max3"));		
		
		user.addOrder(order1);
		user.addOrder(order2);
			
		return user;
	}
}
