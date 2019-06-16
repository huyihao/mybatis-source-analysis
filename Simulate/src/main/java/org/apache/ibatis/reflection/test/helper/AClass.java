package org.apache.ibatis.reflection.test.helper;

import java.lang.reflect.Method;

// 本类用来说明桥接方法的生成和作用
// 来源知乎: https://www.zhihu.com/question/54895701/answer/141623158
public class AClass implements AInterface<String> {
	@Override
	public void func(String t) {
		System.out.println(t);	
	}
	
	public static void main(String[] args) throws Exception {
		AClass obj = new AClass();
		Method func = AClass.class.getMethod("func", String.class);
		func.invoke(obj, "AAA");
	    System.out.println(func.isBridge());
	    func = AClass.class.getMethod("func", Object.class);
	    func.invoke(obj, "BBB");
	    System.out.println(func.isBridge());
	}
}
/**
 * 由于接口在虚拟机中泛型被擦除，如下所示:
 * public interface AInterface<Object> {
 *     void func(Object t);
 * }
 * 
 * 这时候AClass没有实现void func(Object)方法，编译器会生成一个方法func(Object t)，满足实现接口方法的定义
 * public class AClass implements AInterface<String> {
 *     public void func(String t) {
 *	       System.out.println(t);	
 *	   }
 *
 *     public void func(Object t) {
 *     	   func((String) t);
 *     }
 * }
 * 
 * 编译器生成的public void func(Object t)就叫桥接方法
 */
