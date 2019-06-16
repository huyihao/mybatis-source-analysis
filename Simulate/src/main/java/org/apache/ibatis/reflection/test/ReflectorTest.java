package org.apache.ibatis.reflection.test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.property.PropertyNamer;
import org.apache.ibatis.reflection.test.helper.ReflectorDemoClass;
import org.apache.ibatis.reflection.test.helper.ReflectorDemoClassWithoutDefaultConstructor;

public class ReflectorTest {
	public static void main(String[] args) {
		// 1. 测试默认构造器
		System.out.println("【addDefaultConstouctor】");
		addDefaultConstouctorTest();
		System.out.println();
		
		// 2. 公用工具方法
		// [start]
		// 2.1 获取方法签名
		System.out.println("【getSignatureTest】");
		getSignatureTest();
		System.out.println();
		
		// 2.2 安全管理器检查权限
		System.out.println("【canAccessPrivateMethodsTest】");
		canAccessPrivateMethodsTest();
		System.out.println();
		// [end]
		
		// 3. addGetMethods()的分解测试
		// [start]
		// 3.1 获取类的定义的方法(包括实现接口方法和父类的方法，通过方法签名在父类和子类完全一样的方法头方法中选择子类)
		System.out.println("【addUniqueMethodsTest】");
		addUniqueMethodsTest();
		System.out.println();
		
		// 3.2 获取类的所有方法(包括实现接口方法和父类的方法，通过方法签名在父类和子类完全一样的方法头方法中选择子类)
		System.out.println("【getClassMethodsTest】");
		getClassMethodsTest();
		System.out.println();
		
		// 3.3 测试对类反射解析获取到的方法进行过滤得到getter方法
		System.out.println("【addMethodConflictTest】");
		Map<String, List<Method>> conflictingGetters = addMethodConflictTest();
		System.out.println();
		
		// 3.4 测试解决同一属性名有多个getter方法冲突选择的方法
		System.out.println("【resolveGetterConflictTest】");
		resolveGetterConflictTest(conflictingGetters);
		System.out.println();
		// [end]
		
		// 对addSetMethods()的分解测试，跟对addGetMethods的分解测试大同小异
		// 唯一有点不同的是resolveSetterConflict()方法，它会判断同一个属性的getter的返回值跟setter的的参数值类型是否一致，不一致则跳过，最终在setter方法列表里找不到一致的则抛出异常
	}
	
	public static void addDefaultConstouctorTest() {
		Reflector reflector = new Reflector(ReflectorDemoClass.class);
		System.out.println(reflector.getDefaultConstructor());
		Reflector reflector2 = new Reflector(ReflectorDemoClassWithoutDefaultConstructor.class);
		try {
			System.out.println(reflector2.getDefaultConstructor());
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	// 测试方法签名生成方法
	public static void getSignatureTest() {
		Method[] methods = ReflectorDemoClass.class.getDeclaredMethods();
		for (Method method : methods) {
			String signature = ReflectorPrivateMethod.getSignature(method);
			System.out.println(signature);
		}
	}
	
	// 默认安全管理器不开启，允许所有权限操作
	public static void canAccessPrivateMethodsTest() {
		boolean permit = ReflectorPrivateMethod.canAccessPrivateMethods();
		if (permit) {
			System.out.println("安全管理器允许通过反射获取类的非public属性、方法");
		} else {
			System.out.println("安全管理器不允许通过反射获取类的非public属性、方法");
		}
	}
	
	// 测试获取类中定义的方法签名和方法的映射关系
	public static void addUniqueMethodsTest() {
		Map<String, Method> uniqueMethods = new HashMap<String, Method>();
		Method[] methods = ReflectorDemoClass.class.getDeclaredMethods();
		System.out.println("打印类中定义的方法:");
		for (Method method : methods) {
			System.out.println(method);
		}
		System.out.println();
		
		System.out.println("打印经过桥接方法过滤之后的方法签名和方法的映射:");
		ReflectorPrivateMethod.addUniqueMethods(uniqueMethods, methods);
		for (String sginature : uniqueMethods.keySet()) {
			System.out.print("key: " + sginature + "\t");
			System.out.println("value: " + uniqueMethods.get(sginature));
		}
		System.out.println();
		
		System.out.println("打印编译器生成的桥接方法:");
		Collection<Method> methodCollection = uniqueMethods.values();
		for (Method method : methods) {
			if (!methodCollection.contains(method)) {
				System.out.println("桥接方法: " + method);
			}
		}
	}
	
	// 测试通过反射获取类的所有方法，包括:
	// (1) 父类的方法
	// (2) 实现接口的方法
	// 沿着继承链将Object的方法都反射解析了一遍，感觉不是很有必要
	public static void getClassMethodsTest() {
		Method[] methods = ReflectorPrivateMethod.getClassMethods(ReflectorDemoClass.class);
		for (Method method : methods) {
			System.out.println(method);
		}
	}
	
	// 测试对类反射解析获取到的方法进行过滤得到getter方法
	// 在addUniqueMethods()方法中过滤掉了桥接方法，但是编译后擦除泛型的接口方法的方法头跟实现类中的桥接方法一样，
	// 所以在getClassMethods()的返回的Method[]数组中还是会存在一个方法签名跟桥接方法一样的方法
	// 运行结果: conflictingGetters
	//	{
	//      str=[public java.lang.String org.apache.ibatis.reflection.test.helper.ReflectorDemoClass.getStr()],
	//	    doubleArr=[public java.lang.Double[] org.apache.ibatis.reflection.test.helper.ReflectorDemoClass.getDoubleArr()],
	//	    num=[public int org.apache.ibatis.reflection.test.helper.ReflectorDemoClass.getNum()],
	//		list=[public java.util.ArrayList org.apache.ibatis.reflection.test.helper.ReflectorDemoClass.getList(),
	//			  public java.util.List org.apache.ibatis.reflection.test.helper.ReflectorDemoSupclass.getList()]
	//  }
	public static Map<String, List<Method>> addMethodConflictTest() {
		Class<?> clazz = ReflectorDemoClass.class;
		Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
		Method[] methods = ReflectorPrivateMethod.getClassMethods(clazz);
		for (Method method : methods) {
			String name = method.getName();
			if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
				if (method.getParameterTypes().length == 0) {
					name = PropertyNamer.methodToProperty(name);   // 将方法名转化为属性名
					ReflectorPrivateMethod.addMethodConflict(conflictingGetters, name, method);
				}
			}
		}
		for (String propName : conflictingGetters.keySet()) {
			System.out.println("propName = " + propName);
			List<Method> methodlist = conflictingGetters.get(propName);
			System.out.print("methodList = ");
			for (int i = 0; i < methodlist.size(); i++) {
				if (i != 0) {
					System.out.println("             " + methodlist.get(i));
				} else {
					System.out.println(methodlist.get(i));
				}
			}
			System.out.println();
		}
		return conflictingGetters;
	}
	
	public static void resolveGetterConflictTest(Map<String, List<Method>> conflictingGetters) {
		ReflectorPrivateMethod.resolveGetterConflict(conflictingGetters);
	}
	
	public static void resolveSetterConflictTest() {
		
	}
}
