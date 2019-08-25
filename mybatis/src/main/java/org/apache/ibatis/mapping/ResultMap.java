package org.apache.ibatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.ibatis.session.Configuration;

/**
 * 结果映射类
 * [功能] 负责将SQL查询到的结果转化为java对象
 *
 * eg:
 * 	<resultMap type="role" id="roleMap">
 *		<id property="id" column="id"/>
 *		
 *		<result property="roleName" column="role_name"/>
 *		<result property="note" column="note"/>
 *	</resultMap>
 */
public class ResultMap { 
	private String id;	                                    // <resultMap> 节点的 namespace.id 属性
	private Class<?> type;                                  // <resultMap> 节点的 type 属性
	private List<ResultMapping> resultMappings;             // 记录了除 <discrimiator> 节点之外的其他映射关系（即ResultMapping对象集合）
	private List<ResultMapping> idResultMappings;           // 记录了映射关系中带有ID标志的映射关系，例如<id>节点和<constructor>节点的<idArg>子节点
	private List<ResultMapping> constructorResultMappings;  // 记录了映射关系中带有Constructor标志的映射关系，例如<constructor>所有子元素
	private List<ResultMapping> propertyResultMappings;     // 记录了映射关系中不带有Constructor标志的映射关系
	private Set<String> mappedColumns;                      // 记录了所有映射关系中涉及的column属性的集合
	private Discriminator discriminator;                    // 鉴别器，对应 <discriminator> 节点
	private boolean hasNestedResultMaps;                    // 是否含有嵌套的结果映射，如果某个映射关系中存在resultMap属性，且不存在resultSet属性，则为true
	private boolean hasNestedQueries;                       // 是否含有嵌套的查询，如果某个属性映射存在select属性，则为true
	private Boolean autoMapping;                            // 是否开启自动映射

	ResultMap() {}
	
	public static class Builder {
		private ResultMap resultMap = new ResultMap();

		public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
			this(configuration, id, type, resultMappings, null);
		}

		public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings,
				Boolean autoMapping) {
			resultMap.id = id;
			resultMap.type = type;
			resultMap.resultMappings = resultMappings;
			resultMap.autoMapping = autoMapping;
		}

		public Builder discriminator(Discriminator discriminator) {
			resultMap.discriminator = discriminator;
			return this;
		}

		public Class<?> type() {
			return resultMap.type;
		}

		public ResultMap build() {
			// 所有的resultMap必须有个id，为了防止不同mapper中的resultMap定义的id重复
			// 这里的id的值实际上是mapper的namespace.id的形式
			if (resultMap.id == null) {
				throw new IllegalArgumentException("ResultMaps must have an id");
			}
			resultMap.mappedColumns = new HashSet<String>();
			resultMap.idResultMappings = new ArrayList<ResultMapping>();
			resultMap.constructorResultMappings = new ArrayList<ResultMapping>();
			resultMap.propertyResultMappings = new ArrayList<ResultMapping>();
			for (ResultMapping resultMapping : resultMap.resultMappings) {
				// 判断该resultMap是否有嵌套查询的resultMap
				resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
				// 判断该resultMap是否有级联嵌套的resultMap
				resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps
						|| (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
				final String column = resultMapping.getColumn();
				if (column != null) {
					resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
				} else if (resultMapping.isCompositeResult()) {
					for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
						final String compositeColumn = compositeResultMapping.getColumn();
						if (compositeColumn != null) {
							resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
						}
					}
				}
				// resultMapping为 <constructor>
				if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
					resultMap.constructorResultMappings.add(resultMapping);
				} else {
					resultMap.propertyResultMappings.add(resultMapping);
				}
				if (resultMapping.getFlags().contains(ResultFlag.ID)) {
					resultMap.idResultMappings.add(resultMapping);
				}
			}
			if (resultMap.idResultMappings.isEmpty()) {
				resultMap.idResultMappings.addAll(resultMap.resultMappings);
			}
			// lock down collections
			resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
			resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
			resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
			resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
			resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
			return resultMap;
		}
	}

	public String getId() {
		return id;
	}

	public boolean hasNestedResultMaps() {
		return hasNestedResultMaps;
	}

	public boolean hasNestedQueries() {
		return hasNestedQueries;
	}

	public Class<?> getType() {
		return type;
	}

	public List<ResultMapping> getResultMappings() {
		return resultMappings;
	}

	public List<ResultMapping> getConstructorResultMappings() {
		return constructorResultMappings;
	}

	public List<ResultMapping> getPropertyResultMappings() {
		return propertyResultMappings;
	}

	public List<ResultMapping> getIdResultMappings() {
		return idResultMappings;
	}

	public Set<String> getMappedColumns() {
		return mappedColumns;
	}

	public Discriminator getDiscriminator() {
		return discriminator;
	}

	public void forceNestedResultMaps() {
		hasNestedResultMaps = true;
	}

	public Boolean getAutoMapping() {
		return autoMapping;
	}
}