package org.apache.ibatis.io.test;

public class ResolverUtilTest {
	public static void main(String[] args) throws ClassNotFoundException {
//		String fqn = "com/learn/ssm/chapter3/pojo/Role.class";
//		String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
//		System.out.println(externalName);
		
		String externalName = "org.apache.ibatis.parsing.test.XNodeTest";
		Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(externalName);
		System.out.println(type);
	}
}
