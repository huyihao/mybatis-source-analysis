package org.apache.ibatis.mapping;

import java.util.List;

import org.apache.ibatis.binding.SqlCommandType;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.session.Configuration;

/**
 * MappedStatement对象中封装了SQL语句相关的信息，在MyBatis初始化时创建
 */
public final class MappedStatement {

	private String resource;
	private Configuration configuration;
	private String id;
	private Integer fetchSize;
	private Integer timeout;
	private StatementType statementType;
	private ResultSetType resultSetType;
	
	
	
	private List<ResultMap> resultMaps;
	private boolean flushCacheRequired;
	private boolean useCache;
	private boolean resultOrdered;
	private SqlCommandType sqlCommandType;
	
	private String[] keyProperties;
	private String[] keyColumns;
	private boolean hasNestedResultMaps;
	private String databaseId;
	private Log statementLog;
	
	private String[] resultSets;
	
	MappedStatement() {}
	
	public SqlCommandType getSqlCommandType() {
	    return sqlCommandType;
	}

	public String getResource() {
	    return resource;
	}

	public Configuration getConfiguration() {
	    return configuration;
	}

	public String getId() {
	    return id;
	}

	public boolean hasNestedResultMaps() {
	    return hasNestedResultMaps;
	}

	public Integer getFetchSize() {
	    return fetchSize;
	}

	public Integer getTimeout() {
	    return timeout;
	}

	public StatementType getStatementType() {
	    return statementType;
	}	
	
	public ResultSetType getResultSetType() {
	    return resultSetType;
	}
	
	public List<ResultMap> getResultMaps() {
		return resultMaps;
	}
	
	public boolean isFlushCacheRequired() {
	    return flushCacheRequired;
	}

	public boolean isUseCache() {
	    return useCache;
	}

	public boolean isResultOrdered() {
	    return resultOrdered;
	}

	public String getDatabaseId() {
	    return databaseId;
	}

	public String[] getKeyProperties() {
	    return keyProperties;
	}

	public String[] getKeyColumns() {
	    return keyColumns;
	}

	public Log getStatementLog() {
	    return statementLog;
	}	
	
	public String[] getResultSets() {
	    return resultSets;
	}	
}
