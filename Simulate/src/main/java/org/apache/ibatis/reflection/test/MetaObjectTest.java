package org.apache.ibatis.reflection.test;

import java.util.HashMap;
import java.util.Map;

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
		// testBeanWrapper();
		testMapWrapper();
	}
	
	public static void testBeanWrapper() {
		Object object = initJavaBeanObject();
		MetaObject metaObject = MetaObject.forObject(object, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
		
		// 测试MetaObject的各个方法
		// 1.测试findProperty()
		System.out.println("【1. public String findProperty(String propName, boolean useCamelCaseMapping)】");
		String propertyResult = metaObject.findProperty("id", false);
		System.out.println("测试查询简单属性: " + propertyResult);
		String propertyResult2 = metaObject.findProperty("tele.country", false);
		System.out.println("测试查询对象属性的属性: " + propertyResult2);
		String propertyResult3 = metaObject.findProperty("orders[1].id", false);   // 有bug
		System.out.println("测试查询集合属性元素的属性: " + propertyResult3);
		System.out.println();
		
		// 2.测试getGetterNames()
		System.out.println("【2. public String[] getGetterNames()】");
		String[] readablePropertyNames = metaObject.getGetterNames();
		for (int i = 0; i < readablePropertyNames.length; i++) {
			System.out.println("readablePropertyNames[" + i + "] = " + readablePropertyNames[i]);
		}
		System.out.println();
		
		// 3. 测试getSetterNames()
		System.out.println("【3. public String[] getSetterNames()】");
		String[] writeablePropertyNames = metaObject.getSetterNames();
		for (int i = 0; i < writeablePropertyNames.length; i++) {
			System.out.println("writeablePropertyNames[" + i + "] = " + writeablePropertyNames[i]);
		}
		System.out.println();
		
		// 4. 测试getSetterType()
		System.out.println("【4. public Class<?> getSetterType(String name)】");
		Class<?> setterType1 = metaObject.getSetterType("tele.type");
		System.out.println("setterType of 'tele.type': " + setterType1);
		Class<?> setterType2 = metaObject.getSetterType("orders[0].items");
		System.out.println("setterType of 'orders[0].items': " + setterType2);
		Class<?> setterType3 = metaObject.getSetterType("orders[0].items[0]");
		System.out.println("setterType of 'orders[0].items[0]': " + setterType3);
		Class<?> setterType4 = metaObject.getSetterType("orders[0].items[0].id");
		System.out.println("setterType of 'orders[0].items[0].id': " + setterType4);
		System.out.println();
		
		// 5. 测试getGetterType
		System.out.println("【5. public Class<?> getGetterType(String name)】");
		Class<?> getterType1 = metaObject.getGetterType("tele.type");
		System.out.println("getterType of 'orders': " + getterType1);
		Class<?> getterType2 = metaObject.getGetterType("orders[0].items");
		System.out.println("getterType of 'orders[0].items': " + getterType2);
		Class<?> getterType3 = metaObject.getGetterType("orders[0].items[0]");    // 这里注意跟getSetterType的区别，getGetterType认为是要获取集合元素值的类型，getSetterType认为是要获取items的类型，我觉得这里不统一很奇怪
		System.out.println("getterType of 'orders[0].items[0]': " + getterType3);
		Class<?> getterType4 = metaObject.getGetterType("orders[0].items[0].id");
		System.out.println("getterType of 'orders[0].items[0].id': " + getterType4);
		System.out.println();
		
		// 6. 测试hasSetter
		System.out.println("【6. public boolean hasSetter(String name)】");
		System.out.println("hasSetter of 'orders'? " + metaObject.hasSetter("orders"));
		System.out.println("hasSetter of 'username'? " + metaObject.hasSetter("username"));
		System.out.println();
		
		// 7. 测试hasGetter
		System.out.println("【7. public boolean hasGetter(String name)】");
		System.out.println("hasGetter of 'tele.num'? " + metaObject.hasGetter("tele.num"));
		System.out.println("hasGetter of 'orders[0].items[1]'? " + metaObject.hasSetter("orders[0].items[1]"));   // 没有保持一致啊Clinton Begin大哥，这里items[1]数组越界为啥不抛出异常
		// System.out.println("hasGetter of 'orders[2].items[0]'? " + metaObject.hasSetter("orders[2].items[0]"));   // 数组越界，抛出异常
		System.out.println("hasGetter of 'phone.country'? " + metaObject.hasSetter("phone.country"));
		System.out.println();
		
		// 8. 测试getValue
		System.out.println("【8. public Object getValue(String name)】");
			// 直接获取属性值
		System.out.println("value of 'id': " + metaObject.getValue("id"));
		System.out.println("value of 'tele': " + metaObject.getValue("tele"));
		System.out.println("value of 'phone': " + metaObject.getValue("phone"));   // 由于没初始化，这里会打印null
		System.out.println("value of 'orders': " + metaObject.getValue("orders"));
			// 获取对象属性的子属性值
		System.out.println("value of 'tele.country': " + metaObject.getValue("tele.country"));
			// 获取集合属性的子属性
		System.out.println("value of 'orders[0].id': " + metaObject.getValue("orders[0].id"));
		// System.out.println("value of 'orders[0].items[1]': " + metaObject.getValue("orders[0].items[1]"));   // 数组越界，抛异常
		System.out.println();
		
		// 9. 测试setValue
		System.out.println("【9. public void setValue(String name, Object value)】");
		metaObject.setValue("id", "2");
		System.out.println(metaObject.getValue("id"));   // 看看是否设置成功
		metaObject.setValue("tele", new Tele("America", "phone", "18998294750"));
		System.out.println(metaObject.getValue("tele"));
		//metaObject.setValue("orders[1].items[1]", new Item(3, "iphone 8"));  // items[1]数组越界，抛异常
		//System.out.println(metaObject.getValue("orders[1].items[1]"));
		metaObject.setValue("orders[0].items[0]", null);
		System.out.println(metaObject.getValue("orders[0].items[0]"));
		//metaObject.setValue("orders[0].items[0].id", 3);                     // 由于items[0]已经被设为null，这时无法对其属性进行设值，会抛出空指针异常
		//metaObject.setValue("orders[0].items[0].name", "iphone 8");
		metaObject.setValue("orders[0].items[0]", new Item(3, "iphone 8"));    // 但允许对值为null的对象元素设值
		System.out.println(metaObject.getValue("orders[0].items[0]"));
		System.out.println();
		
		// 10. 测试isCollection
		System.out.println("【10. public boolean isCollection()】");
		System.out.println("object isCollection()? " + metaObject.isCollection());
		System.out.println();
		
		// 11. 测试add和addAll(属于集合才能操作的方法，简单Java对象的metaClass调用其方法会抛出异常)
		System.out.println("【11. public void add(Object element) || public <E> void addAll(List<E> list)】");
		try {
			metaObject.add(null);			
		} catch (UnsupportedOperationException e) {
			System.out.println("BeanWrapper does'n suppert this operarion!");			
		}
	}
	
	public static void testMapWrapper() {
		Object object = initMapObject();
		MetaObject metaObject = MetaObject.forObject(object, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
		
		// 测试MapWrapper的方法
		// 1. 测试get
		System.out.println("【1. public Object get(PropertyTokenizer prop)】");
		System.out.println("value of 'item1': " + metaObject.getValue("item1"));
		System.out.println("value of 'item2.name': " + metaObject.getValue("item2.name"));
		System.out.println("value of 'map[mapkey1]': " + metaObject.getValue("map[mapkey1]"));  // MapWrapper解析的特点是key放在中括号里解析，当然第一级key不需要
		System.out.println();
		
		// 2. 测试set
		System.out.println("【2. public void set(PropertyTokenizer prop, Object value)】");
		metaObject.setValue("item2", "updated name2");
		System.out.println("value of 'item2': " + metaObject.getValue("item2"));
		metaObject.setValue("map[mapkey2]", "updated mapValue2");
		System.out.println("value of 'map[mapkey2]': " + metaObject.getValue("map[mapkey2]"));
		System.out.println();
		
		// 3. 测试getGetterNames
		System.out.println("【3. public String[] getGetterNames()】");
		String[] names = metaObject.getGetterNames();
		for (int i = 0; i < names.length; i++) {
			System.out.println("GetterNames[" + i + "] = " + names[i]);
		}
		System.out.println();
		
		// 4. 测试getSetterNames
		System.out.println("【4. public String[] getSetterNames()】");
		String[] names2 = metaObject.getSetterNames();
		for (int i = 0; i < names2.length; i++) {
			System.out.println("SetterNames[" + i + "] = " + names2[i]);
		}
		System.out.println();
		
		// 对Map对象中嵌套的Map类型的属性的子属性解析没有采用上面的"map[mapkey1]"的形式，采用"map.mapkey1"的形式，想不通，为什么不统一属性表达式的解析规则？
		// 5. 测试getSetterType
		System.out.println("【5. public Class<?> getSetterType(String name)】");
		System.out.println("setterType of 'item3': " + metaObject.getSetterType("item3"));
		System.out.println("setterType of 'item1.id': " + metaObject.getSetterType("item1.id"));
		System.out.println("setterType of 'map.mapkey1': " + metaObject.getSetterType("map.mapkey1"));     // 这里又变成了map.key的形式
		// 有bug，实际上是null，走到了if (map.get(name) == null) 的分支，对属性表达式的解析规则跟getValue和setValue不统一
		System.out.println("setterType of 'map[mapkey1]': " + metaObject.getSetterType("map[mapkey1]"));   
		System.out.println();
		
		// 6. 测试getGetterType
		System.out.println("【6. public Class<?> getGetterType(String name)】");
		System.out.println("getterType of 'item3': " + metaObject.getGetterType("item3"));
		System.out.println("getterType of 'item1.id': " + metaObject.getGetterType("item1.id"));
		System.out.println("getterType of 'map.mapkey1': " + metaObject.getGetterType("map.mapkey1"));   
		System.out.println("getterType of 'map[mapkey1]': " + metaObject.getGetterType("map[mapkey1]"));   //
		System.out.println();
		
		// 7. hasSetter固定返回true，因为Map允许插入新的键值对
		
		// 8. 测试hasGetter
		System.out.println("【8. public boolean hasGetter(String name)】");
		System.out.println("hasGetter of 'item3': " + metaObject.hasGetter("item3"));
		System.out.println("hasGetter of 'item1.id': " + metaObject.hasGetter("item1.id"));
		System.out.println("hasGetter of 'map.mapkey1': " + metaObject.hasGetter("map.mapkey1"));
		System.out.println("hasGetter of 'map[mapkey1]': " + metaObject.hasGetter("map[mapkey1]"));  // 因为解析规则变了，这里实际上解析效果等同于"map"
		System.out.println();
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
	
	private static Object initMapObject() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("item1", new Item(1, "name1"));
		map.put("item2", new Item(2, "name2"));
		map.put("item3", new Item(3, "name3"));
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("mapkey1", "mapValue1");
		map2.put("mapkey2", "mapValue2");
		map.put("map", map2);
		return map;
	}
}
