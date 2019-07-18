package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 类型转换接口，用于执行JDBC查询语句时设置查询条件值和获取查询结果时将查到的数据转化为Java对象
public interface TypeHandler<T> {
	// 在通过 PreparedStatement 为SQL语句绑定参数时，会将数据由Java类型转化为JdbcType类型
	void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;
	
	// 从 ResultSet 中获取数据时会调用此方法，会将数据由JdbcType类型转换成Java类型
	T getResult(ResultSet rs, String columnName) throws SQLException;
	T getResult(ResultSet rs, int columnIndex) throws SQLException;
	T getResult(CallableStatement cs, int columnIndex) throws SQLException;
}
