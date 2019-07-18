package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.executor.result.ResultMapException;

/**
 * 所有类型转换器的抽象基类
 * 提供了对空值设置的处理，需要传入类型代码值(Types)
 * 对于非空参数和结果集的处理，定义了四个抽象方法:
 * 1) 给JDBC状态语句设置非空的参数值
 * 2) 通过列名获取ResultSet中的字段值
 * 3) 通过列下标获取ResultSet中的字段值
 * 4) 通过下标获取CallableStatement中的字段值
 * 
 * 具体的类型转换器需要继承本类
 */
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

	@Override
	public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
		// 如果参数值为null，则直接根据下标和参数的类型调用PreparedStatement.setNull()方法即可，不管对哪种类型转换器，这都是通用的
		if (parameter == null) {
			if (jdbcType == null) {
				throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
			}
			try {
				ps.setNull(i, jdbcType.TYPE_CODE);
			} catch (SQLException e) {
				throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . " + 
							"Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. " +
							"Cause: " + e, e);
			}
		} else {
			// 如果参数值非空，则要根据不同参数类型调用不同的方法，比如String类型调用setString()、int类型调用setInt()这些在各个具体类型的类型转换器里实现调用
			try {
				setNonNullParameter(ps, i, parameter, jdbcType);
			} catch (SQLException e) {
				throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . " +
							"Try setting a different JdbcType for this parameter or a different configuration property. " +
							"Cause: " + e, e);
			}
		}
		
	}

	@Override
	public T getResult(ResultSet rs, String columnName) throws SQLException {
		T result = null;
		try {
			result = getNullableResult(rs, columnName);
		} catch (SQLException e) {
			throw new ResultMapException("Error attempting to get column '" + columnName + "'" + "' from result set.  Cause: " + e, e);
		}
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	@Override
	public T getResult(ResultSet rs, int columnIndex) throws SQLException {
		T result = null;
		try {
			result = getNullableResult(rs, columnIndex);
		} catch (SQLException e) {
			throw new ResultMapException("Error attempting to get column #" + columnIndex + " from result set.  Cause: " + e, e);
		}
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	@Override
	public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
		T result = null;
		try {
			result = getNullableResult(cs, columnIndex);
		} catch (SQLException e) {
			throw new ResultMapException("Error attempting to get column #" + columnIndex + " from callable statement.  Cause: " + e, e);
		}
		if (cs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	public abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;
	public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;
	public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;
	public abstract T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException;
}
