package org.apache.ibatis.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

/**
 * SqlSource处理所有动态内容之后的一个实际使用的SQL字符串
 * 这个SQL可能含有SQL占位符 "?" 和一个参数映射的列表，列表中包含了每个占位参数的信息（至少可以根据输入对象的参数名来从列表中读取参数值）
 *
 * 列表参数映射信息也可能是动态SQL中定义提供的
 */
public class BoundSql {
	
	// 该字段记录了SQL语句，该SQL语句中可能含有"?"占位符
	private String sql;	
	
	// SQL中的参数属性集合，ParameterMapping的集合
	private List<ParameterMapping> parameterMappings;
	
	// 客户端执行SQL时传入的实际参数
	private Object parameterObject;
	
	// 空的HashMap集合，之后会赋值DynamicContext.bindings集合中的内容
	private Map<String, Object> additionalParameters;
	
	// additionalParameters集合对应的Metaobject对象
	private MetaObject metaParameters;
	
	public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings,
			Object parameterObject) {
		this.sql = sql;
		this.parameterMappings = parameterMappings;
		this.parameterObject = parameterObject;
		this.additionalParameters = new HashMap<String, Object>();
		this.metaParameters = configuration.newMetaObject(additionalParameters);
	}

	public String getSql() {
		return sql;
	}

	public List<ParameterMapping> getParameterMappings() {
		return parameterMappings;
	}

	public Object getParameterObject() {
		return parameterObject;
	}
	
	// 解析形如 "order['key']" 的表达式，判断additionalParameters集合中是否含有这个key
	public boolean hasAdditionalParameter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		String indexedName = prop.getIndexedName();
		return additionalParameters.containsKey(indexedName);
	}

	// 利用MetaObject对象给参数设值，即使不知道参数的数据结构（对象还是Map或者数据列表），只要给出准确的参数名即可
	public void setAdditionalParameter(String name, Object value) {
		metaParameters.setValue(name, value);
	}

	// 利用MetaObject对象获取参数值，即使不知道参数的数据结构（对象还是Map或者数据列表），只要给出准确的参数名即可
	public Object getAdditionalParameter(String name) {
		return metaParameters.getValue(name);
	}
}
