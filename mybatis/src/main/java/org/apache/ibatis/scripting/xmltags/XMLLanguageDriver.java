package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

public class XMLLanguageDriver implements LanguageDriver {

	@Override
	public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
			BoundSql boundSql) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
		XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
		return builder.parseScriptNode();
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
		
		return null;
	}

}
