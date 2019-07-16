package org.apache.ibatis.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * 对ClassLoaderWrapper进行包装使用的类,所有功能都基于ClassLoaderWrapper
 * 实际上这些功能都可以整合在ClassLoaderWrapper中，但是多加一层封装，并且使用了Resources的类名
 * 使用起来可能更加容易理解
 * 
 * Resources提供了以下功能:
 * 1) getResource*(): 根据资源路径，将资源转化为URL、InputStream、Properties、Reader、File
 * 2) getUrl*(): 根据url，将网络资源转化为InputStream、Reader、Properties
 * 3) classForName(): 根据类的全限定名得到对应的Class对象
 */
public class Resources {
	
	private static ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();
	
	private static Charset charset;
	
	Resources() {}
	
	public static ClassLoader getDefaultClassLoader() {
		return classLoaderWrapper.defaultClassLoader;
	}
	
	public static void setDefaultClassLoader(ClassLoader classLoader) {
		classLoaderWrapper.defaultClassLoader = classLoader;
	}
	
	public static URL getResourceURL(String resource) throws IOException {
		return getResourceURL(null, resource);
	}
	
	public static URL getResourceURL(ClassLoader classLoader, String resource) throws IOException {
		URL url = classLoaderWrapper.getResourceAsURL(resource, classLoader);
		// 因为返回的url有可能是空的，所以这里再判断一下
		if (url == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return url;
	}
	
	public static InputStream getResourceAsStream(String resource) throws IOException {
		return getResourceAsStream(null, resource);
	}
	
	public static InputStream getResourceAsStream(ClassLoader classLoader, String resource) throws IOException {
		InputStream in = classLoaderWrapper.getResourceAsStream(resource, classLoader);
		if (in == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return in;				
	}
	
	public static Properties getResourceAsProperties(String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = getResourceAsStream(resource);
		props.load(in);
		in.close();
		return props;
	}
	
	public static Reader getResourceAsReader(String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(resource), charset);
		}
		return reader;
	}
	
	public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(loader, resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
		}
		return reader;
	}
	
	public static File getResourceAsFile(String resource) throws IOException {
		return new File(getResourceURL(resource).getFile());
	}
	
	public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException {
		return new File(getResourceURL(loader, resource).getFile());
	}
	
	public static InputStream getUrlAsStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		return conn.getInputStream();
	}
	
	public static Reader getUrlAsReader(String urlString) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getUrlAsStream(urlString));
		} else {
			reader = new InputStreamReader(getUrlAsStream(urlString), charset);
		}
		return reader;
	}
	
	public static Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		InputStream in = getUrlAsStream(urlString);
		props.load(in);
		in.close();
		return props;
	}
	
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		return classLoaderWrapper.classForName(className);
	}

	public static Charset getCharset() {
		return charset;
	}

	public static void setCharset(Charset charset) {
		Resources.charset = charset;
	}
}
