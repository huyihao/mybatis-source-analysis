package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

public class PropertyNamer {
	// 源码写法
	// (1) 对于boolean类型的属性，它们的getter一般是isXXX开头的
	// (2) 对name进行了校验，非getter、setter抛出异常 
	// (3) 只对首字母为大写的name转化为小写
	public static String methodToProperty(String name) {
		if (name.startsWith("is")) {
			name = name.substring(2);
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
		}
		
		if (name.length() == 1 || (name.length() > 1 && Character.isUpperCase(name.charAt(0)))) {
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}
		return name;
	}
	
	/* 我的写法
	public static String methodToProperty(String methodName) {
		if (methodName.startsWith("get") || methodName.startsWith("set")) {
			methodName = methodName.substring(3);
		} else {
			return null;
		}
		String property = methodName.substring(0, 1).toLowerCase(Locale.ENGLISH) + methodName.substring(1);
		return property;
	}*/
	
	// 判断方法是否为getter/setter
	public static boolean isProperty(String name) {
		return name.startsWith("is") || name.startsWith("get") || name.startsWith("set");
	}
	
	// 判断方法是否为getter
	public static boolean isGetter(String name) {
		return name.startsWith("is") || name.startsWith("get");
	}
	
	// 判断方法是否为setter
	public static boolean isSetter(String name) {
		return name.startsWith("set");
	}
}
