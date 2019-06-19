package org.apache.ibatis.reflection.test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.reflection.TypeParameterResolver;

/**
 * TypeParameterResolver解析泛型的来源有三种，分别是属性、getter返回值、setter参数值类型
 * 泛型类型一共有四种可能，分别是T(TypeVariable)、List<T>(ParameterizedType)、T[](GenericArrayType)、普通具体类型
 * 3 x 4一共有12种情况，去掉普通具体类型直接返回的情况，一共有9种情况，列举如下：
 * (1) 属性 + TypeVariable 		eg: T t;
 * (2) 属性 + ParameterizedType   eg: List<T> list;
 * (3) 属性 + GenericArrayType    eg: V[] values;
 * (4) 返回值 + TypeVariable       eg: T getT() {}
 * (5) 返回值 + ParameterizedType  eg: List<T> getList() {}
 * (6) 返回值 + GenericArrayType   eg: V[] getValues() {}
 * (7) 参数类型 + TypeVariable      eg: void setT(T t) {}
 * (8) 参数类型 + ParameterizedType eg: void setList(List<T> list) {}
 * (9) 参数类型 + GenericArrayType  eg: void setValues(V[] values) {}
 * 对于属性、返回值、参数值类型的解析，实际上过了入口方法都是一样的，所以只挑(1)、(2)、(3)演示测试案例
 * 
 * 对于形如<? extends>的WildcardType类型，姑且把它叫做"寄生泛型"，必须嵌套在上述三种泛型类型中使用
 */
public class TypeParameterResolverTest {
	SubClassC<Float> sc = new SubClassC<Float>();
	public static void main(String[] args) {
		try {
			// (1) 属性 + TypeVariable
			// [start]
			System.out.println("(1) 属性 + TypeVariable");
			// 1. 演示resolveTypeVar()中走到 (clazz == declaringClass) 分支的情况
			Type type = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("t"), SupClass.class);
			System.out.println(type);
			System.out.println();
			
			// 2. 演示scanSuperTypes()中走到 (superclass instanceof Class) 分支的情况
			Type type2 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("t"), SubClassB.class);
			System.out.println(type2);
			System.out.println();
			
			// 3. 演示scanSuperTypes()中走到!(typeArgs[i] instanceof TypeVariable) 分支的情况
			Type type3 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("t"), SubClassA.class);
			System.out.println(type3);
			System.out.println();
			
			// 4. 演示scanSuperTypes()中走到 (typeArgs[i] instanceof TypeVariable) 分支的情况
			Type srcType = TypeParameterResolverTest.class.getDeclaredField("sc").getGenericType();
			Type type4 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("t"), srcType);
			System.out.println(type4);
			System.out.println();
			
			// 5. 演示scanSuperTypes()中走到 (declaringClass.isAssignableFrom(parentAsClass)) 分支的情况
			Type type5 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("t"), SubClassD.class);
			System.out.println(type5);
			System.out.println();
			// [end]
			
			
			// (2) 属性 + ParameterizedType
			// [start]
			System.out.println("(2) 属性 + ParameterizedType");
			// 1. 如果泛型类型形如List<T>，则会走resolveType的 (type instanceof ParameterizedType) 分支，进入resolveParameterizedType()
			//    根据泛型元素的类型还会有三个分支，分别如下:
			// 1.1  形如 List<T> 的类型满足第一个分支
			// 1.2 形如 List<Set<T>> 的类型满足第二个分支
			// 1.3 形如 List<? extends xxClass> 的类型满足第三个分支，解析WildcardType类型的例子
			Type type6 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("list"), SubClassA.class);
			printTypeInfo(type6);
			System.out.println();
			
			Type type7 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("parameterizedList"), SubClassA.class);
			printTypeInfo(type7);
			System.out.println();
			
			Type type8 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("genericList"), SubClassA.class);
			printTypeInfo(type8);
			System.out.println();
			// [end]
			
			// (3) 属性 + GenericArrayType
			// [start]
			System.out.println("(3) 属性 + GenericArrayType");
			// 测试多了你就会发现，外面的类型其实是一层壳，剥掉之后还是跟(2)一样，resolveGenericArrayType也会有三个分支，此处只演示一个例子，其他分支例子依此类推
			Type type9 = TypeParameterResolver.resolveFieldType(SupClass.class.getDeclaredField("values"), SubClassA.class);
			printTypeInfo(type9);
			System.out.println();						
			// [end]
			
			// 在debug的过程中会发现，所有的类型解析都不是相互割裂开的，而是互相依赖的，因为泛型类型允许无限嵌套，
			// 只要理解好程序处理递归的入口和出口，就能很好理解泛型类型被解析为具体类型的过程
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	// 打印类型信息
	public static void printTypeInfo(Type type) {
		if (type instanceof ParameterizedType) {
			printParameterizedTypeInfo((ParameterizedType) type);
		} else if (type instanceof GenericArrayType) {
			printGenericArrayType((GenericArrayType) type);
		} else if (type instanceof WildcardType) {
			printWildcardType((WildcardType) type);
		} else {
			System.out.println(type);
		}
	}
	
	// 打印ParameterizedType类型信息
	public static void printParameterizedTypeInfo(ParameterizedType parameterizedType) {
		System.out.println("RawType: " + parameterizedType.getRawType());
		System.out.println("OwnerType: " + parameterizedType.getOwnerType());
		System.out.println("ActualTypeArguments: ");
		Type[] actualTypes = parameterizedType.getActualTypeArguments();
		for (int i = 0; i < actualTypes.length; i++) {
			System.out.println("    actualTypes[" + i + "] = " + actualTypes[i]);
			printTypeInfo(actualTypes[i]); 
		}			
	}
	
	// 打印GenericType类型信息
	public static void printGenericArrayType(GenericArrayType genericArrayType) {
		Type type = genericArrayType.getGenericComponentType();
		printTypeInfo(type);
	}
	
	// 打印WildcardType类型信息
	public static void printWildcardType(WildcardType wildcardType) {
		Type[] lowerBounds = wildcardType.getLowerBounds();
		Type[] upperBounds = wildcardType.getUpperBounds();
		for (int i = 0; i < lowerBounds.length; i++) {
			System.out.println("lowerBounds[" + i + "] = ");
			printTypeInfo(lowerBounds[i]);
		}			
		for (int i = 0; i < upperBounds.length; i++) {
			System.out.println("upperBounds[" + i + "] = ");
			printTypeInfo(upperBounds[i]);
		}
	}
}

class SupClass<K, V, T> {
	T t;    
	List<T> list;
	V[] values;
	List<Set<K>> parameterizedList;
	List<? extends V> genericList;
	
	public T getT() {
		return t;
	}
	public void setT(T t) {
		this.t = t;
	}
	public List<T> getList() {
		return list;
	}
	public void setList(List<T> list) {
		this.list = list;
	}
	public V[] getValues() {
		return values;
	}
	public void setValues(V[] values) {
		this.values = values;
	}
	public List<Set<K>> getParameterizedList() {
		return parameterizedList;
	}
	public void setParameterizedList(List<Set<K>> parameterizedList) {
		this.parameterizedList = parameterizedList;
	}
	public List<? extends V> getGenericList() {
		return genericList;
	}
	public void setGenericList(List<? extends V> genericList) {
		this.genericList = genericList;
	}	
}

class SubClassA extends SupClass<String, Integer, Double> {}

class SubClassB extends SubClassA {}

class SubClassC<T> extends SupClass<String, Integer, T> {}

class SubClassD extends SubClassC<String> {}