package org.apache.ibatis.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// 解析泛型类型，获取其实际类型
// eg: StringReference<String> extends TypeException<String>
//     拿到T的类型为String.class
public abstract class TypeReference<T> {
	
	private final Type rawType;
	
	protected TypeReference() {
		rawType = getSuperclassTypeParameter(getClass());
	}
	
	Type getSuperclassTypeParameter(Class<?> clazz) {
		Type genericSuperclass = clazz.getGenericSuperclass();
		// 处理泛型父类不带泛型的情况
		// eg: public class SubTypeReference extends TypeReference {}		
		if (genericSuperclass instanceof Class) {
			// 假如不带泛型参数但不直接继承TypeReference，可能还可以解析出来，递归处理
			// eg: public class SubTypeReference extends TypeReference<String> {}
			//     public class StringSubReference extends SubTypeReference {}
			// 递归第一轮解析出来的genericSuperclass是SubTypeReference
			// 递归第二轮解析出来的genericSuperclass是TypeReference<String>，这是个ParameterizedType，可以从中解析出实际类型String			
			if (TypeReference.class != genericSuperclass) {
				return getSuperclassTypeParameter(clazz.getSuperclass());
			}
			
			// 假如直接继承了TypeReference但又不带泛型参数，抛出异常
			throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. " + 
									"Remove the extension or add a type parameter to it.");
		}
		
		// 假如ParameterizedType参数的实际类型还是个ParameterizedType，这时应该去参数化类型中的原始类型
		// eg: TypeReference<List<String>>  
		//     ==> rawType = List<String>;   
		//     ==> rawType = List;		
		Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}
		return rawType;
	}
	
	public final Type getRawType() {
		return rawType;
	}
	
	@Override
	public String toString() {
		return rawType.toString();
	}
}
