package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * SqlSource对象的构造器
 */
public class SqlSourceBuilder extends BaseBuilder {

	private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";
	
	public SqlSourceBuilder(Configuration configuration) {
		super(configuration);
	}
	
	/**
	 * 构建SqlSource对象
	 * 
	 * @param originalSql			第一个参数是经过SqlNode.apply()方法处理之后的SQL语句
	 * @param parameterType         第二个参数是用户传入的实参类型
	 * @param additionalParameters  第三个参数记录了形参与实参的对应关系，其实就是经过SqlNode.apply()方法处理后的DynamicContext.bindings集合
	 * @return
	 */
	public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
	    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
	    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
	    String sql = parser.parse(originalSql);
	    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());		
	}
	
	private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {
		// 用于记录解析得到的ParameterMapping集合
		private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
		private Class<?> parameterType;		  // 参数类型
		private MetaObject metaParameters;    // DynamicContext.bindings集合对应的MetaObject对象
		
		public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
			super(configuration);
			this.parameterType = parameterType;
			this.metaParameters = configuration.newMetaObject(additionalParameters);
		}

		public List<ParameterMapping> getParameterMappings() {
			return parameterMappings;
		}

		/**
		 * 构建parameterMapping对象，将占位符 "#{}" 包括里面的内容替换成 "?"
		 * 
		 * eg:
		 * 	  select * from Blog B where id in(?, ?)
		 * [第1个?替换的占位内容] "#{__frch_item_0, javaType=int}"   => ParameterMapping paraMap1 = {property='__frch_item_0', mode=IN, javaType=class java.lang.Integer, ...}
		 * [第2个?替换的占位内容] "#{__frch_item_1, javaType=int}"   => ParameterMapping paraMap2 = {property='__frch_item_0', mode=IN, javaType=class java.lang.Integer, ...}
		 * 
		 * List<ParameterMapping> parameterMappings = {paraMap1, paraMap2}
		 */
		@Override
		public String handleToken(String content) {
		     parameterMappings.add(buildParameterMapping(content));
		     return "?";
		}
		
		private ParameterMapping buildParameterMapping(String content) {
			// 解析参数的属性，并形成Map。例如 "#{__frc_item_0, javaType=int, jdbcType=NUMERIC, typeHandler=MyTypeHandler}"
			// 这个占位符，它会被解析成如下Map:
			// {"property" -> "__frc_item_0", "javaType" -> "int", "jdbcType" -> "NUMERIC", "typeHandler" -> "MyTypeHandler"}
			Map<String, String> propertiesMap = parseParameterMapping(content);
			String property = propertiesMap.get("property");    // 获取参数名称
			Class<?> propertyType;
			
			// 确定参数的javaType属性
			if (metaParameters.hasGetter(property)) {           // 通过反射获取参数类型
				propertyType = metaParameters.getGetterType(property);
			} else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {  // 通过判断参数类型是否存在类型转换器获取参数类型
		        propertyType = parameterType;                    
		    } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
		        propertyType = java.sql.ResultSet.class;
		    } else if (property != null) {
		        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
		        if (metaClass.hasGetter(property)) {
		        	propertyType = metaClass.getGetterType(property);
		        } else {
		        	propertyType = Object.class;
		        }
		    } else {
		        propertyType = Object.class;
		    }
			
			// 创建ParameterMapping的建造者，并设置ParameterMapping相关配置
			ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
			Class<?> javaType = propertyType;
			String typeHandlerAlias = null;
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if ("javaType".equals(name)) {
					javaType = resolveClass(value);
					builder.javaType(javaType);
				} else if ("jdbcType".equals(name)) {
					builder.jdbcType(resolveJdbcType(value));
				} else if ("mode".equals(name)) {
					builder.mode(resolveParameterMode(value));
				} else if ("numericScale".equals(name)) {
					builder.numericScale(Integer.valueOf(value));
				} else if ("resultMap".equals(name)) {
					builder.resultMapId(value);
				} else if ("typeHandler".equals(name)) {
					typeHandlerAlias = value;
				} else if ("jdbcTypeName".equals(name)) {
					builder.jdbcTypeName(value);
				} else if ("property".equals(name)) {
				} else if ("expression".equals(name)) {
					throw new BuilderException("Expression based parameters are not supported yet");
				} else {
					throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
							+ "}.  Valid properties are " + parameterProperties);
				}
			}
			if (typeHandlerAlias != null) {   // 获取TypeHandler对象
				builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
			}
			// 创建ParameterMapping对象，注意，如果没有指定的TypeHandler，则会在这里的build()方法中
			// 根据javaType和jdbcType从TypeHandlerRegistry中获取对应的TypeHandler对象
			return builder.build();		
		}
		
		private Map<String, String> parseParameterMapping(String content) {
			try {
				return new ParameterExpression(content);
			} catch (BuilderException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new BuilderException("Parsing error was found in mapping #{" + content
						+ "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
			}
		}
	}
}
