package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 【源码注释翻译】
 * <p>ResolverUtil用来定位满足任何情况下在classpth中可用的类。
 * 任意情况中最常见的两种情况是:<br/>
 * (1) 一个继承或实现了其他类的类<br/>
 * (2) 被特定的注解注解了的类<br/>
 * 但是，通过使用 {@link Test} 类，可以找到任意一种情况下的类
 * </p>
 * 
 * <p>一个类加载器(ClassLoader)用来定位包含所有带确定包的类的classpath中的任何地方 (目录和jar文件)。
 * 通过使用 {@code Thread.currentThread().getContextClassLoader()} 可以返回一个默认的ClassLoader，
 * 但是为了调用任一 {@code find()} 可以通过先调用 {@link #setClassLoader(ClassLoader)} 来覆盖 
 * </p>
 * 
 * <p>通用的查询可以通过调用 {@link #find(org.apache.ibatis.io.ResolverUtil.Test, String)} ()}
 * 方法，传入一个Test实例和一个包名来使用。这会导致所有被扫描的命名包和它的<b>所有子包</b>满足测试的类。还有用于扫描多个包
 * 以用于特定类的扩展或用特定注释注释的类的常见用例的实用方法</p>
 * 
 * <p>几种使用ResolverUtil类的标准模式如下:</p>
 * 
 * <pre>
 * ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 * resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 * resolver.find(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg2);
 * Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 * </pre>
 * 
 * ResolverUtil提供的功能如下:
 * 	   筛选某个/多个包中所有类中属于某种类的类的集合
 */
public class ResolverUtil<T> {
	
	public static interface Test {
		boolean matches(Class<?> type);
	}
	
	// 检查某个指定的类B是否属于类A
	public static class IsA implements Test {
		private Class<?> parent;
		
		public IsA(Class<?> parentType) {   // 类A
			this.parent = parentType;
		}
		
		@Override
		public boolean matches(Class<?> type) {	// 类B		
			return type != null && parent.isAssignableFrom(type);
		}
		
		@Override
		public String toString() {
			return "is assignable to " + parent.getSimpleName();
		}
	}
	
	// 检测某个指定的注解类B是否属于注解类A(注解也可继承)
	public static class AnnotatedWith implements Test {
		private Class<? extends Annotation> annotation;
		
		public AnnotatedWith(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}
		
		@Override
		public boolean matches(Class<?> type) {		
			return type != null && type.isAnnotationPresent(annotation);
		}
		
		@Override
		public String toString() {
			return "annotated with @" + annotation.getSimpleName();
		}
	}

	// 解析到的符合匹配条件的包中的类
	private Set<Class<? extends T>> matches = new HashSet<Class<? extends T>>();
	
	private ClassLoader classLoader;

	public ClassLoader getClassLoader() {
		return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public Set<Class<? extends T>> getClasses() {
		return matches;
	}
	
	public ResolverUtil<T> findImplementation(Class<?> parent, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		
		Test test = new IsA(parent);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		
		return this;
	}
	
	public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		
		Test test = new AnnotatedWith(annotation);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		
		return this;
	}
	
	public ResolverUtil<T> find(Test test, String packageName) {
		String path = getPackagePath(packageName);
		
		try {
			List<String> children = VFS.getInstance().list(path);
			for (String child : children) {
				if (child.endsWith(".class")) {
					addIfMatching(test, child);					
				}
			}
		} catch (IOException ioe) {
			// TODO: handle exception
		}
		
		return this;
	}
	
	/**
	 * 将包的全限定名转换为路径
	 * eg: com.learn.ssm.chapter3.pojo  => com/learn/ssm/chapter3/pojo
	 */	
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	@SuppressWarnings("unchecked")
	protected void addIfMatching(Test test, String fqn) {
		try {
			// eg: com/learn/ssm/chapter3/pojo/Role.class 
			//  => com.learn.ssm.chapter3.pojo.Role
			String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
			ClassLoader loader = getClassLoader();
			
			Class<?> type = loader.loadClass(externalName);
			if (test.matches(type)) {
				matches.add((Class<T>) type);
			}
		} catch (Throwable t) {
			
		}
	}
}