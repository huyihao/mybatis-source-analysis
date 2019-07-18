package org.apache.ibatis.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * TypeHandlerRegistry提供了以下功能:
 * 1) register系列重载方法，注册Java类型与JDBC类型、类型转换器之间的映射关系
 * 2) hasTypeHandler系列重载方法，支持通过Java类型、JDBC类型、类型应用TypeReference判断是否有对应的类型转换器的功能
 * 3) getTypeHandler系列重载方法，支持通过Java类型、JDBC类型、类型应用TypeReference获取对应的转换器的功能
 */
public class TypeHandlerRegistry {
	// 核心字段
	// 记录JdbcType与TypeHandler之间的对应关系，其中JdbcType是一个枚举类型，它定义对应的JDBC类型
	// 该集合主要用于从结果集读取数据时，将数据从Jdbc类型转换成Java类型
	private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<JdbcType, TypeHandler<?>>(JdbcType.class);
	
	// 记录了Java类型向指定的JdbcType转换时，需要使用的TypeHandler对象。例如: Java类型中的String可能
	// 转换成数据库的char、varchar等多种类型，所以存在一对多的关系，所以值要用Map来存储
	private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<Type, Map<JdbcType, TypeHandler<?>>>();
	
	// 记录了全部TypeHandler的类型以及该类型对应的TypeHandler对象
	private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();
	
	// 未知类型对象的TypeHandler
	private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknowTypeHandler(this);
	
	public TypeHandlerRegistry() {
		register(Boolean.class, new BooleanTypeHandler());     // TYPE_HANDLER_MAP: <(Type) Boolean.class, <null, BooleanTypeHandler>>，可以把BooleanTypeHandler看成是处理Boolean.class类型的默认类型转换器
		register(boolean.class, new BooleanTypeHandler());
		register(JdbcType.BOOLEAN, new BooleanTypeHandler());  // JDBC_TYPE_HANDLER_MAP: <JdbcType.BOOLEAN, BooleanTypeHandler>
		register(JdbcType.BIT, new BooleanTypeHandler());
		
	    register(Byte.class, new ByteTypeHandler());
	    register(byte.class, new ByteTypeHandler());
	    register(JdbcType.TINYINT, new ByteTypeHandler());		
		
	    register(Short.class, new ShortTypeHandler());
	    register(short.class, new ShortTypeHandler());
	    register(JdbcType.SMALLINT, new ShortTypeHandler());
	    
	    register(Integer.class, new IntegerTypeHandler());
	    register(int.class, new IntegerTypeHandler());
	    register(JdbcType.INTEGER, new IntegerTypeHandler());

	    register(Long.class, new LongTypeHandler());
	    register(long.class, new LongTypeHandler());

	    register(Float.class, new FloatTypeHandler());
	    register(float.class, new FloatTypeHandler());
	    register(JdbcType.FLOAT, new FloatTypeHandler());

	    register(Double.class, new DoubleTypeHandler());
	    register(double.class, new DoubleTypeHandler());
	    register(JdbcType.DOUBLE, new DoubleTypeHandler());
	    
	    register(Reader.class, new ClobReaderTypeHandler());
	    register(String.class, new StringTypeHandler());
	    
	    // Java String类型可以转化为JDBC的(CHAR, CLOB, VARCHAR, LONGVARCHAR, NVARCHAR, NCHAR, NCLOB)
	    register(String.class, JdbcType.CHAR, new StringTypeHandler());  // TYPE_HANDLER_MAP: <(Type) String.class, <JdbcType.CHAR, StringTypeHandler>>
	    register(String.class, JdbcType.CLOB, new ClobTypeHandler());
	    register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
	    register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
	    register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
	    register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
	    register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
	    register(JdbcType.CHAR, new StringTypeHandler());
	    register(JdbcType.VARCHAR, new StringTypeHandler());
	    register(JdbcType.CLOB, new ClobTypeHandler());
	    register(JdbcType.LONGVARCHAR, new ClobTypeHandler());
	    register(JdbcType.NVARCHAR, new NStringTypeHandler());
	    register(JdbcType.NCHAR, new NStringTypeHandler());
	    register(JdbcType.NCLOB, new NClobTypeHandler());
	    
	    register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
	    register(JdbcType.ARRAY, new ArrayTypeHandler());

	    register(BigInteger.class, new BigIntegerTypeHandler());
	    register(JdbcType.BIGINT, new LongTypeHandler());

	    register(BigDecimal.class, new BigDecimalTypeHandler());
	    register(JdbcType.REAL, new BigDecimalTypeHandler());
	    register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
	    register(JdbcType.NUMERIC, new BigDecimalTypeHandler());	   
	    
	    register(InputStream.class, new BlobInputStreamTypeHandler());
	    register(Byte[].class, new ByteObjectArrayTypeHandler());
	    register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
	    register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
	    register(byte[].class, new ByteArrayTypeHandler());
	    register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
	    register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
	    register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
	    register(JdbcType.BLOB, new BlobTypeHandler());	 
	    
	    register(Object.class, UNKNOWN_TYPE_HANDLER);
	    register(Object.class, JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
	    register(JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
	    
	    register(Date.class, new DateTypeHandler());
	    register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
	    register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
	    register(JdbcType.TIMESTAMP, new DateTypeHandler());
	    register(JdbcType.DATE, new DateOnlyTypeHandler());
	    register(JdbcType.TIME, new TimeOnlyTypeHandler());

	    register(java.sql.Date.class, new SqlDateTypeHandler());
	    register(java.sql.Time.class, new SqlTimeTypeHandler());
	    register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());	    
	    
	    // 一般情况下够用了
	    // 引入jar包时使用, download from https://mvnrepository.com/artifact/org.mybatis/mybatis-typehandlers-jsr310
	    // mybatis-typehandlers-jsr310
	    try {
	      // since 1.0.0
	      register("java.time.Instant", "org.apache.ibatis.type.InstantTypeHandler");
	      register("java.time.LocalDateTime", "org.apache.ibatis.type.LocalDateTimeTypeHandler");
	      register("java.time.LocalDate", "org.apache.ibatis.type.LocalDateTypeHandler");
	      register("java.time.LocalTime", "org.apache.ibatis.type.LocalTimeTypeHandler");
	      register("java.time.OffsetDateTime", "org.apache.ibatis.type.OffsetDateTimeTypeHandler");
	      register("java.time.OffsetTime", "org.apache.ibatis.type.OffsetTimeTypeHandler");
	      register("java.time.ZonedDateTime", "org.apache.ibatis.type.ZonedDateTimeTypeHandler");
	      // since 1.0.1
	      register("java.time.Month", "org.apache.ibatis.type.MonthTypeHandler");
	      register("java.time.Year", "org.apache.ibatis.type.YearTypeHandler");

	    } catch (ClassNotFoundException e) {
	      // no JSR-310 handlers
	    }	    
	    
	    register(Character.class, new CharacterTypeHandler());
	    register(char.class, new CharacterTypeHandler());
	}
	
	// hasTypeHandler的重载方法
	// [start]
	public boolean hasTypeHandler(Class<?> javaType) {
	    return hasTypeHandler(javaType, null);
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
	    return hasTypeHandler(javaTypeReference, null);
	}

    public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
	    return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
	    return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
	}	
	// [end]
	
	// getTypeHandler的重载方法
	// [start]
	public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
	    return ALL_TYPE_HANDLERS_MAP.get(handlerType);
	}	
	
	public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
	    return JDBC_TYPE_HANDLER_MAP.get(jdbcType);
	}	
	
	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
	    return getTypeHandler(javaTypeReference, null);
	}	
	
	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
	    return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
	}	
	
	public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
		return getTypeHandler((Type) type, null);
	}
	
	public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
		return getTypeHandler((Type) type, jdbcType);
	}	
	
	// 根据Java类型和JdbcType获取匹配的TypeHandler
	@SuppressWarnings("unchecked")
	private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
		// 查找Java类型对应的TypeHandler集合
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
		TypeHandler<?> handler = null;
		if (jdbcHandlerMap != null) {
			// 根据JdbcType类型查找TypeHandler对象
			handler = jdbcHandlerMap.get(jdbcType);
			if (handler == null) {   // eg: <(Type) Boolean.class, <null, BooleanTypeHandler>>
				handler = jdbcHandlerMap.get(null);    // 查找不到的话就找默认的TypeHandler
			}
			if (handler == null) {   // 没有找到匹配jdbcType的转换器，也没注册默认转换器
				// 如果jbdcHandlerMap只注册了一个TypeHandler，则使用此TypeHandler对象				
				handler = pickSoleHandler(jdbcHandlerMap);
			}
		}
		// 如果根据Java类型找不到TypeHandler，有可能是枚举类型
		if (handler == null && type != null && type instanceof Class && Enum.class.isAssignableFrom((Class<?>) type)) {
			handler = new EnumTypeHandler((Class<?>) type);
		}
		return (TypeHandler<T>) handler;
	}
	
	// 找到唯一的类型转换器，假如一个Java Type没有默认转换器，也没有jdbcType对应的转换器，那么除非它只剩下一个对应其他jdbctype的转换器，否则返回null
	private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
		TypeHandler<?> soleHandler = null;
		for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
			if (soleHandler == null) {
				soleHandler = handler;
			} else if (!handler.getClass().equals(soleHandler.getClass())) {
				return null;  // 注册了多个类型转换器
			}
 		}
		return soleHandler;
	}
	// [end]
	
	public TypeHandler<Object> getUnknownTypeHandler() {
	    return UNKNOWN_TYPE_HANDLER;
	}
	
	// register的重载方法
	// [start]
	public void register(JdbcType jdbcType, TypeHandler<?> handler) {
		JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
	}
	
	// 依赖register(Class, JdbcType, TypeHandler)的register重载方法
	// [start]
	// (java class, jdbc type, handler class)
	public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
		register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
	}
	
	// (java class, jdbc type, handler)
	public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
		register((Type) type, jdbcType, handler);
	}
	// [end]
	
	public void register(String packageName) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
		Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
		for (Class<?> type : handlerSet) {
			if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
				register(type);
			}
		}
	}
	
	// (handler class)  
	public void register(Class<?> typeHandlerClass) {
		boolean mappedTypeFound = false;
		MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
		if (mappedTypes != null) {
			for (Class<?> javaTypeClass : mappedTypes.value()) {
				register(javaTypeClass, typeHandlerClass);
				mappedTypeFound = true;
			}
		}
		if (!mappedTypeFound) {
			register(getInstance(null, typeHandlerClass));
		}
	}
	
	// (String, String)   这里必须要用类的带包名的全限定名
	public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
		register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
	}
	
	// (java class, handler class)
	public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
	}
	
	// (handler)
	@SuppressWarnings("unchecked")
	public <T> void register(TypeHandler<T> typeHandler) {
		// 只有一个TypeHandler会尝试去获取它的注解，拿到javatype和jdbctype
		boolean mappedTypeFound = false;
		MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
		if (mappedTypes != null) {
			for (Class<?> handledType : mappedTypes.value()) {
				register(handledType, typeHandler);
				mappedTypeFound = true;
			}
		}
		if (!mappedTypeFound && typeHandler instanceof TypeReference) {
			try {
				TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
				register(typeReference.getRawType(), typeHandler);
				mappedTypeFound = true;
			} catch (Throwable t) {
				// maybe users define the TypeReference with a different type and are not assignable, so just ignore it
			}
		}
		
		if (!mappedTypeFound) {
			register((Class<T>) null, typeHandler);
		}
	}
	
	// (java class, handler)
	public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
		register((Type) javaType, typeHandler);
	}
	
	// (java typeReference, handler) 
	public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
		register(javaTypeReference.getRawType(), handler);
	}
	
	// (jdbc type, handler)
	private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
		// 根据TypeHandler的@MappedJdbcTypes注解去获取JdbcType
		// MyBatis自带的TypeHandler没使用这个注解，该注解用于自定义的TypeHandler使用，并且支持注解多个JdbcType
		MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
		if (mappedJdbcTypes != null) {
			for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
				register(javaType, handledJdbcType, typeHandler);
			}
			if (mappedJdbcTypes.includeNullJdbcType()) {
				register(javaType, null, typeHandler);
			}
		} else {
			register(javaType, null, typeHandler);
		}
	}
	
	// (java type, jdbc type, handler) 
	private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
		if (javaType != null) {
			Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
			if (map == null) {
				map = new HashMap<JdbcType, TypeHandler<?>>();
				TYPE_HANDLER_MAP.put(javaType, map);
			}
			map.put(jdbcType, handler);
		}
		ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
	}
	// [end]
	
	@SuppressWarnings("unchecked")
	public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		if (javaTypeClass != null) {
			try {
				Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
				return (TypeHandler<T>) c.newInstance(javaTypeClass);
			} catch (NoSuchMethodException ignored) {
				// ignored
			} catch (Exception e) {
				throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
			}
		} 
		try {
			Constructor<?> c = typeHandlerClass.getConstructor();
			return (TypeHandler<T>) c.newInstance();
		} catch (Exception e) {
			throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
		}
	}	
}