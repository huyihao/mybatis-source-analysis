package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;


/**
 * 方法参数名解析器
 */
public class ParamNameResolver {

	private static final String GENERIC_NAME_PREFIX = "param";
	private static final String PARAMETER_CLASS = "java.lang.reflect.Parameter";
	private static Method GET_NAME = null;
	private static Method GET_PARAMS = null;
	
	static {
		try {
			Class<?> paramClass = Resources.classForName(PARAMETER_CLASS);
			GET_NAME = paramClass.getMethod("getName");
			GET_PARAMS = Method.class.getMethod("getParameters");
		} catch (Exception e) {
			// ignore
		}
	}	
	
	/**
	 * 记录参数在参数列表中的位置索引与参数名称之间的对应关系
	 * <p>
	 * key表示参数在参数列表中的索引位置，value表示参数名称。
	 * 如果使用了{@link Param}注解，则value为注解指定的名称，否则使用参数数字索引作为其名称；
	 * 如果参数列表中包含 {@link RowBounds} 或 {@link ResultHandler} 类型的参数，则
	 * 这两种类型的参数并不会被记录到name集合中，这会导致参数的索引与名称不一致
	 * </p>
	 * <ul>
	 * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
	 * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
	 * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
	 * </ul>
	 */
	private final SortedMap<Integer, String> names;
	
	// 记录方法的参数列表中是否使用了@Param注解
	private boolean hasParamAnnotation;
	
	public ParamNameResolver(Configuration config, Method method) {
		// 获取参数列表中每个参数的类型
		final Class<?>[] paramType = method.getParameterTypes();
		// 获取参数列表上的注解
		final Annotation[][] paramAnnotations = method.getParameterAnnotations();
		// 该集合用于记录参数索引与参数名称的对应关系
		final SortedMap<Integer, String> map = new TreeMap<Integer, String>();
		// 使用了注解的参数个数
		int paramCount = paramAnnotations.length;		
		
		for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
			if (isSpecialParameter(paramType[paramIndex])) {
				// 如果参数是RowBounds类型或ResultHandler类型，则跳过对该参数的分析
				continue;
			}
			
			// 遍历该参数对一个的注解集合
			String name = null;
			for (Annotation annotation : paramAnnotations[paramIndex]) {
				if (annotation instanceof Param) {
					// @Param注解出现过一次，就将hasParamAnnotation初始化为true
					hasParamAnnotation = true;
					name = ((Param) annotation).value();  // 获取@Param注解指定的参数名称
					break;
				}
			}
			
			if (name == null) {
				// 该参数没有对应的@Param注解，则根据配置决定是否使用参数实际名称作为其名称
				if (config.isUseActualParamName()) {
					name = getActualParamName(method, paramIndex);
				}
				if (name == null) {
					// 使用参数索引作为参数名字，一般是从"0"开始，所以这里就用map.size()
					name = String.valueOf(map.size());
				}
			}
			map.put(paramIndex, name);
		}
		names = Collections.unmodifiableSortedMap(map);
	}
	
	// 获取方法索引地址的实际参数名
	public String getActualParamName(Method method, int paramIndex) {
		if (GET_PARAMS == null) {
			return null;
		}
		try {
			// 为什么不能采取下面的做法，而一定要通过反射实现？没看懂？
			// Parameter[] params = method.getParameters();
			// return params[paramIndex].getName();
			
			Object[] params = (Object[]) GET_PARAMS.invoke(method);
			return (String) GET_NAME.invoke(params[paramIndex]);
		} catch (Exception e) {
			throw new ReflectionException("Error occurred when invoking Method#getParameters().", e);
		}
	}
	
	// 判断一个类是否是RowBounds或ResultHandler
	private static boolean isSpecialParameter(Class<?> clazz) {
		return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
	}
	
	// 返回SQL providers引用的参数名称数组
	public String[] getName() {
		return names.values().toArray(new String[0]);
	}	
	
	// 根据用户传入的实参列表，返回实参与名称管理的map
	public Object getNamedParams(Object[] args) {
		final int paramCount = names.size();
		if (args == null || paramCount == 0) {  // 无参数，返回null
			return null;
		} else if (!hasParamAnnotation && paramCount == 1) {  // 未使用@Param且只有一个参数
			return args[names.firstKey()];
		} else {  // 处理使用@Param注解指定可参数名称或有多个参数的情况
			// param这个Map中记录了参数名称与实参之间的对应关系。ParamMap继承了HashMap
			// ParamMap中添加已经存在的key，会报错，其他行为与HashMap相同，得到参数名跟参数值的映射关系
			final Map<String, Object> param = new ParamMap<Object>();
			int i = 0;
			for (Map.Entry<Integer, String> entry : names.entrySet()) {
				param.put(entry.getValue(), args[entry.getKey()]);
				final String genericParaName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
				// 在不对参数命名的情况下，默认使用下面的格式
				// 下面是为参数创建"param+索引"格式的默认参数名称，例如：param1, param2等，并添加
				// 到param集合中
				if (!names.containsValue(genericParaName)) {
					param.put(genericParaName, args[entry.getKey()]);
				}
				i++;				
			}
			return param;
		}
	}
}
