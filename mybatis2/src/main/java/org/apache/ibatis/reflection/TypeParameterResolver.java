package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * 类型参数解析器
 * 从Reflector的解析逻辑可知，Reflector不仅会解析传入的Class构造参数类中定义的属性、getter、setter
 * 还会解析继承链上所有祖先类、实现的所有接口中定义的属性、getter、setter，这使得通过反射获取到的属性、getter
 * 返回值、setter的参数值类型都有可能不是具体类型，而是泛型，eg:
 * class SupClass<T> {
 * 	  T t;
 * }
 * class SubClass extends SupClass<String> { ... }
 * 如果使用Reflector获取SubClass.class的元信息，那么会得到一个类型为T的属性t，但是很明显，对于SubClass，
 * 它从父类继承的属性t的类型应该是String，TypeParameterResolver的作用就是对这种泛型进行解析的类。
 * 
 * 核心处理逻辑:
 * 泛型类型往往是从父类、接口(declaringClass)中定义的，在继承父类、实现接口的类中(srcType)，往往会给泛型赋予
 * 一个具体类型，通过解析srcType中的具体类型跟declaringClass中泛型的映射关系，就能得到泛型的具体类型，如上面的
 * 例子中，泛型是T，具体类型是String，很容易推断SubClass继承的属性t的实际类型就是String，这个信息是从类定义中
 * 可以提取到的；当然，还有另外一种形式具体类型的信息的获取，eg:
 * 	  class SubClass<T> extends SupClass<T> { ... }
 *    SubClass<String> obj = new SubClass<String>();
 * 子类没有指定泛型T的具体类型，T的具体类型由子类的实例化对象决定，这种情况就需要通过对象获取泛型类型的信息
 */
public class TypeParameterResolver {
	// 解析属性真实类型的入口方法
	public static Type resolveFieldType(Field field, Type srcType) {
		Type fieldType = field.getGenericType();
		Class<?> declaringClass = field.getDeclaringClass();
		return resolveType(fieldType, srcType, declaringClass);
	}
	
	// 解析getter方法返回值真实类型的入口方法
	public static Type resolveReturnType(Method method, Type srcType) {
		Type returnType = method.getGenericReturnType();
		Class<?> declaringClass = method.getDeclaringClass();
		return resolveType(returnType, srcType, declaringClass);
	}
	
	// 解析setter方法参数值真实类型的入口方法
	public static Type[] resolveParamTypes(Method method, Type srcType) {
		Type[] paramTypes = method.getGenericParameterTypes();
		Class<?> declaringClass = method.getDeclaringClass();
		Type[] result = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			result[i] = resolveType(paramTypes[i], srcType, declaringClass);
		}
		return result;
	}
	
	private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
		if (type instanceof TypeVariable) {    // eg: Map<K, V> => K, V
			return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
		} else if (type instanceof ParameterizedType) {  // eg: List<T>
			return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
		} else if (type instanceof GenericArrayType) {   // eg: List<T>[]  T[]
			return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
		} else {
			return type;      // 参数类型是具体类型，直接返回即可
		}
	}
	
	private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
		// 对于数组类型，要解析其元素的具体类型，外层的类型没有意义
		Type componentType = genericArrayType.getGenericComponentType();
		Type resolvedComponentType = null;
		if (componentType instanceof TypeVariable) {
			resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
		} else if (componentType instanceof ParameterizedType) {
			resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
		} else if (componentType instanceof GenericArrayType) {
			resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
		} 
		if (resolvedComponentType instanceof Class) {
			return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
		} else {
			return new GenericArrayTypeImpl(resolvedComponentType);
		}
	}
	
	private static Type resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
		Class<?> rawType = (Class<?>) parameterizedType.getRawType();
		Type[] typeArgs = parameterizedType.getActualTypeArguments();
		Type[] args = new Type[typeArgs.length];
		for (int i = 0; i < typeArgs.length; i++) {
			if (typeArgs[i] instanceof TypeVariable) {
				args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
			} else if (typeArgs[i] instanceof ParameterizedType) {
				args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
			} else if (typeArgs[i] instanceof WildcardType) {
				args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
			} else {
				args[i] = typeArgs[i];
			}
		}
		return new ParameterizedTypeImpl(rawType, null, args);
	}	
	
	private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
		Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
		Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
		return new WildcardTypeImpl(lowerBounds, upperBounds);
	}
	
	private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
		Type[] result = new Type[bounds.length];
		for (int i = 0; i < bounds.length; i++) {
			if (bounds[i] instanceof TypeVariable) {
				result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
			} else if (bounds[i] instanceof ParameterizedType) {
				result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
			} else if (bounds[i] instanceof WildcardType) {
				result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
			} else {
				result[i] = bounds[i];
			}
		}
		return result;
	}
	
	private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
		Type result = null;
		Class<?> clazz = null;
		// 判断srcType的类型
		// (1) Class: 直接通过类的Class对象元信息来解析实际类型
		// (2) ParameterizedType: 通过对象的类型信息来解析实际类型，eg: SubClass<String>
		if (srcType instanceof Class) {
			clazz = (Class<?>) srcType;
		} else if (srcType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) srcType;
			clazz = (Class<?>) parameterizedType.getRawType();
		} else {
			throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
		}
		
		// 如果srcType就是定义了泛型的类本身，那没什么好解析的，所有的类型都继承自Object，因此返回Object.class
		if (clazz == declaringClass) {
			return Object.class;
		}
		
		// 假如clazz != declaringClass，证明该类型是继承父类或实现接口得到的，这时有机会解析得到真正的类型，
		// 当然如果子类没有对继承的泛型赋值为具体类型，那最终解析到的泛型还是跟泛型在类中定义的一样返回Object.class
		// eg: 
		//    class SupClass<T>{}
		//    class SubClass<T> extends SupClass<T>{}
		Type superclass = clazz.getGenericSuperclass();
		result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
		if (result != null) {   // 如果等于null就是上面注释说的情况
			return result;
		}
		
		Type[] superInterfaces = clazz.getGenericInterfaces();
		for (Type superInterface : superInterfaces) {
			result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
			if (result != null) {
				return result;
			}
		}
		
		return Object.class;
	}
	
	private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
		Type result = null;
		if (superclass instanceof ParameterizedType) {    // eg: superClass = SupClass<String, Integer, T>
			ParameterizedType parentAsType = (ParameterizedType) superclass;
			Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();   // parentAsClass = SupClass
			if (declaringClass == parentAsClass) {
				Type[] typeArgs = parentAsType.getActualTypeArguments();   // [String, Integer, T]
				TypeVariable<?>[] declaredTypeVars = declaringClass.getTypeParameters();  // [K, V, T]
				for (int i = 0; i < declaredTypeVars.length; i++) {
					if (declaredTypeVars[i] == typeVar) {
						// 只有对类的实例对象使用反射获取类型才会走到这个分支
						if (typeArgs[i] instanceof TypeVariable) {
							TypeVariable<?>[] typeParams = clazz.getTypeParameters();  // SubClassC<T> => [T]
							for (int j = 0; j < typeParams.length; j++) {
								if (typeParams[j] == typeArgs[i]) {
									if (srcType instanceof ParameterizedType) {
										result = ((ParameterizedType) srcType).getActualTypeArguments()[j];  // SubClass<Float> => [Float]
									}
									break;
								}
							}
						} else {
							// TODO. 这里不太明白为啥不break?
							result = typeArgs[i];
						} 
					} 
				}
			} 
			// 继续解析父类，直到解析到指定该泛型具体类型的类
			// eg: superClass = A<V>, declaringClass = SupClass.class, parentAsClass = A.class
			//     class SupClass<K, V>{}
			//     class A<V> extends SupClass<String, V>{}
			//     class B extends A<Double>{}
			else if (declaringClass.isAssignableFrom(parentAsClass)) {
				result = resolveTypeVar(typeVar, parentAsType, declaringClass);
			}
		} 
		// 假如继承链中A类对父类的全部泛型都赋予了具体类型，则A类的子类B不需要继承或指定A类的泛型
		// 这种情况下解析泛型的具体类型其实跟类B无关了，递归调用resolveType()将srcType由B.class替换为A.class
		// eg: srcType = B.class, superClass = A.class, declaringClass = SupClass.class
		//     class SupClass<T>{}
		//     class A extends SupClass<String>{}
		//     class B extends A{}
		else if (superclass instanceof Class) {
			if (declaringClass.isAssignableFrom((Class<?>) superclass)) {
				result = resolveTypeVar(typeVar, superclass, declaringClass);
			}
		}
		return result;
	}

	// 避免客户程序实例化本类
	private TypeParameterResolver() {
		super();
	}
	
	static class ParameterizedTypeImpl implements ParameterizedType {

		private Class<?> rawType;
		private Type ownerType;
		private Type[] actualTypeArguments;
		
		public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
			super();
			this.rawType = rawType;
			this.ownerType = ownerType;
			this.actualTypeArguments = actualTypeArguments;
		}
		
		@Override
		public Type[] getActualTypeArguments() {
			return actualTypeArguments;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		public Type getOwnerType() {
			return ownerType;
		}
		
	}

	static class WildcardTypeImpl implements WildcardType {

		private Type[] upperBounds;
		private Type[] lowerBounds;
		
		public WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
			super();
			this.lowerBounds = lowerBounds;
			this.upperBounds = upperBounds;
		}
		
		@Override
		public Type[] getUpperBounds() {
			return upperBounds;
		}

		@Override
		public Type[] getLowerBounds() {
			return lowerBounds;
		}		
	}
	
	static class GenericArrayTypeImpl implements GenericArrayType {
		private Type genericComponentType;
		
		public GenericArrayTypeImpl(Type genericComponentType) {
			super();
			this.genericComponentType = genericComponentType;
		}
		
		@Override
		public Type getGenericComponentType() {
			return genericComponentType;
		}		
	}
}
