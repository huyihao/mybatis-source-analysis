package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.exceptions.ReflectionException;
import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * Reflector类缓存了反射操作需要使用的类的元信息，使得属性名和getter/setter之间的映射更加简单
 * 
 * 提供下以下功能:
 * (1) 类中可读属性集合
 * (2) 类中可写属性集合
 * (3) 判断类中是否存在某可读属性
 * (4) 判断类中是否存在某可写属性
 * (5) 判断类中是否存在某属性
 * (5) 获取类中某可读属性的类型
 * (6) 获取类中某可写属性的类型
 * (7) 通过反射获取类的对象实例某属性值
 * (8) 通过反射设置类的对象实例某属性值
 * 
 */
public class Reflector {
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private Class<?> type;    // 对应的Class类型
	private String[] readablePropertyNames = EMPTY_STRING_ARRAY;  // 可读属性的名称集合，可读属性就是存在相应getter方法的属性，初始值为空数组
	private String[] writeablePropertyNames = EMPTY_STRING_ARRAY;  // 可写属性的名称集合，可写属性就是存在相应setter方法的属性，初始值为空数组
	private Map<String, Invoker> setMethods = new HashMap<String, Invoker>();  // 记录属性相应的setter方法，key是属性名称，value是Invoker对象，它是对setter方法对应Method对象的封装
	private Map<String, Invoker> getMethods = new HashMap<String, Invoker>();  // 记录属性相应的getter方法，key是属性名称，value是Invoker对象，它是对getter方法对应Method对象的封装
	private Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();  // 记录了属性相应的setter方法的参数值类型，key是属性名称，value是setter方法的参数类型
	private Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();  // 记录了属性相应的getter方法的参数值类型，key是属性名称，value是getter方法的返回值类型
	private Constructor<?> defaultConstructor;  // 记录了默认构造方法
	
	private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();  // 记录了所有属性的名称集合
	
	public Reflector(Class<?> clazz) {
		this.type = clazz;
		addDefaultConstouctor(clazz);
		addGetMethods(clazz);   // 处理clazz中的getter方法，填充getMethods集合和getTypes集合
		addSetMethods(clazz);   // 处理clazz中的setter方法，填充setMethods集合和setTypes集合
		addFields(clazz);       // 处理没有getter/setter方法的字段
		// 根据getMethods/setMethods集合，初始化可读/写属性的名称集合
		readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
		writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
		// 在resolveSetterConflict()方法中可以看到，如果一个属性只有getter而没有setter是会抛反射异常的
		// 所以一个正常的属性在readablePropertyNames和writeablePropertyNames中都肯定有值
		// 但是有些特殊的属性比如常量，只有可读权限没有修改权限，因此只存在于readablePropertyNames集合，所以两个集合的并集才是完整的所有属性的集合
		for (String propName : readablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
		for (String propName : writeablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
	}
	
	// 通过反射获取类的默认构造方法
	private void addDefaultConstouctor(Class<?> clazz) {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			// 默认构造方法不含参
			if (constructor.getParameterTypes().length == 0) {
				// 判断系统安全管理器是否允许访问不可见方法
				if (canAccessPrivateMethods()) {
					try {
						constructor.setAccessible(true);  // 将构造器设置为可访问(覆盖，因为可能本来就可访问了)
					} catch (Exception e) {
						// 忽略，这只是最后的预防措施，我们无能为力
					}
				}
				if (constructor.isAccessible()) {
					this.defaultConstructor = constructor;
				}
			}
		}
	}
	
	// 获取类中所有getter方法
	private void addGetMethods(Class<?> clazz) {
		// conflictingGetters集合的key为属性名称，value是getter方法集合，因为子类可能覆盖父类的getter方法
		// 所以同一属性名称，可能会存在多个getter方法
		Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
		
		// 获取指定类及其父类和接口中定义的方法
		Method[] methods = getClassMethods(clazz);
		
		// 按照JavaBean规范查找getter方法，并记录到conflictingGetters集合中
		for (Method method : methods) {
			String name = method.getName();    // 方法名
			if (name.startsWith("get") && name.length() > 3) {
				if (method.getParameterTypes().length == 0) {
					name = PropertyNamer.methodToProperty(name);   // 将方法名转化为属性名
					addMethodConflict(conflictingGetters, name, method);
				}
			} else if (name.startsWith("is") && name.length() > 2) {
				if (method.getParameterTypes().length == 0) {
					name = PropertyNamer.methodToProperty(name);   // 将方法名转化为属性名
					addMethodConflict(conflictingGetters, name, method);
				}
			}
			
			// 简洁写法
			/*if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
				if (method.getParameterTypes().length == 0) {
					name = PropertyNamer.methodToProperty(name);   // 将方法名转化为属性名
					addMethodConflict(conflictingGetters, name, method);
				}
			}*/
		}
		resolveGetterConflict(conflictingGetters);
	}
	
	/**
	 * 当子类覆盖了父类的getter方法且返回值发生变化时，在getClassMethod()中就会产生两个签名不同的方法
	 * eg:
	 * 	     父类定义了方法: public List<String> getProp() { ... }
	 *    子类重载了方法: public ArrayList<String> getProp() { ... }
	 */
	private void resolveGetterConflict(Map<String, List<Method>> conflictingGetters) {
		for (String propName : conflictingGetters.keySet()) {
			List<Method> methods = conflictingGetters.get(propName);
			Iterator<Method> iterator = methods.iterator();
			Method firstMethod = iterator.next();
			if (methods.size() == 1) {
				addGetMethod(propName, firstMethod);
			} else {
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
				addGetMethod(propName, getter);
			}
		}
	}
	
	private void addGetMethod(String name, Method method) {
		// 检测属性名是否合法
		if (isValidPropertyName(name)) {
			// 将属性名以及对应的MethodInvoker对象添加到getMethods集合中
			getMethods.put(name, new MethodInvoker(method));
			// 获取返回值类型的真实类型(因为有可能带泛型)
			Type returnType = TypeParameterResolver.resolveReturnType(method, type);
			// 将属性名称及其getter方法的返回值类型添加到getTypes集合中保存
			getTypes.put(name, typeToClass(returnType));
		}
	}
	
	// 获取类中所有setter方法
	private void addSetMethods(Class<?> clazz) {
		Map<String, List<Method>> conflictingSetters = new HashMap<String, List<Method>>();
		Method[] methods = getClassMethods(clazz);
		for (Method method : methods) {
			String name = method.getName();    // 方法名
			if (name.startsWith("set") && name.length() > 3) {
				name = PropertyNamer.methodToProperty(name);
				addMethodConflict(conflictingSetters, name, method);
			}
		}
		resolveSetterConflict(conflictingSetters);
	}
	
	private void resolveSetterConflict(Map<String, List<Method>> conflictingSetters) {
		for (String propName : conflictingSetters.keySet()) {
			List<Method> setters = conflictingSetters.get(propName);
			Method firstMethod = setters.get(0);
			if (setters.size() == 1) {
				addSetMethod(propName, firstMethod);
			} else {
				// 一个属性有getter就应该有setter，如果在setter里有，但是getter里没有，不符合JavaBean的规范，抛出异常
				Class<?> expectedType = getTypes.get(propName);
				if (expectedType ==  null) {
					throw new ReflectionException("Illegal overloaded setter method with ambiguous type for property "
							+ propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
							"specification and can cause unpredicatble results.");
				} else {
					Iterator<Method> iterator = setters.iterator();
					Method setter = null;
					while (iterator.hasNext()) {
						Method method = iterator.next();
						if (method.getParameterTypes().length == 1 &&
							expectedType.equals(method.getParameterTypes()[0])) {
							setter = method;
							break;
						}
					}
					// 如果没有一个setter方法的参数类型跟getter的返回参数类型一致，说明解析的类不符合JavaBean的getter/setter规范
					if (setter == null) {
						throw new ReflectionException("Illegal overloaded setter method with ambiguous type for property "
				                + propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
				                "specification and can cause unpredicatble results.");
					}
					addSetMethod(propName, setter);
				}
			}
		}
	}
	
	private void addSetMethod(String name, Method method) {
		if (isValidPropertyName(name)) {
			setMethods.put(name, new MethodInvoker(method));
			// 因为通过反射获取setter方法参数类型时已经过滤了只取一个参数的方法，所以这里获得的数组长度肯定为1
			Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
			setTypes.put(name, typeToClass(paramTypes[0]));
		}
	}
	
	private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
		List<Method> list = conflictingMethods.get(name);
		if (list == null) {
			list = new ArrayList<Method>();
			conflictingMethods.put(name, list);
		}
		// 因为list有可能为空，在本方法内被创建，兼容不为空的情况，把add语句放在后面
		list.add(method);
	}
	
	// 获取类中所有属性
	private void addFields(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (canAccessPrivateMethods()) {
				try {
					field.setAccessible(true);
				} catch (Exception e) {}
			}
			if (field.isAccessible()) {
				// 当setMethods集合不包含同名属性时，将其记录到setMethods集合(此时用的是反射设置属性的值，跟通过反射调用setter方法设置属性值有所不同)和setTypes集合
				if (!setMethods.containsKey(field.getName())) {
					int modifiers = field.getModifiers();
					// 如果属性单独被修饰为final或static，则在类初始化时还可以设置属性值
					// 但是如果同时被static final修饰，证明这个属性应该是个常量，此时不应提供通过反射设置属性值的Invoker
					if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
						addSetField(field);
					}
				}
				if (!getMethods.containsKey(field.getName())) {
					addGetField(field);
				}
			}
		}
		if (clazz.getSuperclass() != null) {   // 沿着继承链往上追溯  
			addFields(clazz.getSuperclass());
		}
	}
	
	private void addSetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			setMethods.put(field.getName(), new SetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			setTypes.put(field.getName(), typeToClass(fieldType));
		}
	}
	
	private void addGetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			getMethods.put(field.getName(), new GetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			getTypes.put(field.getName(), typeToClass(fieldType));
		}
	}
 	
	private Class<?> typeToClass(Type src) {
		Class<?> result = null;
		if (src instanceof Class) {
			result = (Class<?>) src;
		} else if (src instanceof ParameterizedType) {
			result = (Class<?>) ((ParameterizedType) src).getRawType();
		} else if (src instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) src).getGenericComponentType();
			if (componentType instanceof Class) {
				result = Array.newInstance((Class<?>) componentType, 0).getClass();
			} else {
				Class<?> componentClass = typeToClass(componentType);
				result = Array.newInstance((Class<?>) componentClass, 0).getClass();
			}
		}
		if (result == null) {
			result = Object.class;
		}
		return result;
	}
	
	private boolean isValidPropertyName(String name) {
		return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
	}
	
	// 通过反射获取类元信息相关方法
	// [start]
	// 返回类中的方法数组（包括继承链上祖先类的方法和实现的接口的方法，过滤桥接方法、安全管理器不允许访问的方法）
	private Method[] getClassMethods(Class<?> clazz) {
		Map<String, Method> uniqueMethods = new HashMap<String, Method>();    // 用于记录指定类中定义的全部方法的唯一签名以及对应的Method对象
		Class<?> currentClass = clazz;
		while(currentClass != null) {
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
	
	// 将方法签名和方法的映射关系放入一个Map中
	// 这里作用实际上主要是过滤掉设置了安全管理器无法访问的方法，还有编译器生成的桥接方法
	private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
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
	
	/**
	 * 获取方法签名，唯一标识每个方法
	 * 格式: [方法返回值类型的全限定名]#[方法名]:[参数1类型的全限定名],[参数2类型的全限定名]...
	 * 
	 * 以Reflector的getSignature方法为例，返回：
	 * 		java.lang.String#getSignature:java.lang.reflect.Method
	 * 以getClassMethods方法为例，返回（前面的[L表示是一个数组）：
	 * 		[Ljava.lang.reflect.Method;#getClassMethods:java.lang.Class
	 */
	private String getSignature(Method method) {
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
	// [end]
	
	/**
	 * 是否可以访问类的私有方法，根据Java系统安全管理器检查反射调用非public方法权限判断
	 * [From JDK]
	 * "ReflectPermission 是一种指定权限，没有动作。当前定义的唯一名称是 suppressAccessChecks，
	 *  它允许取消由反射对象在其使用点上执行的标准 Java 语言访问检查 - 对于 public、default（包）访问、protected、private 成员。 "
	 * [如何启动安全管理器?]
	 *     运行程序时加上JVM启动参数-Djava.security.manager
	 * [启动安全管理器后反射权限不被允许，如何添加权限?]
	 *     在${JAVA_HOME}目录下的jre\lib\security子目录中的java.policy中的grant{}添加
	 *     permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
	 */
	private static boolean canAccessPrivateMethods() {
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

	// 对外暴露的方法
	public Class<?> getType() {
		return type;
	}

	public Constructor<?> getDefaultConstructor() {
		if (defaultConstructor != null) {
			return defaultConstructor;
		} else {
			throw new ReflectionException("There is no default constructor for " + type);
		}
	}
	
	public boolean hasDefaultConstructor() {
		return defaultConstructor != null;
	}
	
	public Invoker getSetInvoker(String propertyName) {
		Invoker method = setMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}		
		return method;
	} 
	
	public Invoker getGetInvoker(String propertyName) {
		Invoker method = getMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}	
	
	public Boolean hasSetter(String propertyName) {
		return setMethods.keySet().contains(propertyName);
	}
	
	public Boolean hasGetter(String propertyName) {
		return getMethods.keySet().contains(propertyName);
	}	
	
	public Class<?> getSetterType(String propertyName) {
		Class<?> clazz = setTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}
	
	public Class<?> getGetterType(String propertyName) {
		Class<?> clazz = getTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}
	
	public String[] getGetablePropertyNames() {
		return readablePropertyNames;
	}
	
	public String[] getSetablePropertyNames() {
		return writeablePropertyNames;
	}
	
	public String findPropertyName(String name) {
		return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
	}
}