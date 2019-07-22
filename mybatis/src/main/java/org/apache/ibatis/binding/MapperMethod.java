package org.apache.ibatis.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 * 封装了MapperMethod接口中对应的方法的信息，以及对应SQL语句的信息
 * MapperMethod对象会完成参数转换以及SQL语句的执行功能 
 */
public class MapperMethod {

	private final SqlCommand command;
	private final MethodSignature method;
	
	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		this.command = new SqlCommand(config, mapperInterface, method);
		this.method = new MethodSignature(config, mapperInterface, method);
	}
	
	public Object execute(SqlSession sqlSession, Object[] args) {
		Object result = null;
		// 根据SQL语句的类型调用SqlSession对应的方法完成数据库操作
		switch (command.getType()) {
			case INSERT: {
				// 使用ParamNameResolver处理args[]数组（用户传入的实参列表），将用户传入的实参与指定参数名称关联起来
				Object param = method.convertArgsToSqlCommandParam(args);
				// 调用SqlSession.insert()方法，rowCountResult()方法会根据method字段中记录的方法的返回值类型对结果进行转换
				result = rowCountResult(sqlSession.insert(command.getName(), param));
				break;
			}
		    case UPDATE: {
		        Object param = method.convertArgsToSqlCommandParam(args);
		        result = rowCountResult(sqlSession.update(command.getName(), param));
		        break;
		    }
		    case DELETE: {
		        Object param = method.convertArgsToSqlCommandParam(args);
		        result = rowCountResult(sqlSession.delete(command.getName(), param));
		        break;
		    }
		    case SELECT:
		    	// 处理返回值为void且ResultSet通过ResultHandler处理的方法
		    	if (method.returnsVoid() && method.hasResultHandler()) {
		            executeWithResultHandler(sqlSession, args);
		            result = null;
		    	} else if (method.returnsMany) {
		    		result = executeForMany(sqlSession, args);
		    	} else if (method.returnsMap) {
		    		result = executeForMap(sqlSession, args);
		    	} else if (method.returnsCursor) {
		    		result = executeForCursor(sqlSession, args);
		    	} else {
		    		Object param = method.convertArgsToSqlCommandParam(args);
		    		result = sqlSession.selectOne(command.getName(), param);
		    	}
		    	break;
		    case FLUSH:
		        result = sqlSession.flushStatements();
		        break;
		    default:
		        throw new BindingException("Unknown execution method for: " + command.getName());		    
		}
	    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
	        throw new BindingException("Mapper method '" + command.getName() 
	            + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
	    }		
		return result;
	}
	
	// 将SQL语句执行的结果转换成为Mapper接口中对应的方法的返回值类型
	private Object rowCountResult(int rowCount) {
		final Object result;
	    if (method.returnsVoid()) {
	        result = null;
	    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
	        result = rowCount;
	    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
	        result = (long) rowCount;
	    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
	        result = rowCount > 0;
	    } else {
	        throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
	    }
	    return result;
	}
	
	// 查询多条数据，返回值是集合或数组
	private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
		List<E> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<E>selectList(command.getName(), param);
		}
		// 支持返回数组或集合
		if (!method.getReturnType().isAssignableFrom(result.getClass())) {
			if (method.getReturnType().isArray()) {
				return convertToArray(result);
			} else {
				return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
			}
		}
		return result;		
	}	
	
	// 如果Mapper接口中定义的方法准备使用ResultHandler处理查询结果集，则通过本方法进行处理
	private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
		// 获取SQL语句对应的MappedStatement对象，MappedStatement对象中记录了SQL语句相关信息
		MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
		// 当使用ResultHandler处理结果集时，必须指定ResultMap或ResultType
		if (void.class.equals(ms.getResultMaps().get(0).getType())) {
		      throw new BindingException("method " + command.getName() 
	          + " needs either a @ResultMap annotation, a @ResultType annotation," 
	          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");			
		}
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {   // 检测参数列表中是否有RowBounds类型的参数
			RowBounds rowBounds = method.extractRowBounds(args);
		    sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
		} else {
			sqlSession.select(command.getName(), param, method.extractResultHandler(args));
		}
	}
	
	private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
		Cursor<T> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<T>selectCursor(command.getName(), param);
		}
		return result;
	}
	
	// 将返回的列表转化为集合对象
	private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
		// 通过反射的方式创建集合对象
		Object collection = config.getObjectFactory().create(method.getReturnType());
		// 创建MetaObject对象
		MetaObject metaObject = config.newMetaObject(collection);
		metaObject.addAll(list);   // 实际上就是调用Collection.addAll()方法
		return collection;
	}
	
	// 将返回结果从列表转化为数组
	@SuppressWarnings("unchecked")
	private <E> E[] convertToArray(List<E> list) {
		E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
		array = list.toArray(array);
		return array;		
	}
	
	// Mapper接口中执行方法返回值类型为Map时使用本方法进行处理
	private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
		Map<K, V> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
		} else {
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
		}
		return result;
	}
	
	public static class ParamMap<V> extends HashMap<String, V> {
		
		private static final long serialVersionUID = 678379423176018349L;

		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
			}
			return super.get(key);
		}
	}	
	
	// 标记SQL命令的id和类型
	public static class SqlCommand {
		private final String name;         // SQL的名字，实际上就是mapper中命令标签的id属性
		private final SqlCommandType type; // SQL命令的类型，增删查改刷新未知
		
		public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
			// SQL语句的名称是由Mapper的接口与对应的方法名称组成
			String statementName = mapperInterface.getName() + "." + method.getName();
			MappedStatement ms = null;
			if (configuration.hasStatement(statementName)) {  // 检测是否有该名称的SQL语句
				// 从Configuration.mappedStatements集合中查找对应MappedStatement对象
				// MappedStatement对象中封装了SQL语句相关的信息，在MyBatis初始化时创建
				ms = configuration.getMappedStatement(statementName);
			} else if (!mapperInterface.equals(method.getDeclaringClass())) {
				// 如果指定方法是在父接口中定义的，则在此进行继承结构的处理
				// 从Configuration.mappedStatements集合中查找对应MappedStatement对象
				String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
				if (configuration.hasStatement(parentStatementName)) {
					ms = configuration.getMappedStatement(parentStatementName);
				}								
			}
			
			if (ms == null) {
				if (method.getAnnotation(Flush.class) != null) {   // 处理@Flush注解
					name = null;
					type = SqlCommandType.FLUSH;
				} else {
					throw new BindingException("Invalid bound statement (not found): " + statementName);
				}
			} else {
				name = ms.getId();   // 初始化name和type
				type = ms.getSqlCommandType();
				if (type == SqlCommandType.UNKNOW) {
					throw new BindingException("Unknown execution method for: " + name);
				}
			}
		}
		
		public String getName() {
			return name;
		}
		
		public SqlCommandType getType() {
			return type;
		}
	}
	
	// 封装Mapper接口中定义的方法的相关信息，包含了一下功能:
	// 1) 对方法返回值类型的判断
	// 2) 对方法参数中的ResultHandler和RowBounds类型参数进行解析
	// 3) 解析方法参数的参数名和位置数字索引
	public static class MethodSignature {
		
		private final boolean returnsMany;         // 返回类型是否为Collections类型或是数组类型
		private final boolean returnsMap;          // 返回类型是否为Map类型
		private final boolean returnsVoid;         // 返回类型是否为void
		private final boolean returnsCursor;       // 返回值是否为Cursor类型		
		private final Class<?> returnType;         // 返回值类型
		private final String mapKey;               // 如果返回值是Map，则该字段记录了作为key的列名
		private final Integer resultHandlerIndex;  // 用来标记该方法参数列表中ResultHandler类型参数的位置		
		private final Integer rowBoundsIndex;      // 用来标记该方法参数列表中RowBounds类型参数的位置		
		private final ParamNameResolver paramNameResolver;  // 该方法对应的ParamNameResolver对象
		
		public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
			Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
			if (resolvedReturnType instanceof Class<?>) {
				this.returnType = (Class<?>) resolvedReturnType;
			} else if (resolvedReturnType instanceof ParameterizedType) {
				this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
			} else {
				this.returnType = method.getReturnType();
			}
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
			this.returnsCursor = Cursor.class.equals(this.returnType);
			// 若MethodSignature对应的方法的返回值是Map且指定了@MapKey注解，则使用getMapKey()方法处理
			this.mapKey = getMapKey(method);
			this.returnsMap = (this.mapKey != null);
			// 初始化rowBoundsIndex和resultHandlerIndex字段
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
			// 创建ParamNameResolver对象
			this.paramNameResolver = new ParamNameResolver(configuration, method);
		}
		
		// 负责将args[]数组（用户传入的实参列表）转换成SQL语句对应的参数列表
		// 返回的Object实际上是一个Map，key为命名参数名，value为参数数字索引位置
	    public Object convertArgsToSqlCommandParam(Object[] args) {
	        return paramNameResolver.getNamedParams(args);
	    }		
	    
	    public boolean hasRowBounds() {
	        return rowBoundsIndex != null;
	    }

        public RowBounds extractRowBounds(Object[] args) {
	        return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
	    }

	    public boolean hasResultHandler() {
	        return resultHandlerIndex != null;
	    }

	    public ResultHandler extractResultHandler(Object[] args) {
	        return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
	    }
	    
	    public String getMapKey() {
	        return mapKey;
	    }

	    public Class<?> getReturnType() {
	        return returnType;
	    }

	    public boolean returnsMany() {
	        return returnsMany;
	    }

	    public boolean returnsMap() {
	        return returnsMap;
	    }

	    public boolean returnsVoid() {
	        return returnsVoid;
	    }

	    public boolean returnsCursor() {
	        return returnsCursor;
	    }	    
		
		// 查找指定类型的参数在参数列表中的位置，主要是为查找RowBounds、ResultHandler类型的参数服务
		// 对于每个Mapper方法来说，参数列表中RowBounds或ResultHandler类型参数只允许出现一次
		private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
			Integer index = null;
			final Class<?>[] argTypes = method.getParameterTypes();
			for (int i = 0; i < argTypes.length; i++) {
				if (paramType.isAssignableFrom(argTypes[i])) {
					if (index == null) {  // 记录paramType类型参数在参数列表中的位置索引
						index = i;
					} else {  // RowBounds和ResultHandler类型的参数只能有一个，且不能重复出现
						throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
					}
				}
			}
			return index;
		}		
		
		// 若MethodSignature对应方法的返回值是Map且指定了@MapKey注解，则使用getMapKey()方法处理
		private String getMapKey(Method method) {
			String mapKey = null;
			if (Map.class.isAssignableFrom(method.getReturnType())) {
				final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
				if (mapKeyAnnotation != null) {
					mapKey = mapKeyAnnotation.value();
				}
			}
			return mapKey;
		}
	}
}
