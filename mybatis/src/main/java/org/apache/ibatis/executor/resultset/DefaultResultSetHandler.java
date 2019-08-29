package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class DefaultResultSetHandler implements ResultSetHandler {

	private static final Object DEFERED = new Object();
	
	private final Executor executor;;                       // SQL执行器
	private final Configuration configuration;              // mybatis全局配置
	private final MappedStatement mappedStatement;          // 根据映射XML文件创建的Statement对象
	private final RowBounds rowBounds;                      // 分页插件
	private final ParameterHandler parameterHandler;        // 参数处理器
	private final ResultHandler<?> resultHandler;           // 用户指定用于处理结果集的ResultHandler对象
	private final BoundSql boundSql;                        // 动态SQL解析后生成的BoundSql对象（含有可直接执行的SQL，设置SQL中"?"占位的值需要的参数类型和值信息集合）
	private final TypeHandlerRegistry typeHandlerRegistry;  // 类型转换器注册中心
	private final ObjectFactory objectFactory;              // 对象工厂
	private final ReflectorFactory reflectorFactory;        // 反射器工厂
	
	// 嵌套的ResultMap
	private final Map<CacheKey, Object> nestedResultObjects = new HashMap<CacheKey, Object>();
	private final Map<String, Object> ancestorObjects = new HashMap<String, Object>();
	private Object previousRowValue;

	// 多结果集
	private final Map<String, ResultMapping> nextResultMaps = new HashMap<String, ResultMapping>();
	private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<CacheKey, List<PendingRelation>>();
	
	private static class PendingRelation {
		public MetaObject metaObject;
		public ResultMapping propertyMapping;
	}

	private static class UnMappedColumnAutoMapping {
		private final String column;
		private final String property;
		private final TypeHandler<?> typeHandler;
		private final boolean primitive;

		public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler,
				boolean primitive) {
			this.column = column;
			this.property = property;
			this.typeHandler = typeHandler;
			this.primitive = primitive;
		}
	} 	
	
	public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler, 
								   ResultHandler<?> resultHandler, BoundSql boundSql, RowBounds rowBounds) {
		this.executor = executor;
		this.configuration = mappedStatement.getConfiguration();
		this.mappedStatement = mappedStatement;
		this.rowBounds = rowBounds;
		this.parameterHandler = parameterHandler;
		this.boundSql = boundSql;
		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.objectFactory = configuration.getObjectFactory();
		this.reflectorFactory = configuration.getReflectorFactory();
		this.resultHandler = resultHandler;
	}
	
	@Override
	public <E> List<E> handleResultSets(Statement stmt) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E> Cursor<E> handleCursorResuleSets(Statement stmt) throws SQLException {
		// 该集合用于保存映射结果集得到的结果对象
		List<ResultMap> resultMaps = mappedStatement.getResultMaps();
		return null;
	}

	@Override
	public void handleOutputParameters(CallableStatement cs) throws SQLException {
		// TODO Auto-generated method stub
		
	}

}
