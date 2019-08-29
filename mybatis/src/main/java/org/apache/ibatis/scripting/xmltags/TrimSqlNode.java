package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.Configuration;

/**
 * 对应动态SQL中的<trim>节点
 *
 * [示例]
 * <trim prefix="WHERE" prefixOverrides="AND |OR " suffixOverrides="aa">
 *    <if>and xxx</if>
 *    <if>and yyy aa</if>
 * </trim>
 * 
 * [效果]
 *     去除最开头的 AND或OR 前缀，去除掉最结尾的aa，并补上WHERE前缀
 * eg: where xxx and yyy
 */
public class TrimSqlNode implements SqlNode {
	private SqlNode contents;                // <trim>节点的子节点
	private String prefix;                   // 记录了前缀字符串（为<trim>节点包裹的SQL语句添加的前缀）
	private String suffix;                   // 记录了后缀字符串（为<trim>节点包裹的SQL语句添加的后缀）
	private List<String> prefixesToOverride; // 如果<trim>节点包裹的SQL语句时空语句（经常出现在if判断为否的情况下），删除指定的前缀，如"and"
	private List<String> suffixesToOverride; // 如果<trim>节点包裹的SQL语句时空语句（经常出现在if判断为否的情况下），删除指定的后缀，如","
	private Configuration configuration;
	
	public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixedToOverride, String suffix, String suffixesToOverride) {
		this(configuration, contents, prefix, parseOverrides(prefixedToOverride), suffix, parseOverrides(suffixesToOverride));
	}
	
	public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixedToOverride) {
		this.contents = contents;
		this.prefix = prefix;
		this.suffix = suffix;
		this.prefixesToOverride = prefixesToOverride;
		this.suffixesToOverride = suffixedToOverride;
		this.configuration = configuration;
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
		boolean result = contents.apply(filteredDynamicContext);
		filteredDynamicContext.applyAll();
		return result;
	}
	
	// 将形如 "aa | bb | cc" 的字符串解析一个列表 { AA, BB, CC }
	private static List<String> parseOverrides(String overrides) {
		if (overrides != null) {
			final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
			final List<String> list = new ArrayList<String>(parser.countTokens());
			while (parser.hasMoreTokens()) {
				list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
			}
			return list;
		}
		return Collections.emptyList();
	}
	
	// 继承了DynamicContext，封装了DynamicContext对象
	private class FilteredDynamicContext extends DynamicContext {
		private DynamicContext delegate;	// 底层封装的DynamicContext对象
		
		// 是否已经处理过前缀和后缀，初始值都为false
		private boolean prefixApplied;
		private boolean suffixApplied;
		
		// 用于记录子节点解析后的结果，FilteredDynamicContext.appendSql() 方法会向
		// 该字段添加解析结果，而不是调用delegate.appendSql() 方法
		private StringBuilder sqlBuffer;
		
		public FilteredDynamicContext(DynamicContext delegate) {
			super(configuration, null);
			this.delegate = delegate;
			this.prefixApplied = false;
			this.suffixApplied = false;
			this.sqlBuffer = new StringBuilder();
		}
		
		public void applyAll() {
			// 获取子节点解析后的结果，并全部转换为大写
			sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
			String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
			if (trimmedUppercaseSql.length() > 0) {
				applyPrefix(sqlBuffer, trimmedUppercaseSql);   // 处理前缀
				applySuffix(sqlBuffer, trimmedUppercaseSql);   // 处理后缀
			}
			delegate.appendSql(sqlBuffer.toString());
		}
		
		@Override
		public Map<String, Object> getBindings() {
			return delegate.getBindings();
		}

		@Override
		public void bind(String name, Object value) {
			delegate.bind(name, value);
		}

		@Override
		public int getUniqueNumber() {
			return delegate.getUniqueNumber();
		}

		@Override
		public void appendSql(String sql) {
			sqlBuffer.append(sql);
		}

		@Override
		public String getSql() {
			return delegate.getSql();
		}
	    
	    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
	    	if (!prefixApplied) {      // 判断是否已处理过前缀
	    		prefixApplied = true;
	    		if (prefixesToOverride != null) {
	    			// 遍历prefixesToOverride集合，如果以prefixesToOverride中某项开头，则将该项从SQL语句开头删除掉
	    			for (String toRemove : prefixesToOverride) {
	    				if (trimmedUppercaseSql.startsWith(toRemove)) {
	    					sql.delete(0, toRemove.trim().length());
	    					break;
	    				}
	    			}
	    		}
	    		if (prefix != null) {
	    			sql.insert(0, " ");
	    			sql.insert(0, prefix);
	    		}
	    	}
	    }
	    
	    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
	    	if (!suffixApplied) {
	    		suffixApplied = true;
	    		if (suffixesToOverride != null) {
	    			for (String toRemove : suffixesToOverride) {
	    				if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
	    					int start = sql.length() - toRemove.trim().length();
	    					int end = sql.length();
	    					sql.delete(start, end);
	    					break;
	    				}
	    			}
	    		}
	    		if (suffix != null) {
	    			sql.append(" ");
	    			sql.append(suffix);
	    		}
	    	}
	    }
	}
}
