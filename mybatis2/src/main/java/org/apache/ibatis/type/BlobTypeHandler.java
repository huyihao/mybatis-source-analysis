package org.apache.ibatis.type;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BLOB(binary large object): 二进制大对象
 * Java byte[]类型和数据库blob的互相转换
 */
public class BlobTypeHandler extends BaseTypeHandler<byte[]> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType)
			throws SQLException {
		ByteArrayInputStream bis = new ByteArrayInputStream(parameter);
		ps.setBinaryStream(i, bis, parameter.length);
		// ps.setBlob(i, bis, parameter.length);
		// Blob是一个接口，如果使用setBlob()，需要用Blob的实现类的设值
		// 在JDK中，Blob接口的实现类有:
		// 1) BlobImpl: 扩展库中jtds.jar中
		// 2) SerialBlob: javax.sql.rowset.serial.SerialBlob
	}

	@Override
	public byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Blob blob = rs.getBlob(columnName);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}
		return returnValue;
	}

	@Override
	public byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Blob blob = rs.getBlob(columnIndex);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}
		return returnValue;
	}

	@Override
	public byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Blob blob = cs.getBlob(columnIndex);
		byte[] returnValue = null;
		if (null != blob) {
			returnValue = blob.getBytes(1, (int) blob.length());
		}
		return returnValue;
	}
}
