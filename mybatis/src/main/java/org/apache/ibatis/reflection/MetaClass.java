package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 
 * 对类级别的元信息进行封装和处理:
 * (1) 对表达式的解析主要依赖于PropertyTokenizer
 * (2) 对类的元信息的解析主要依赖于Reflector
 * (3) ReflectorFactory对多个类的反射元信息数据进行了缓存
 * 
 */
public class MetaClass {
	
	private ReflectorFactory reflectorFactory;
	private Reflector reflector;
	
	private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
		this.reflectorFactory = reflectorFactory;
		this.reflector = reflectorFactory.findForClass(type);
	}
	
	public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
		return new MetaClass(type, reflectorFactory);
	}
	
	// 这里的name是属性名，即为类中的子属性创建一个对应的MetaClass对象
	public MetaClass metaClassForProperty(String name) {
		Class<?> propType = reflector.getGetterType(name);
		return MetaClass.forClass(propType, reflectorFactory);
	}
	
	// 根据属性表达式对应的PropertyTokenizer对象获取表达式所要表达的属性的类型，并根据类型创建一个对应的MetaClass对象
	public MetaClass metaClassForProperty(PropertyTokenizer prop) {
		//Class<?> propType = reflector.getGetterType(prop.getName());
		Class<?> propType = getGetterType(prop);
		return MetaClass.forClass(propType, reflectorFactory);
	}
	
	// 这里的name是属性表达式，既然是表达式，就应该用expression作为参数名，起个name让人误解垃圾
	public String findProperty(String name) {
		StringBuilder prop = buildProperty(name, new StringBuilder());
		return prop.length() > 0 ? prop.toString() : null;
	}
	
	public String findProperty(String name, boolean useCamelCaseMapping) {
		if (useCamelCaseMapping) {
			name = name.replace("_", "");
		}
		return findProperty(name);
	}
	
	// 返回类中所有可读属性
	public String[] getGetterNames() {
		return reflector.getGetablePropertyNames();
	}
	
	public String[] getSetterNames() {
		return reflector.getSetablePropertyNames();
	}
	
	// 这里有点疑问？假如name="order[0]"，到底是要获取order的类型还是order里面元素的类型？
	// 跟下面的getGetterType没有保持一致
	public Class<?> getSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop.getName());
			return metaProp.getSetterType(prop.getChildren());
		} else {
			return reflector.getSetterType(prop.getName());
		}
	}
	
	public Class<?> getGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop);
			return metaProp.getGetterType(prop.getChildren());
		}
		// 如果这里直接使用reflector.getGetterType(prop.getName())，可能得到的会是个Collection类型
		// eg: name="order[0]"，假设order是某个对象内的列表，这个很明显想获取列表中元素的类型
		return getGetterType(prop);
	}
	
	/**
	 *  传进来的属性表达式中的name在类里面可能是一个集合类型，则返回其中的元素类型
	 *  eg: List<element> order;  // element类中有items这个成员
	 *      order[0].items
	 *  (1) 如果是简单类型的数组，则直接返回数组元素类型，eg: int[]返回int.class
	 *  (2) 如果是集合类型的数组，从理论上说都应该是Collection<T>的这种形势
	 *      2.1 如果T是一个Class类型，则直接返回即可，eg: List<String>返回String.class
	 *      2.2 如果T还是一个嵌套的ParameterizedType类型，则返回其集合类型，eg: List<Set<Double>>返回Set.class
	 */		
	private Class<?> getGetterType(PropertyTokenizer prop) {
		Class<?> type = reflector.getGetterType(prop.getName());
		if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
			Type returnType = getGenericGetterType(prop.getName());
			if (returnType instanceof ParameterizedType) {
				Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
				// 这里之所以要判断actualTypeArguments.length == 1，是因为要筛选数组类型的ParameterizedType，这样可以拿到集合中元素的具体类型
				// eg: List<Double>
				// 如果actualTypeArguments.length != 1，可能是存在多个数据类型数据结构，eg: Map<K, V>
				// 但是属性表达式只支持数组类型，不支持带键值的表达式，eg: "map['key'].prop"，而且前面对参数类型也做了判断，必须是Collection，理论上代码应该是不会走到非if的情况
				if (actualTypeArguments != null && actualTypeArguments.length == 1) {
					returnType = actualTypeArguments[0];
					if (returnType instanceof Class) {
						type = (Class<?>) returnType;
					} else if (returnType instanceof ParameterizedType) {
						type = (Class<?>) ((ParameterizedType) returnType).getRawType();
					}
				}
			}
		}
		return type;
	}
	
	// 类中定义的属性可能是带泛型的，这时需要解析获取带泛型集合类型的实际类型
	// 类的属性、参数、方法返回值的具体类型信息，实际上是存储在Invoker里
	private Type getGenericGetterType(String propertyName) {
		try {
			Invoker invoker = reflector.getGetInvoker(propertyName);
			if (invoker instanceof MethodInvoker) {
				Field _method = MethodInvoker.class.getDeclaredField("method");
				_method.setAccessible(true);
				Method method = (Method) _method.get(invoker);
				return TypeParameterResolver.resolveReturnType(method, reflector.getType());
			} else if (invoker instanceof GetFieldInvoker) {
				Field _field = GetFieldInvoker.class.getDeclaredField("field");
				_field.setAccessible(true);
				Field field = (Field) _field.get(invoker);
				return TypeParameterResolver.resolveFieldType(field, reflector.getType());
			}
	    } catch (NoSuchFieldException e) {
	    } catch (IllegalAccessException e) {
	    }
		return null;
	}
	
	public StringBuilder buildProperty(String name, StringBuilder builder) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			String propertyName = reflector.findPropertyName(prop.getName());
			if (propertyName != null) {
				builder.append(propertyName);
				builder.append(".");
				MetaClass metaProp = metaClassForProperty(propertyName);
				metaProp.buildProperty(prop.getChildren(), builder);
			}
		} else {
			String propertyName = reflector.findPropertyName(name);
			if (propertyName != null) {
				builder.append(propertyName);
			}
		}
		return builder;
	}
	
	// 根据属性表达式判断是否有属性的setter，需要一级一级判断属性是否存在
	// Q: 假如要遵循这套标准，生成子属性对应的MetaClass对象需要先判断子属性是否存在，为啥上面的方法不这样做？
	// A: 假如在当前类的reflector中找不到属性表达式中子属性的类型，则会抛出异常，但这里要判断是否存在，不存在返回false
	// eg: "customer.order[0].item"，需要先判断customer属性在当前reflector中是否存在
	//     若存在获取customer属性类型对应的MetaClass，再对"order[0].item"进行解析判断，以此类推
	public boolean hasSetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (reflector.hasSetter(prop.getName())) {
				MetaClass metaProp = metaClassForProperty(prop.getName());
				return metaProp.hasSetter(prop.getChildren());					
			} else {
				return false;
			}
		} else {
			return reflector.hasSetter(prop.getName());
		}
	}
	
	public boolean hasGetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (reflector.hasGetter(prop.getName())) {
				MetaClass metaProp = metaClassForProperty(prop);
				return metaProp.hasGetter(prop.getChildren());
			} else {
				return false;
			}
		} else {
			return reflector.hasGetter(prop.getName());
		}
	}
	
	public Invoker getGetInvoker(String name) {
	    return reflector.getGetInvoker(name);
    }

	public Invoker getSetInvoker(String name) {
	    return reflector.getSetInvoker(name);
	}
	
	public boolean hasDefaultConstructor() {
		return reflector.hasDefaultConstructor();
	}
}
