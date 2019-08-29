package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * 解析构造Mapper的辅助类
 */
public class MapperBuilderAssistant extends BaseBuilder {

	private String currentNamespace; // mapper.xml文件中<mapper>节点的namespace属性，为Mapper接口的含包名类名，eg: <mapper
										// namespace="com.learn.ssm.chapter5.mapperInterface.RoleMapper"></mapper>
	private String resource; // 第一种方式中resource属性的值
	private Cache currentCache; // Mapper对应的缓存
	private boolean unresolvedCacheRef; // 表示该mapper.xml中是否已解析缓存配置引用

	public MapperBuilderAssistant(Configuration configuration, String resource) {
		super(configuration);
		this.resource = resource;
	}

	public String getCurrentNamespace() {
		return currentNamespace;
	}

	// 每个mapper.xml的<mapper>节点的namespace属性是必须设的，定义了该mapper.xml要跟哪个Mapper接口绑定
	public void setCurrentNamespace(String currentNamespace) {
		if (currentNamespace == null) {
			throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
		}

		if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
			throw new BuilderException(
					"Wrong namespace. Expected '" + this.currentNamespace + "' but found '" + currentNamespace + "'.");
		}

		this.currentNamespace = currentNamespace;
	}

	public String applyCurrentNamespace(String base, boolean isReference) {
		if (base == null) {
			return null;
		}
		// 是否mapper引用
		if (isReference) {
			if (base.contains(".")) {
				return base;
			}
		} else {
			if (base.startsWith(currentNamespace + ".")) {
				return base;
			}
			// 如果不是mapper引用，但是又不是本mapper的namespace，则抛出异常
			if (base.contains(".")) {
				throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
			}
		}
		return currentNamespace + "." + base;
	}

	// 获取<cache-ref> 节点的namespace属性指向的Cache
	public Cache useCacheRef(String namespace) {
		if (namespace == null) {
			throw new BuilderException("cache-ref element requires a namespace attribute.");
		}
		try {
			unresolvedCacheRef = true; // 初始化未成功解析Cache引用
			Cache cache = configuration.getCache(namespace);
			if (cache == null) {
				throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
			}
			currentCache = cache; // 记录当前命名空间使用的Cache
			unresolvedCacheRef = false; // 标识已成功解析Cache引用
			return cache;
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
		}
	}

	// 创建namespace对应的Cache
	public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
			Integer size, boolean readWrite, boolean blocking, Properties props) {
		// 创建Cache对象，这里使用了建造者模式，CacheBuilder是建造者的角色，而Cache是生成的产品
		Cache cache = new CacheBuilder(currentNamespace).implementation(valueOrDefault(typeClass, PerpetualCache.class))
				.addDecorator(valueOrDefault(evictionClass, LruCache.class)).clearInterval(flushInterval).size(size)
				.readWrite(readWrite).blocking(blocking).properties(props).build();
		// 将Cache对象添加到Configuartion.caches集合中保存，其中会将Cache的id作为key，Cache对象本身作为value
		configuration.addCache(cache);
		currentCache = cache;
		return cache;
	}

	// 创建<parameterMap>节点对应的ParameterMap对象
	public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
		id = applyCurrentNamespace(id, false);
		ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
		configuration.addParameterMap(parameterMap);
		return parameterMap;
	}
	
	// 创建<parameter>节点对应的ParameterMapping对象
	public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
			JdbcType jdbcType, String resultMap, ParameterMode parameterMode,
			Class<? extends TypeHandler<?>> typeHandler, Integer numericScale) {
		resultMap = applyCurrentNamespace(resultMap, true);

		// Class parameterType = parameterMapBuilder.type();
		Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);

		return new ParameterMapping.Builder(configuration, property, javaTypeClass).jdbcType(jdbcType)
				.resultMapId(resultMap).mode(parameterMode).numericScale(numericScale).typeHandler(typeHandlerInstance)
				.build();
	}

	// 创建ResultMap对象并将其添加到Configuration对象的resultMaps集合中
	public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
			List<ResultMapping> resultMappings, Boolean autoMapping) {
		// ResultMap的完整id是"namespace.id"的格式
		id = applyCurrentNamespace(id, false);
		// 获取被继承的ResultMap的完整id，也就是父ResultMap对象的完整id
		extend = applyCurrentNamespace(extend, true);

		if (extend != null) { // 针对extend属性的处理
			// 检测configuration的resultMaps集合中是否存在被继承的ResultMap对象，不存在则抛出异常
			if (!configuration.hasResultMap(extend)) {
				throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
			}
			ResultMap resultMap = configuration.getResultMap(extend); // 获取需要被继承的ResultMap对象，也就是父ResultMap对象
			List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings()); // 获取获取父ResultMap对象中记录的ResultMapping集合
			extendedResultMappings.removeAll(resultMappings); // 删除需要覆盖的ResultMapping集合，这里是筛掉有重复的字段，因为子类父类可能有相同的字段，这里以子类为准
			// 如果当前<resultMap> 节点中定义了 <constructor> 节点，则不需要使用父ResultMap中记录
			// 的相应<constructor> 节点，则将其对应的ResultMapping对象删除
			boolean declaresConstructor = false;
			for (ResultMapping resultMapping : resultMappings) {
				if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
					declaresConstructor = true;
					break;
				}
			}
			if (declaresConstructor) {
				Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
				while (extendedResultMappingsIter.hasNext()) {
					if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
						extendedResultMappingsIter.remove();
					}
				}
			}
			resultMappings.addAll(extendedResultMappings);
		}
		ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
				.discriminator(discriminator).build();
		// 在Configuration
		configuration.addResultMap(resultMap);
		return resultMap;
	}

	// 创建Discriminator对象
	public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType,
			Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {
		ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null,
				null, typeHandler, new ArrayList<ResultFlag>(), null, null, false);
		Map<String, String> namespaceDiscriminatorMap = new HashMap<String, String>();
		for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
			String resultMap = e.getValue();
			resultMap = applyCurrentNamespace(resultMap, true);
			namespaceDiscriminatorMap.put(e.getKey(), resultMap);
		}
		return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
	}

	// 如果有通过节点的 lang属性设置LanguageDriver，则获取对应的LangugeDriver；否则获取默认的LanguageDriver
	public LanguageDriver getLanguageDriver(Class<?> langClass) {
		if (langClass != null) {
			configuration.getLanguageRegistry().register(langClass);
		} else {
			langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
		}
		return configuration.getLanguageRegistry().getDriver(langClass);
	}
	
	public MappedStatement addMappedStatement(String id,
										      SqlSource sqlSource,
										      StatementType statementType,
										      SqlCommandType sqlCommandType,
										      Integer fetchSize,
										      Integer timeout,
										      String parameterMap,
										      Class<?> parameterType,
										      String resultMap,
										      Class<?> resultType,
										      ResultSetType resultSetType,
										      boolean flushCache,
										      boolean useCache,
										      boolean resultOrdered,
										      KeyGenerator keyGenerator,
										      String keyProperty,
										      String keyColumn,
										      String databaseId,
										      LanguageDriver lang,
										      String resultSets) {
		return null;
	}
	
	private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType,
			JdbcType jdbcType) {
		if (javaType == null) {
			if (JdbcType.CURSOR.equals(jdbcType)) {
				javaType = java.sql.ResultSet.class;
			} else if (Map.class.isAssignableFrom(resultType)) {
				javaType = Object.class;
			} else {
				MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
				javaType = metaResultType.getGetterType(property);
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}

	// 为<resultMap> 中的列创建ResultMapping对象
	public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
			JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
			Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn,
			boolean lazy) {
		// 解析 <resultType> 节点指定的property属性的类型
		Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
		// 获取typeHandler指定的TypeHandler对象，底层依赖于typeHandlerRegistry
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
		// 解析column属性值，当column是"{prop1=col1,prop2=col2}"形式时，会解析成ResultMapping对象集合，
		// column的这种形式主要用于嵌套查询的参数传递
		List<ResultMapping> composites = parseCompositeColumnName(column);
		if (composites.size() > 0) {
			column = null;
		}
		// 创建ResultMapping.Builder对象，创建ResultMapping对象，并设置其字段
		return new ResultMapping.Builder(configuration, property, column, javaTypeClass).jdbcType(jdbcType)
				.nestedQueryId(applyCurrentNamespace(nestedSelect, true))
				.nestedResultMapId(applyCurrentNamespace(nestedResultMap, true)).resultSet(resultSet)
				.typeHandler(typeHandlerInstance).flags(flags == null ? new ArrayList<ResultFlag>() : flags)
				.composites(composites).notNullColumns(null).columnPrefix(columnPrefix).foreignColumn(foreignColumn)
				.lazy(lazy).build();
	}

	// 解析字段的 Java 类型
	private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
		// 如果没有显式指定字段的java类型是什么则根据反射获取
		if (javaType == null && property != null) {
			try {
				MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
				javaType = metaResultType.getSetterType(property);
			} catch (Exception e) {
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}

	// 解析表示多个参数类型映射的json字符串
	private List<ResultMapping> parseCompositeColumnName(String columnName) {
		List<ResultMapping> composites = new ArrayList<ResultMapping>();
		if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
			StringTokenizer parser = new StringTokenizer(columnName, "{}=", false);
			while (parser.hasMoreTokens()) {
				String property = parser.nextToken();
				String column = parser.nextToken();
				ResultMapping complexResultMapping = new ResultMapping.Builder(configuration, property, column,
						configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
				composites.add(complexResultMapping);
			}
		}
		return composites;
	}

	private <T> T valueOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}
}