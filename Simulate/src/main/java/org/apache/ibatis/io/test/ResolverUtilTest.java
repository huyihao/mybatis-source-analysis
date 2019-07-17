package org.apache.ibatis.io.test;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.test.helper.Person;

public class ResolverUtilTest {
	public static void main(String[] args) throws ClassNotFoundException {
//		String fqn = "com/learn/ssm/chapter3/pojo/Role.class";
//		String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
//		System.out.println(externalName);
		
//		String externalName = "org.apache.ibatis.parsing.test.XNodeTest";
//		Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(externalName);
//		System.out.println(type);
		
		// 1. 测试在包中寻找继承了
		testIsA();
	}
	
	public static void testIsA() {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		resolverUtil.find(new ResolverUtil.IsA(Person.class), "org.apache.ibatis.io.test.helper");
		for (Class<?> type : resolverUtil.getClasses()) {
			System.out.println(type);
		}
	}
}
