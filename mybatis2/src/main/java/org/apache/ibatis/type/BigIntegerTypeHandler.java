package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 大整数类型转换器
 */
public class BigIntegerTypeHandler extends BaseTypeHandler<BigInteger> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, BigInteger parameter, JdbcType jdbcType)
			throws SQLException {
		// PreparedStatement没有setBigInteger，这里用BigDecimal类型
		ps.setBigDecimal(i, new BigDecimal(parameter));
	}

	@Override
	public BigInteger getNullableResult(ResultSet rs, String columnName) throws SQLException {
		BigDecimal bigDecimal = rs.getBigDecimal(columnName);
		return bigDecimal == null ? null : bigDecimal.toBigInteger();
	}

	@Override
	public BigInteger getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		BigDecimal bigDecimal = rs.getBigDecimal(columnIndex);
		return bigDecimal == null ? null : bigDecimal.toBigInteger();
	}

	@Override
	public BigInteger getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		BigDecimal bigDecimal = cs.getBigDecimal(columnIndex);
		return bigDecimal == null ? null : bigDecimal.toBigInteger();
	}

}
