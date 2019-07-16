package org.apache.ibatis.io;

import java.io.InputStream;
import java.net.URL;

/**
 * 多个ClassLoader的使用的包装类，使得使用多个ClassLoader时像使用一个类加载器一样
 * 将资源路径转化为URL、InputStream、Class
 */
public class ClassLoaderWrapper {
	
	ClassLoader defaultClassLoader;
	ClassLoader systemClassLoader;
	
	ClassLoaderWrapper() {
		try {
			systemClassLoader = ClassLoader.getSystemClassLoader();
		} catch (SecurityException ignored) {			
		}
	}
	
	public URL getResourceAsURL(String resource) {
		return getResourceAsURL(resource, getClassLoader(null));
	}
	
	public URL getResourceAsURL(String resource, ClassLoader classLoader) {
		return getResourceAsURL(resource, getClassLoader(classLoader));
	}	
	
	public InputStream getResourceAsStream(String resouce) {
		return getResourceAsStream(resouce, getClassLoader(null));
	}
	
	public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
		return getResourceAsStream(resource, getClassLoader(classLoader));
	}
	
	public Class<?> classForName(String name) throws ClassNotFoundException {
		return classForName(name, getClassLoader(null));
	}
	
	public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
		return classForName(name, getClassLoader(classLoader));
	}
	
	URL getResourceAsURL(String resource, ClassLoader[] classLoader) {
		URL url;
		for (ClassLoader cl : classLoader) {
			// null是系统导引类加载器(Bootstrap ClassLoader)的表示，不用于加载应用程序类
			if (null != cl) {
				// 尝试找到传参进来的资源
				url = cl.getResource(resource);
				if (null == url) {
					// 有一些类加载器需要资源路径以"/"开头，所以如果上面加载失败的话，这里再补上"/"重新尝试一下加载资源
					url = cl.getResource("/" + resource);
				}
				if (null != url) {
					return url;
				}
			}
		}
		return null;
	}
	
	InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
		InputStream inputStream;
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				inputStream = cl.getResourceAsStream(resource);
				if (null == inputStream) {
					inputStream = cl.getResourceAsStream("/" + resource);
				}
				if (null != inputStream) {
					return inputStream;
				}
			}
		}
		return null;
	} 
	
	Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				try {
					Class<?> c = Class.forName(name, true, cl);
					if (null != c) {
						return c;
					}
				} catch (ClassNotFoundException e) {
					// 先忽略异常，知道所有的类加载器都无法加载获取这个类才在循环外抛异常
				}
			}
		}
		throw new ClassNotFoundException("Cannot find class: " + name);
	}
	
	/**
	 *  返回一个ClassLoader的数组，优先级如下:
	 *  指定的ClassLoader > 默认的ClassLoader(实际上是null,代表BootstrapClassLoader,如果调用了Resources的setDefaultClassLoader方法则可以指定默认的类加载器) 
	 *                  > 当前线程的上下文ClassLoader > 加载本类的ClassLoader > 系统ClassLoader(一般为ApplicationClassLoader)
	 */	
	ClassLoader[] getClassLoader(ClassLoader classLoader) {
		return new ClassLoader[] {
			classLoader,
			defaultClassLoader,
			Thread.currentThread().getContextClassLoader(),
			getClass().getClassLoader(),
			systemClassLoader
		};
	}
}
