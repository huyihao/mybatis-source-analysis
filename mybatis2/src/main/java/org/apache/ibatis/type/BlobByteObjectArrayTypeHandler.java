package org.apache.ibatis.type;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java Byte[]类型和数据库blob类型的互相转换
 */
public class BlobByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Byte[] parameter, JdbcType jdbcType)
			throws SQLException {
		ByteArrayInputStream bis = new ByteArrayInputStream(ByteArrayUtils.convertToPrimitiveArray(parameter));
		ps.setBinaryStream(i, bis, parameter.length);
	}

	@Override
	public Byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Blob blob = rs.getBlob(columnName);
		return getBytes(blob);
	}

	@Override
	public Byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Blob blob = rs.getBlob(columnIndex);
		return getBytes(blob);
	}

	@Override
	public Byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Blob blob = cs.getBlob(columnIndex);
		return getBytes(blob);
	}

	private Byte[] getBytes(Blob blob) throws SQLException {
		Byte[] returnValue = null;
		if (blob != null) {
			returnValue = ByteArrayUtils.convertToObjectArray(blob.getBytes(1, (int) blob.length()));
		}
		return returnValue;
	}
}
