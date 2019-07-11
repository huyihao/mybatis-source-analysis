package org.apache.ibatis.type;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 将Java InputStream类型数据存储在数据库的Blob类型字段中
 */
public class BlobInputStreamTypeHandler extends BaseTypeHandler<InputStream> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, InputStream parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setBlob(i, parameter);		
	}

	@Override
	public InputStream getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toInputStream(rs.getBlob(columnName));
	}

	@Override
	public InputStream getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toInputStream(rs.getBlob(columnIndex));
	}

	@Override
	public InputStream getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toInputStream(cs.getBlob(columnIndex));
	}

	private InputStream toInputStream(Blob blob) throws SQLException {
		if (blob == null) {
			return null;
		} else {
			return blob.getBinaryStream();
		}
	}
}
