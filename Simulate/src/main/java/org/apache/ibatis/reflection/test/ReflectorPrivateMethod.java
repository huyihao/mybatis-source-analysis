package org.apache.ibatis.reflection.test;

import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.exceptions.ReflectionException;
import org.apache.ibatis.reflection.invoker.MethodInvoker;

// 本类用来辅助测试Reflector中的一些私有方法，简单拷贝代码并修改可见性修饰符为public
public class ReflectorPrivateMethod {
	public static String getSignature(Method method) {
		StringBuilder sb = new StringBuilder();
		Class<?> returnType = method.getReturnType();
		if (returnType != null) {
			sb.append(returnType.getName()).append("#");
		}
		sb.append(method.getName());
		Class<?>[] parameters = method.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (i == 0) {
				sb.append(":");
			} else {
				sb.append(",");
			}
			sb.append(parameters[i].getName());
		}
		return sb.toString();
	}
	
	public static boolean canAccessPrivateMethods() {
		try {
			SecurityManager securityManager = System.getSecurityManager();  // 获取系统安全管理器
			// 检查安全管理器是否允许某种权限，如果不允许会抛出一个SecurityException
			if (null != securityManager) {
				securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
			}
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}
	
	public static void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
		for (Method method : methods) {
			if (!method.isBridge()) {
				String signature = getSignature(method);
				if (!uniqueMethods.containsKey(signature)) {
					if (canAccessPrivateMethods()) {
						try {
							method.setAccessible(true);	
						} catch (Exception e) {
							// 忽略，预防抛出异常
						}
					}
					uniqueMethods.put(signature, method);
				}
			}
		}
	}
	
	public static Method[] getClassMethods(Class<?> clazz) {
		Map<String, Method> uniqueMethods = new HashMap<String, Method>();    // 用于记录指定类中定义的全部方法的唯一签名以及对应的Method对象
		Class<?> currentClass = clazz;
		while(currentClass != null && currentClass != Object.class) {
			addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());  // 获取currentClass这个类中定义的全部方法
			                                                                     // PS.只能获取类内定义的普通方法，对于继承、重载、重写的方法无法通过反射获取
			Class<?>[] interfaces = currentClass.getInterfaces();  // 寻找接口方法，因为类可能是抽象的，可能实现了接口方法，这些方法无法通过上面的反射获取
			for (Class<?> anInterface : interfaces) {
				addUniqueMethods(uniqueMethods, anInterface.getDeclaredMethods());
			}
			currentClass = currentClass.getSuperclass();   // 一直往继承链上获取祖先类的方法签名及其映射，因为继承父类的方法不管是否重载都无法通过反射获取
		}
		Collection<Method> methods = uniqueMethods.values();
		return methods.toArray(new Method[methods.size()]);
	}
	
	public static void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
		List<Method> list = conflictingMethods.get(name);
		if (list == null) {
			list = new ArrayList<Method>();
			conflictingMethods.put(name, list);
		}
		// 因为list有可能为空，在本方法内被创建，兼容不为空的情况，把add语句放在后面
		list.add(method);
	}
	
	public static void resolveGetterConflict(Map<String, List<Method>> conflictingGetters) {
		for (String propName : conflictingGetters.keySet()) {
			List<Method> methods = conflictingGetters.get(propName);
			Iterator<Method> iterator = methods.iterator();
			Method firstMethod = iterator.next();
			System.out.println("propName = " + propName);
			if (methods.size() == 1) {
				System.out.println("No coflict, Call addGetMethod()");
			} else {
				System.out.println("There exists conflict, methodLists :");
				for (int i = 0; i < methods.size(); i++) {
					if (i == 0) {
						System.out.println("[" + methods.get(i) + ",");
					} else if(i < methods.size() - 1) {
						System.out.println(" " + methods.get(i) + ",");	
					} else {
						System.out.println(" " + methods.get(i) + "]");
					}
				}
				Method getter = firstMethod;
				Class<?> getterType = getter.getReturnType();   // 最终会
				while (iterator.hasNext()) {
					Method method = iterator.next();
					Class<?> methodType = method.getReturnType();
					if (getterType.equals(methodType)) {
						// 返回值相同，实际上没有冲突，证明方法签名生成那个可能有问题了，抛出异常
						throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
				                + propName + " in class " + firstMethod.getDeclaringClass()
				                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
					} else if (methodType.isAssignableFrom(getterType)) {
						// 加入判断结果为true，表示methodType是getterType的继承父类或实现的接口
						// 当前最合适的方法(getter)的返回值是当前方法(method)返回值的子类，什么都不做，当前最适合的方法依然不变
					} else if (getterType.isAssignableFrom(methodType)) {
						// getter是method的父类，明显method辈分更小，更适合
						getter = method;
						getterType = methodType;
					} else {
						// 返回值相同，二义性，抛出异常
						throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
				                + propName + " in class " + firstMethod.getDeclaringClass()
				                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
					}
				}
				System.out.println("Resolve conflict, Call addGetMethod(), choose method:\n" + getter);
			}
			System.out.println();
		}
	}
}
