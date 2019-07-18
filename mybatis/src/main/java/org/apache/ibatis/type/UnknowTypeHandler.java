package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UnknowTypeHandler extends BaseTypeHandler<Object> {

	private static final ObjectTypeHandler OBJECT_TYPE_HANDLER = new ObjectTypeHandler();
	
	private TypeHandlerRegistry typeHandlerRegistry;
	
	public UnknowTypeHandler(TypeHandlerRegistry typeHandlerRegistry) {
		this.typeHandlerRegistry = typeHandlerRegistry;
	}
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
			throws SQLException {
		TypeHandler handler = resolveTypeHandler(parameter, jdbcType);
	}

	@Override
	public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private TypeHandler<? extends Object> resolveTypeHandler(Object parameter, JdbcType jdbcType) {
		TypeHandler<? extends Object> handler = null;
		if (parameter == null) {
			handler = OBJECT_TYPE_HANDLER;
		} else {
			
		}
		return handler;
	}

}
