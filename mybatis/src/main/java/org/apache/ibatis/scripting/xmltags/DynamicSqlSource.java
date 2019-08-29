package org.apache.ibatis.scripting.xmltags;

import java.util.Map;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * 动态SqlSource
 */
public class DynamicSqlSource implements SqlSource {

	private Configuration configuration;
	private SqlNode rootSqlNode;	
	
	public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
		this.configuration = configuration;
		this.rootSqlNode = rootSqlNode;
	}
	
	@Override
	public BoundSql getBoundSql(Object parameterObject) {
		// 1.获取调用SQL时传入的参数对象信息，构建动态上下文对象DynamicContext
		DynamicContext context = new DynamicContext(configuration, parameterObject);
		
		// 2.通过调用SqlNode.apply()方法调用整个属性结构中全部SqlNode.apply()方法
		//   这里体现出组合设计模式的好处。每个SqlNode的apply()方法都将解析到的SQL语句片段
		//   追加到context中，最终通过context.getSql()得到完整的SQL语句
		rootSqlNode.apply(context);
		
		// 3.创建SqlSourceBuilder，解析参数属性，将SQL语句的 "#{}" 占位符替换成 "?" 占位符
		SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
		Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
		SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
		
		// 4.创建BoundSql对象，并将DynamicContext.bindings中的参数信息赋值到其additionalParameters集合中保存
		BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
	    for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
	        boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
	    }
	    return boundSql;
	}

}
