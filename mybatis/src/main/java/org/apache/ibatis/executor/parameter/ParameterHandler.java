package org.apache.ibatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 一个用来设置 {@code PreparedStatement} 的参数的参数处理器
 */
public interface ParameterHandler {
	
	Object setParameterObject();
	
	void setParameters(PreparedStatement ps) throws SQLException;
}
