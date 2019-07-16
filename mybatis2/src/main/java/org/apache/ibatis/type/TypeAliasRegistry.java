package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * 类别名注册器
 */
public class TypeAliasRegistry {
	// 存储类别名和类的Class对象的对照关系
	private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();
	
	public TypeAliasRegistry() {
	    registerAlias("string", String.class);

	    registerAlias("byte", Byte.class);
	    registerAlias("long", Long.class);
	    registerAlias("short", Short.class);
	    registerAlias("int", Integer.class);
	    registerAlias("integer", Integer.class);
	    registerAlias("double", Double.class);
	    registerAlias("float", Float.class);
	    registerAlias("boolean", Boolean.class);

	    registerAlias("byte[]", Byte[].class);
	    registerAlias("long[]", Long[].class);
	    registerAlias("short[]", Short[].class);
	    registerAlias("int[]", Integer[].class);
	    registerAlias("integer[]", Integer[].class);
	    registerAlias("double[]", Double[].class);
	    registerAlias("float[]", Float[].class);
	    registerAlias("boolean[]", Boolean[].class);

    	// 基本类型的在前面加一个下划线，与对象类型区分开来
	    registerAlias("_byte", byte.class);
	    registerAlias("_long", long.class);
	    registerAlias("_short", short.class);
	    registerAlias("_int", int.class);
	    registerAlias("_integer", int.class);
	    registerAlias("_double", double.class);
	    registerAlias("_float", float.class);
	    registerAlias("_boolean", boolean.class);

	    registerAlias("_byte[]", byte[].class);
	    registerAlias("_long[]", long[].class);
	    registerAlias("_short[]", short[].class);
	    registerAlias("_int[]", int[].class);
	    registerAlias("_integer[]", int[].class);
	    registerAlias("_double[]", double[].class);
	    registerAlias("_float[]", float[].class);
	    registerAlias("_boolean[]", boolean[].class);

	    registerAlias("date", Date.class);
	    registerAlias("decimal", BigDecimal.class);
	    registerAlias("bigdecimal", BigDecimal.class);
	    registerAlias("biginteger", BigInteger.class);
	    registerAlias("object", Object.class);

	    registerAlias("date[]", Date[].class);
	    registerAlias("decimal[]", BigDecimal[].class);
	    registerAlias("bigdecimal[]", BigDecimal[].class);
	    registerAlias("biginteger[]", BigInteger[].class);
	    registerAlias("object[]", Object[].class);

	    registerAlias("map", Map.class);
	    registerAlias("hashmap", HashMap.class);
	    registerAlias("list", List.class);
	    registerAlias("arraylist", ArrayList.class);
	    registerAlias("collection", Collection.class);
	    registerAlias("iterator", Iterator.class);

	    registerAlias("ResultSet", ResultSet.class);
	}
	
	// 根据类型别名或类的全限定名获取对应的Class对象
	@SuppressWarnings("unchecked")
	public <T> Class<T> resolveAlias(String string) {
		try {
			if (string == null) {
				return null;
			}
			
			String key = string.toLowerCase(Locale.ENGLISH);
			Class<T> value;
			if (TYPE_ALIASES.containsKey(key)) {
				value = (Class<T>) TYPE_ALIASES.get(key);
			} else {
				value = (Class<T>) Resources.classForName(string);
			}
			return value;
		} catch (ClassNotFoundException e) {
			throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
		}
	}
	
	public void registerAliases(String packageName) {
		registerAliases(packageName, Object.class);
	}
	
	public void registerAliases(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		// 查找指定包下的superType类型类
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
		for (Class<?> type : typeSet) {
			// 过滤掉内部类、接口以及抽象类
			if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
				registerAlias(type);
			}
		}
	}
	
	// 注册Class对象的别名，如果有用Alias注解则用注解值作为别名，否则使用简单类名作为别名
	public void registerAlias(Class<?> type) {
		String alias = type.getSimpleName();
		Alias aliasAnnotation = type.getAnnotation(Alias.class);
		if (aliasAnnotation != null) {
			alias = aliasAnnotation.value();
		}
		registerAlias(alias, type);
	}
	
	// 使用指定的alias别名和对应的Class对象注册别名映射关系
	public void registerAlias(String alias, Class<?> value) {
		if (alias == null) {
			throw new TypeException("The parameter alias cannot be null");
		}
		String key = alias.toLowerCase(Locale.ENGLISH);   // 统一转换为小写
		// 如果该别名已注册，注册的Class对象不为空且与value值相同，证明同一别名被注册了两次，抛出异常，否则覆盖注册
		if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
			throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
		}
		TYPE_ALIASES.put(key, value);
	}
	
	// 根据指定别名和类的全限定名注册别名映射关系
	public void registerAlias(String alias, String value) {
		try {
			registerAlias(alias, Resources.classForName(value));
		} catch (ClassNotFoundException e) {
			throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
		}
	} 

	// 获取所有类别名和Class对象的Map
	public Map<String, Class<?>> getTypeAliases() {
		return Collections.unmodifiableMap(TYPE_ALIASES);  // 返回一个不可修改的Map，防止被篡改
	}
}
