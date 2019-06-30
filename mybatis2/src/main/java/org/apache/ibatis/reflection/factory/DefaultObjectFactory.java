package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.exceptions.ReflectionException;

public class DefaultObjectFactory implements ObjectFactory, Serializable {

	private static final long serialVersionUID = 3751508584689950052L;

	@Override
	public <T> T create(Class<T> type) {
		return create(type, null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		Class<?> classToCreate = resolveInterface(type);		
		return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
	}

	@Override
	public void setProperties(Properties properties) {}	
	
	<T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		try {
			Constructor<T> constructor;
			if (constructorArgTypes == null || constructorArgs == null) {
				constructor = type.getDeclaredConstructor();
				if (!constructor.isAccessible()) {
					constructor.setAccessible(true);
				}
				return constructor.newInstance();
			}
			constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
		} catch (Exception e) {
			StringBuilder argTypes = new StringBuilder();
			if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
				for (Class<?> argType : constructorArgTypes) {
					argTypes.append(argType.getSimpleName());
					argTypes.append(",");
				}
				argTypes.deleteCharAt(argTypes.length() - 1);
			}
			StringBuilder argValues = new StringBuilder();
			if (constructorArgs != null && !constructorArgs.isEmpty()) {
				for (Object argValue : constructorArgs) {
					argValues.append(String.valueOf(argValue));
					argValues.append(",");
				}
				argValues.deleteCharAt(argValues.length() - 1);
			}
			throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
		}
	}
	
	// 如果是集合类型，有可能type是接口类型，这时创建对象有需要使用一个合适的接口实现类类型
	protected Class<?> resolveInterface(Class<?> type) {
		Class<?> classToCreate;
		if (type == List.class || type == Collection.class || type == Iterable.class) {
			classToCreate = ArrayList.class;
		} else if (type == Map.class) {
			classToCreate = HashMap.class;
		} else if (type == SortedSet.class) {
			classToCreate = TreeSet.class;
		} else if (type == Set.class) {
			classToCreate = HashSet.class;
		} else {
			classToCreate = type;
		}
		return classToCreate;
	}
	
	@Override
	public <T> boolean isCollection(Class<?> type) {
		return Collection.class.isAssignableFrom(type);
	}

}
