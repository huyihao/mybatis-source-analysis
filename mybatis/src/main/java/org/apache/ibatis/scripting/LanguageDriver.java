package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;

public interface LanguageDriver {

	// 创建一个用来传递实际参数给JDBC statement的 {@link ParameterHandler}
	ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);
	
	/**
	 * 创建一个 {@link SqlSource}，它将保存从mapper xml配置文件中读取出来的statement
	 * 该方法在mybatis启动时被调用，当mapped statement从一个类或者xml文件中读取出来时
	 */
	SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);
	
	SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);
}
