package org.apache.ibatis.mapping;

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 记录了动态SQL经过SqlNode.apply()解析之后的含 "#{}" 占位符的动态SQL中的 "#{}" 占位符中的参数属性
 *
 */
public class ParameterMapping {
	private Configuration configuration;		// mybatis全局配置
	
	private String property;					// 传进来的属性name
	private ParameterMode mode;                 // 输入参数还是输出参数
	private Class<?> javaType = Object.class;   // 参数的Java类型
	private JdbcType jdbcType;                  // 参数的JDBC类型
	private Integer numericScale;               // 浮点参数的精度
	private TypeHandler<?> typeHandler;         // 参数对应的TypeHandler对象
	private String resultMapId;                 // 参数对应的ResultMap的Id
	private String jdbcTypeName;                // 参数的jdbcTypeName属性
	private String expression;                  
	
	private ParameterMapping() {}

	public static class Builder {
		private ParameterMapping parameterMapping = new ParameterMapping();
		
		public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.typeHandler = typeHandler;
			parameterMapping.mode = ParameterMode.IN;
		}
		
		public Builder(Configuration configuration, String property, Class<?> javaType) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.javaType = javaType;
			parameterMapping.mode = ParameterMode.IN;
		}
		
		public Builder mode(ParameterMode mode) {
			parameterMapping.mode = mode;
			return this;
		}
		
		public Builder javaType(Class<?> javaType) {
			parameterMapping.javaType = javaType;
			return this;
		}
		
		public Builder jdbcType(JdbcType jdbcType) {
			parameterMapping.jdbcType = jdbcType;
			return this;
		}
		
		public Builder numericScale(Integer numericScale) {
			parameterMapping.numericScale = numericScale;
			return this;
		}
		
		public Builder typeHandler(TypeHandler<?> typeHandler) {
			parameterMapping.typeHandler = typeHandler;
			return this;
		}
		
		public Builder resultMapId(String resultMapId) {
			parameterMapping.resultMapId = resultMapId;
			return this;
		}
		
		public Builder jdbcTypeName(String jdbcTypeName) {
			parameterMapping.jdbcTypeName = jdbcTypeName;
			return this;
		}
		
		public Builder expression(String expression) {
			parameterMapping.expression = expression;
			return this;
		}
		
		public ParameterMapping build() {
			resolveTypeHandler();
			validate();
			return parameterMapping;
		}
		
		// 解析要使用哪个类型转换器
		private void resolveTypeHandler() {
			if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
				Configuration configuration = parameterMapping.configuration;
				TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
				parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
			}
		}
		
		private void validate() {
		    if (ResultSet.class.equals(parameterMapping.javaType)) {
		        if (parameterMapping.resultMapId == null) { 
		            throw new IllegalStateException("Missing resultmap in property '"  
		                + parameterMapping.property + "'.  " 
		                + "Parameters of type java.sql.ResultSet require a resultmap.");
		        }            
		    } else {
		        if (parameterMapping.typeHandler == null) { 
		            throw new IllegalStateException("Type handler was null on parameter mapping for property '"
		              + parameterMapping.property + "'. It was either not specified and/or could not be found for the javaType ("
		              + parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType + ") combination.");
		        }
		    }
		}
	}
	
	public String getProperty() {
		return property;
	}

	public ParameterMode getMode() {
		return mode;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public JdbcType getJdbcType() {
		return jdbcType;
	}

	public Integer getNumericScale() {
		return numericScale;
	}

	public TypeHandler<?> getTypeHandler() {
		return typeHandler;
	}

	public String getResultMapId() {
		return resultMapId;
	}

	public String getJdbcTypeName() {
		return jdbcTypeName;
	}

	public String getExpression() {
		return expression;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ParameterMapping{");
		//sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
		sb.append("property='").append(property).append('\'');
		sb.append(", mode=").append(mode);
		sb.append(", javaType=").append(javaType);
		sb.append(", jdbcType=").append(jdbcType);
		sb.append(", numericScale=").append(numericScale);
		//sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
		sb.append(", resultMapId='").append(resultMapId).append('\'');
		sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
		sb.append(", expression='").append(expression).append('\'');
		sb.append('}');
	    return sb.toString();
	}
}
