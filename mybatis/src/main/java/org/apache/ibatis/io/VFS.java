package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * VFS(Virtual File System) 虚拟文件系统
 */
public abstract class VFS {
	private static final Log log = LogFactory.getLog(VFS.class);
	
	// 记录了MyBatis提供的两个VFS实现类
	public static final Class<?>[] IMPLEMENTATIONS = { JBoss6VFS.class, DefaultVFS.class };
	
	// 记录了用户自定义的VFS实现类。VFS.addImplClass() 方法会将制定的VFS实现对应的Class对象添加到USER_IMPLEMENTS集合中
	public static final List<Class<? extends VFS>> USER_IMPLEMENTS = new ArrayList<Class<? extends VFS>>();
	
	// 单例模式，记录了全局唯一的VFS对象
	private static VFS instance;
	
	@SuppressWarnings("unchecked")
	public static VFS getInstance() {
		if (instance != null) {
			return instance;
		}
		
		// 优先使用用户自定义的VFS实现，如果没有自定义VFS实现，则使用MyBatis提供的VFS实现
		List<Class<? extends VFS>> impls = new ArrayList<Class<? extends VFS>>();
		impls.addAll(USER_IMPLEMENTS);
		impls.addAll(Arrays.asList((Class<? extends VFS>[]) IMPLEMENTATIONS));
		
		// 遍历impls集合，依次实例化VFS对象并检测VFS对象是否有效，一旦得到有效的VFS对象，则结束循环
		VFS vfs = null;
		for (int i = 0; vfs == null || !vfs.isValid(); i++) {
			Class<? extends VFS> impl = impls.get(i);			
			try {
				vfs = impl.newInstance();
				if (vfs == null || !vfs.isValid()) {
					if (log.isDebugEnabled()) {
						log.debug("VFS implementation " + impl.getName() + " is not valid in this environment.");
					}
				}					
			} catch (InstantiationException e) {
				log.error("Failed to instantiate " + impl, e);
				return null;
			} catch (IllegalAccessException e) {
				log.error("Failed to instantiate " + impl, e);
				return null;
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Using VFS adapter " + vfs.getClass().getName());
		}
		
		VFS.instance = vfs;
		return VFS.instance;
	}
	
	// 添加用户自定义VFS的实现
	public static void addImplClass(Class<? extends VFS> clazz) {
		if (clazz != null) {
			USER_IMPLEMENTS.add(clazz);
		}
	}
	
	// 根据类名加载一个类，如果加载失败异常则返回null
	protected static Class<?> getClass(String className) {
		try {
			return Thread.currentThread().getContextClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			if (log.isDebugEnabled()) {
				log.debug("Class not found: " + className);
			}
		}
		return null;
	}
	
	// 根据指定类的方法名和参数类型列表获取对应的Method对象
	protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		if (clazz == null) {
			return null;
		}
		try {
			return clazz.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			log.error("Method not found " + clazz.getName() + "." + methodName + ".  Cause: " + e);
			return null;
		} catch (SecurityException e) { 
			log.error("Security exception looking for method " + clazz.getName() + "." + methodName + ".  Cause: " + e);
			return null;
		}
	}
	
	// 实际上就是通过放射调用类的方法，只不过增加了对异常的转化处理
	@SuppressWarnings("unchecked")
	protected static <T> T invoke(Method method, Object object, Object... parameters) throws IOException {
		try {
			return (T) method.invoke(object, parameters);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof IOException) {
				throw (IOException) e.getTargetException();
			} else {
				throw new RuntimeException(e);
			}
		}
	}
	
	// 根据资源路径获取到该路径下所有资源的URL列表(一般是包相对classpath的文件路径)
	// eg: com/learn/ssm/chapter3/pojo  => URL(file:/F:/mybatis/workspace/mybatis-chapter5/target/classes/com/learn/ssm/chapter3/pojo)
	protected static List<URL> getResources(String path) throws IOException {
		return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
	}		
	
	// 抽象方法，子类实现，负责检测当前VFS对象在当前环境下是否有效
	public abstract boolean isValid();
	
	// 子类实现
	protected abstract List<String> list(URL url, String forPath) throws IOException;
	
	/**
	 * 找到某一目录下的所有所有.class文件(用路径字符表示)
	 */
	public List<String> list(String path) throws IOException {
		List<String> name = new ArrayList<String>();
		for (URL url : getResources(path)) {
			name.addAll(list(url, path));
		}
		return name;
	}
}
