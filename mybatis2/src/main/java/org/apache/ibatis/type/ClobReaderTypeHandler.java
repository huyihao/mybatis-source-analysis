package org.apache.ibatis.type;

import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java Reader类型和数据库Clob类型的转换
 */
public class ClobReaderTypeHandler extends BaseTypeHandler<Reader> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Reader parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setClob(i, parameter);		
	}

	@Override
	public Reader getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toReader(rs.getClob(columnName));
	}

	@Override
	public Reader getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toReader(rs.getClob(columnIndex));
	}

	@Override
	public Reader getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toReader(cs.getClob(columnIndex));
	}
	
	private Reader toReader(Clob clob) throws SQLException {
		if (clob == null) {
			return null;
		} else {
			return clob.getCharacterStream();
		}
	}
}
