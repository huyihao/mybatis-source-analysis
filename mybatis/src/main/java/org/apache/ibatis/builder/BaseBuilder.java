package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 基础建造者抽象类，扮演着建造者接口的角色
 */
public abstract class BaseBuilder {
	/**
	 * Configuration是Mybatis初始化过程的核心对象，Mybatis中几乎全部的配置信息都会保存到
	 * Configuration对象中。Configuration对象是在MyBatis初始化过程中创建且全局唯一的，
	 * 也有人说它是一个 “All-In-One” 配置对象
	 */
	protected final Configuration configuration;
	
	/**
	 * 在mybatis-config.xml配置文件中可以使用<typeAliases>标签定义别名，这些定义的别名
	 * 都会记录在该TypeAliasRegistry对象中
	 */
	protected final TypeAliasRegistry typeAliasRegistry;
	
	/**
	 * 在mybatis-config.xml配置文件中可以使用<typehandlers>标签添加自定义TypeHandler，
	 * 完成指定数据库类型与Java类型的转换，这些TypeHandler都会记录在TypeHandlerRegistry中。
	 */
	protected final TypeHandlerRegistry typeHandlerRegistry;
	
	public BaseBuilder(Configuration configuration) {
		this.configuration = configuration;
		this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
		this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
	}
	
	public Configuration getConfiguration() {
		return configuration;
	}
	
	protected Pattern parseExpression(String regex, String defaultValue) {
		return Pattern.compile(regex == null ? defaultValue : regex);
	}
	
	protected Boolean booleanValueOf(String value, Boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf(value);
	}
	
	protected Integer integerValueOf(String value, Integer defaultValue) {
		return value == null ? defaultValue : Integer.valueOf(value);
	}
	
	protected Set<String> stringSetValueOf(String value, String defalutValue) {
		value = (value == null ? defalutValue : value);
		return new HashSet<String>(Arrays.asList(value.split(",")));
	}
	
	// 这里的alias是JdbcType定义的枚举名
	protected JdbcType resolveJdbcType(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return JdbcType.valueOf(alias);	
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
		}
	}
	
	protected ResultSetType resolveResultSetType(String alias) {
	    if (alias == null) {
	    	return null;
		}
		try {
		    return ResultSetType.valueOf(alias);
		} catch (IllegalArgumentException e) {
		    throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
		}
	}

	protected ParameterMode resolveParameterMode(String alias) {
	    if (alias == null) {
	    	return null;
		}
		try {
		    return ParameterMode.valueOf(alias);
		} catch (IllegalArgumentException e) {
		    throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
		}
	}
	
	protected Object createInstance(String alias) {
		Class<?> clazz = resolveClass(alias);
		if (clazz == null) {
			return null;
		}
		try {
			return resolveClass(alias).newInstance();
		} catch (Exception e) {
			throw new BuilderException("Error creating instance. Cause: " + e, e);
		}
	}
	
	protected Class<?> resolveClass(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return resolveAlias(alias);
		} catch (Exception e) {
			throw new BuilderException("Error resolving class, Cause: " + e, e);
		}
	}
	
	protected Class<?> resolveAlias(String alias) {
		return typeAliasRegistry.resolveAlias(alias);
	}
	
	// 根据Java类型和TypeHandler别名解析得到TypeHandler
	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
		if (typeHandlerAlias == null) {
			return null;
		}
		Class<?> type = resolveClass(typeHandlerAlias);
		if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
			throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
		return resolveTypeHandler(javaType, typeHandlerType);
	}
	
	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
		if (typeHandlerType == null) {
			return null;
		}
		// 先去ALL_TYPE_HANDLERS_MAP中找，如果找不到则这个TypeHandler可能没注册，通过反射创建其TypaHandler对象
		TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
		if (handler == null) {
			handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
		}
		return handler;
	}
}
