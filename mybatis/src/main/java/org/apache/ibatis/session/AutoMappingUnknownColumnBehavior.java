package org.apache.ibatis.session;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * 指定自动映射中检测到未知列/属性时的处理策略:
 * 1) NONE: 不处理跳过
 * 2) WARNING: 告警
 * 3) FAILING: 失败抛出异常
 */
public enum AutoMappingUnknownColumnBehavior {
	NONE {
		@Override
		public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
			
		}
	},
	
	WARNING {
		@Override
		public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
			log.warn(buildMessage(mappedStatement, columnName, property, propertyType));
		}
	},
	
	FAILING {
		@Override
		public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
			throw new SqlSessionException(buildMessage(mappedStatement, columnName, property, propertyType));
		}
	};
	
	private static final Log log = LogFactory.getLog(AutoMappingUnknownColumnBehavior.class);
	
	public abstract void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType);
	
	private static String buildMessage(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
	    return new StringBuilder("Unknown column is detected on '")
		      .append(mappedStatement.getId())
		      .append("' auto-mapping. Mapping parameters are ")
		      .append("[")
		      .append("columnName=").append(columnName)
		      .append(",").append("propertyName=").append(property)
		      .append(",").append("propertyType=").append(propertyType != null ? propertyType.getName() : null)
		      .append("]")
		      .toString();
	}	
}
