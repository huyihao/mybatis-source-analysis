package org.apache.ibatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 表示<resultMap> 节点中每一列的java属性跟数据库的列的映射转化关系
 * eg:
 * 	   <id property="id" column="id"/>
 * 	   <result property="roleName" column="role_name"/>
 */
public class ResultMapping {

	private Configuration configuration;   // Configuration对象
	private String property;               // 对应节点的property属性，表示的是与该列进行映射的属性
	private String column;                 // 对应节点的column属性，表示的是从数据库中得到的列名或是列名的别名
	private Class<?> javaType;             // 对应节点的javaType属性，表示的是一个JavaBean的完全限定名，或一个类型别名
	private JdbcType jdbcType;             // 对应节点的jdbcType属性，表示的是进行映射的列的JDBC类型
	private TypeHandler<?> typeHandler;    // 对应节点的TypeHandler属性，表示的是类型处理器，它会覆盖默认的类型处理器
	
	// 对应节点的 resultMap 属性，该属性通过id引用了另一个<resultMap>节点定义，它负责将结果集中的一部
	// 分列映射成其他关联的结果对象。这样我们就可以通过join方式进行关联查询，然后直接映射成多个对象，并同时
	// 设置这些对象之间的组合关系
	private String nestedResultMapId;
	
	// 对应节点的 select 属性，该属性通过id引用了另一个<select>节点定义，它会把指定的列的值传入
	// select 属性指定 的select语句中作为参数进行查询。使用select属性可能导致N+1问题
	private String nestedQueryId;
	private Set<String> notNullColumns;    // 对应节点的notNullColumn属性拆分后的结果
	private String columnPrefix;           // 对应节点的columnPrefix属性
	private List<ResultFlag> flags;        // 处理后的标志，标志共两个：id和constructor
	private List<ResultMapping> composites;// 对应节点的column属性拆分后生成的结果，composites.size() > 0会使column为null
	private String resultSet;              // 对应节点的resultSet属性
	private String foreignColumn;          // 对应节点的foreignColumn属性
	private boolean lazy;                  // 是否延迟加载，对应节点的fetchType属性
	
	ResultMapping() {}
	
	public static class Builder {
		private ResultMapping resultMapping = new ResultMapping();

		public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
			this(configuration, property);
			resultMapping.column = column;
			resultMapping.typeHandler = typeHandler;
		}

		public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
			this(configuration, property);
			resultMapping.column = column;
			resultMapping.javaType = javaType;
		}

		public Builder(Configuration configuration, String property) {
			resultMapping.configuration = configuration;
			resultMapping.property = property;
			resultMapping.flags = new ArrayList<ResultFlag>();
			resultMapping.composites = new ArrayList<ResultMapping>();
			resultMapping.lazy = configuration.isLazyLoadingEnabled();
		}

		public Builder javaType(Class<?> javaType) {
			resultMapping.javaType = javaType;
			return this;
		}

		public Builder jdbcType(JdbcType jdbcType) {
			resultMapping.jdbcType = jdbcType;
			return this;
		}

		public Builder nestedResultMapId(String nestedResultMapId) {
			resultMapping.nestedResultMapId = nestedResultMapId;
			return this;
		}

		public Builder nestedQueryId(String nestedQueryId) {
			resultMapping.nestedQueryId = nestedQueryId;
			return this;
		}

		public Builder resultSet(String resultSet) {
			resultMapping.resultSet = resultSet;
			return this;
		}

		public Builder foreignColumn(String foreignColumn) {
			resultMapping.foreignColumn = foreignColumn;
			return this;
		}

		public Builder notNullColumns(Set<String> notNullColumns) {
			resultMapping.notNullColumns = notNullColumns;
			return this;
		}

		public Builder columnPrefix(String columnPrefix) {
			resultMapping.columnPrefix = columnPrefix;
			return this;
		}

		public Builder flags(List<ResultFlag> flags) {
			resultMapping.flags = flags;
			return this;
		}

		public Builder typeHandler(TypeHandler<?> typeHandler) {
			resultMapping.typeHandler = typeHandler;
			return this;
		}

		public Builder composites(List<ResultMapping> composites) {
			resultMapping.composites = composites;
			return this;
		}

		public Builder lazy(boolean lazy) {
			resultMapping.lazy = lazy;
			return this;
		}

		public ResultMapping build() {
			// 将 resultMapping 对象的flags属性和composites属性设置为不可变集合
			resultMapping.flags = Collections.unmodifiableList(resultMapping.flags);
			resultMapping.composites = Collections.unmodifiableList(resultMapping.composites);
			resolveTypeHandler();
			validate();
			return resultMapping;
		}

		private void validate() {
			// Issue #697: cannot define both nestedQueryId and nestedResultMapId
			// 不能同时使用 resultMap 和 select 属性
			if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
				throw new IllegalStateException(
						"Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
			}
			// Issue #5: there should be no mappings without typehandler
			// 不应该存在没有类型转化器的映射，一般常规类型都覆盖了
			if (resultMapping.nestedQueryId == null && resultMapping.nestedResultMapId == null
					&& resultMapping.typeHandler == null) {
				throw new IllegalStateException("No typehandler found for property " + resultMapping.property);
			}
			// Issue #4 and GH #39: column is optional only in nested resultmaps but not in
			// the rest
			if (resultMapping.nestedResultMapId == null && resultMapping.column == null
					&& resultMapping.composites.isEmpty()) {
				throw new IllegalStateException(
						"Mapping is missing column attribute for property " + resultMapping.property);
			}
			if (resultMapping.getResultSet() != null) {
				int numColumns = 0;
				if (resultMapping.column != null) {
					numColumns = resultMapping.column.split(",").length;
				}
				int numForeignColumns = 0;
				if (resultMapping.foreignColumn != null) {
					numForeignColumns = resultMapping.foreignColumn.split(",").length;
				}
				if (numColumns != numForeignColumns) {
					throw new IllegalStateException(
							"There should be the same number of columns and foreignColumns in property "
									+ resultMapping.property);
				}
			}
		}

		private void resolveTypeHandler() {
			if (resultMapping.typeHandler == null && resultMapping.javaType != null) {
				Configuration configuration = resultMapping.configuration;
				TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
				resultMapping.typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.javaType,
						resultMapping.jdbcType);
			}
		}

		public Builder column(String column) {
			resultMapping.column = column;
			return this;
		}
	}

	// Getters & Setters
	// [start]
	public String getProperty() {
		return property;
	}

	public String getColumn() {
		return column;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public JdbcType getJdbcType() {
		return jdbcType;
	}

	public TypeHandler<?> getTypeHandler() {
		return typeHandler;
	}

	public String getNestedResultMapId() {
		return nestedResultMapId;
	}

	public String getNestedQueryId() {
		return nestedQueryId;
	}

	public Set<String> getNotNullColumns() {
		return notNullColumns;
	}

	public String getColumnPrefix() {
		return columnPrefix;
	}

	public List<ResultFlag> getFlags() {
		return flags;
	}

	public List<ResultMapping> getComposites() {
		return composites;
	}

	public boolean isCompositeResult() {
		return this.composites != null && !this.composites.isEmpty();
	}

	public String getResultSet() {
		return this.resultSet;
	}

	public String getForeignColumn() {
		return foreignColumn;
	}

	public void setForeignColumn(String foreignColumn) {
		this.foreignColumn = foreignColumn;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}
	// [end]

	@Override
	public boolean equals(Object o) {
	    if (this == o) {
	        return true;
        }
	    if (o == null || getClass() != o.getClass()) {
	    	return false;
	    }

	    ResultMapping that = (ResultMapping) o;

	    if (property == null || !property.equals(that.property)) {
	    	return false;
	    }

	    return true;
	}
	
	@Override
	public int hashCode() {
		if (property != null) {
			return property.hashCode();
		} else if (column != null) {
			return column.hashCode();
		} else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ResultMapping{");
		// sb.append("configuration=").append(configuration); // configuration doesn't
		// have a useful .toString()
		sb.append("property='").append(property).append('\'');
		sb.append(", column='").append(column).append('\'');
		sb.append(", javaType=").append(javaType);
		sb.append(", jdbcType=").append(jdbcType);
		// sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't
		// have a useful .toString()
		sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
		sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
		sb.append(", notNullColumns=").append(notNullColumns);
		sb.append(", columnPrefix='").append(columnPrefix).append('\'');
		sb.append(", flags=").append(flags);
		sb.append(", composites=").append(composites);
		sb.append(", resultSet='").append(resultSet).append('\'');
		sb.append(", foreignColumn='").append(foreignColumn).append('\'');
		sb.append(", lazy=").append(lazy);
		sb.append('}');
		return sb.toString();
	}
}
