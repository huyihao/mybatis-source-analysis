package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * 记录动态SQL语句解析结果的上下文
 *
 */
public class DynamicContext {
	
	public static final String PARAMETER_OBJECT_KEY = "_parameter";
	public static final String DATABASE_ID_KEY = "_databaseId";
	
	private final ContextMap bindings;
	private final StringBuilder sqlBuilder = new StringBuilder();  // 在SqlNode解析动态SQL时，会将解析后的SQL语句片段添加到该属性中保存，最终拼凑出一条完整的SQL语句
	private int uniqueNumber = 0;
	
	public DynamicContext(Configuration configuration, Object parameterObject) {
		if (parameterObject != null && !(parameterObject instanceof Map)) {
			// 对于非Map类型的参数，创建对应的MetaObject对象，并拼装成ContextMap对象
			MetaObject metaObject = configuration.newMetaObject(parameterObject);
			bindings = new ContextMap(metaObject);   // 初始化bindings集合
		} else {
			bindings = new ContextMap(null);
		}
		bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
		bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
	}
	
	public Map<String, Object> getBindings() {
		return bindings;
	}
	
	public void bind(String name, Object value) {
		bindings.put(name, value);
	}
	
	public void appendSql(String sql) {
		sqlBuilder.append(sql);
		sqlBuilder.append(" ");
	}
	
	public String getSql() {
		return sqlBuilder.toString().trim();
	}
	
	public int getUniqueNumber() {
		return uniqueNumber++;
	}
	
	static class ContextMap extends HashMap<String, Object> {
		private static final long serialVersionUID = 548537755878749476L;
		
		private MetaObject parameterMetaObject;	  // 将用户传入的参数封装成了MetaObject对象
		public ContextMap(MetaObject parameterMetaObject) {
			this.parameterMetaObject = parameterMetaObject;
		}
		@Override
		public Object get(Object key) {
			String strKey = (String) key;
			// 如果ContextMap中已经包含了该key，则直接返回
			if (super.containsKey(strKey)) {
				return super.get(strKey);
			}
			// 从运行时参数中查找对应树形
			if (parameterMetaObject != null) {
				return parameterMetaObject.getValue(strKey);
			}
			return null;
		}
	}
}
