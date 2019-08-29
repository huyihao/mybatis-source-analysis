package org.apache.ibatis.scripting.xmltags;

import java.util.Map;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.session.Configuration;

/**
 * 处理动态SQL中的 <foreach> 节点
 * 
 * [使用实例]
 * <select id="selectPostIn" resultType="domain.blog.Post">
 * 	SELECT *
 * 	FROM POST P
 * 	WHERE ID in
 * 	<foreach item="item" index="index" collection="list"
 *     open="(" separator="," close=")">
 *       #{item}
 * 	</foreach>
 * </select>
 */
public class ForEachSqlNode implements SqlNode {   
	public static final String ITEM_PREFIX = "__frch_";   // 占位参数处理固定前缀
	
	private ExpressionEvaluator evaluator;
	private String collectionExpression;	// 迭代的集合表达式
	private SqlNode contents;				// 记录了该ForEachSqlNode节点的子节点
	private String open;					// 在循环开始前要添加的字符串
	private String separator;               // 在循环过程中，每项之间的分隔符
	private String close;                   // 在循环结束后要添加的字符串
	// index是当前迭代的次数，item是每次迭代的元素。若迭代集合是Map，则index是键，item是值
	private String item;                    
	private String index;                   
	private Configuration configuration;    // 配置对象
	
	public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index,
						  String item, String open, String close, String separator) {
		this.evaluator = new ExpressionEvaluator();
		this.collectionExpression = collectionExpression;
		this.contents = contents;
		this.open = open;
		this.close = close;
		this.separator = separator;
		this.index = index;
		this.item = item;
		this.configuration = configuration;
	}

	@Override
	public boolean apply(DynamicContext context) {
		// 1.解析集合表达式，获取实际参数
		Map<String, Object> bindings = context.getBindings();
		final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
		if (!iterable.iterator().hasNext()) {
			
		}
		
		// 2.循环开始之前，添加open字段指定的字符串
		applyOpen(context);
		
		// 3.遍历集合，根据遍历的位置和是否指定分隔符。用PrefixedContext封装DynamicContext
		boolean first = true;
		int i = 0;
		for (Object o : iterable) {
			DynamicContext oldContext = context;
			if (first) {
				context = new PrefixedContext(context, "");
			} else if (separator != null) {
				context = new PrefixedContext(context, separator);
			} else {
				context = new PrefixedContext(context, "");
			}
			// 4.调用applyIndex()方法将index添加到DynamicContext.bindings集合中，供后续使用
			// applyIndex: { "index" = key/i, "__frch_index_uniqueNumber" = key/i}
			
			// 5.调用applyItem()方法将item添加到DynamicContext.bindings集合中
			// applyItem: { "item" = value, "__frch_item_uniqueNumber" = value }
			int uniqueNumber = context.getUniqueNumber();
			if (o instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
				applyIndex(context, mapEntry.getKey(), uniqueNumber);
				applyItem(context, mapEntry.getValue(), uniqueNumber);
			} else {
				applyIndex(context, i, uniqueNumber);
				applyItem(context, o, uniqueNumber);
			}
			
			// 6.转换子节点中的 "#{}" 占位符，此步骤会将PrefixedContext封装成FilteredDynamicContext，在追加子节点转换结果时，就会使用
			//  FilteredDynamicContext.apply()方法将 "#{item}" 占位符转换成 "#{_frch_item_0...}" 的格式
			contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
			if (first) {
				first = !((PrefixedContext) context).isPrefixApplied();
			}
			context = oldContext;   // 复位
			i++;    // 迭代次数+1
		}
		
		// 7.循环结束后，调用DynamicContext.appendSql()方法添加close指定的字符串
		applyClose(context);
		return true;
	}
	
	private void applyIndex(DynamicContext context, Object o, int i) {
		if (index != null) {
			context.bind(index, o);    // key为index，value是集合元素
			context.bind(itemizeItem(index, i), o);  // 为index添加前缀和后缀形成新的key
		}
	}
	
	private void applyItem(DynamicContext context, Object o, int i) {
		if (item != null) {
			context.bind(item, o);
			context.bind(itemizeItem(item, i), o);
		}
	}
	
	private void applyOpen(DynamicContext context) {
		if (open != null) {
			context.appendSql(open);
		}
	}

	private void applyClose(DynamicContext context) {
		if (close != null) {
			context.appendSql(close);
		}
	}
	
	// 添加 "__frch_" 前缀和i后缀
	private static String itemizeItem(String item, int i) {
	    return new StringBuilder(ITEM_PREFIX).append(item).append("_").append(i).toString();
	}	
	
	// 负责处理 "#{}" 占位符
	private static class FilteredDynamicContext extends DynamicContext {
		private DynamicContext delegate; // 底层封装的DynamicContext对象
		private int index; 				 // 对应集合项的在集合中的索引位置
		private String itemIndex; 		 // 对应集合项的index
		private String item; 			 // 对应集合项的item

		public FilteredDynamicContext(Configuration configuration, DynamicContext delegate, String itemIndex, String item, int i) {
			super(configuration, null);
			this.delegate = delegate;
			this.index = i;
			this.itemIndex = itemIndex;
			this.item = item;
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
		public String getSql() {
			return delegate.getSql();
		}
		
	    @Override
	    public void appendSql(String sql) {
	    	GenericTokenParser parser = new GenericTokenParser("#{", "}", new TokenHandler() {
				
	    		// 将形如 "#{item}" 的占位处理成 "#{__frch_item_1}"
	    		// 后面的数字是FilteredDynamicContext产生的单调递增值
				@Override
				public String handleToken(String content) {
					String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
					if (itemIndex != null && newContent.equals(content)) {
						newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
					}
					return new StringBuilder("#{").append(newContent).append("}").toString();
				}
			});
	    	delegate.appendSql(parser.parse(sql));
	    }

		@Override
		public int getUniqueNumber() {
			return delegate.getUniqueNumber();
		}	    
	}
	
	private class PrefixedContext extends DynamicContext {
		private DynamicContext delegate;    // 底层封装的DynamicContext对象
		private String prefix;              // 指定的前缀
		private boolean prefixApplied;      // 是否已经处理过前缀
		
	    public PrefixedContext(DynamicContext delegate, String prefix) {
	        super(configuration, null);
	        this.delegate = delegate;
	        this.prefix = prefix;
	        this.prefixApplied = false;
	    }
	    
		public boolean isPrefixApplied() {
			return prefixApplied;
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
		public void appendSql(String sql) {
			if (!prefixApplied && sql != null && sql.trim().length() > 0) {  // 判断是否已追加前缀
				delegate.appendSql(prefix);		// 追加前缀
				prefixApplied = true;           // 将追加前缀标志置为true，表示已追加
			}
			delegate.appendSql(sql);   // 追加sql片段
		}

		@Override
		public String getSql() {
			return delegate.getSql();
		}

		@Override
		public int getUniqueNumber() {
			return delegate.getUniqueNumber();
		}    
	}
}
