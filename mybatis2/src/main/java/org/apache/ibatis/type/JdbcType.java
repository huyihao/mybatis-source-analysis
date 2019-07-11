package org.apache.ibatis.type;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public enum JdbcType {
	// 数组
	ARRAY(Types.ARRAY),
	// 整数，字段大小从小到大
	BIT(Types.BIT),
	TINYINT(Types.TINYINT),
	SMALLINT(Types.SMALLINT),
	INTEGER(Types.INTEGER),
	BIGINT(Types.BIGINT),
	// 浮点数，精度从小到大
	FLOAT(Types.FLOAT),
	REAL(Types.REAL),
	DOUBLE(Types.DOUBLE),
	NUMERIC(Types.NUMERIC),
	DECIMAL(Types.DECIMAL),
	// 字符，字段从小到大
	CHAR(Types.CHAR),
	VARCHAR(Types.VARCHAR),
	LONGVARCHAR(Types.LONGVARCHAR),
	// 时间日期类型
	DATE(Types.DATE),
	TIME(Types.TIME),
	TIMESTAMP(Types.TIMESTAMP),
	// 二进制数据
	BINARY(Types.BINARY),
	VARBINARY(Types.VARBINARY),
	LONGVARBINARY(Types.LONGVARBINARY),
	// 空
	NULL(Types.NULL),
	OTHER(Types.OTHER),
	// 用二进制表示的大对象
	BLOB(Types.BLOB),
	CLOB(Types.CLOB),
	// 布尔型
	BOOLEAN(Types.BOOLEAN),
	// 游标，Oracle
	CURSOR(-10), 
    UNDEFINED(Integer.MIN_VALUE + 1000),
    // 使用Unicode标准字符集
    NVARCHAR(Types.NVARCHAR), // JDK6
    NCHAR(Types.NCHAR), // JDK6
    NCLOB(Types.NCLOB), // JDK6
    
    STRUCT(Types.STRUCT),
    JAVA_OBJECT(Types.JAVA_OBJECT),
    DISTINCT(Types.DISTINCT),
    REF(Types.REF),
    DATALINK(Types.DATALINK),
    ROWID(Types.ROWID), // JDK6
    LONGNVARCHAR(Types.LONGNVARCHAR), // JDK6
    SQLXML(Types.SQLXML), // JDK6
    DATETIMEOFFSET(-155); // SQL Server 2008	
	
	public final int TYPE_CODE;
	private static Map<Integer, JdbcType> codeLookup = new HashMap<Integer, JdbcType>();
	
	static {
		for (JdbcType type : JdbcType.values()) {
			codeLookup.put(type.TYPE_CODE, type);
		}
	}
	
	JdbcType(int code) {
		this.TYPE_CODE = code;
	}
	
	public static JdbcType forCode(int code) {
		return codeLookup.get(code);
	}
}
