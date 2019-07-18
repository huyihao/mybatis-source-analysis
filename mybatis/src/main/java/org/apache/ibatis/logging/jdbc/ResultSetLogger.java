package org.apache.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

public class ResultSetLogger extends BaseJdbcLogger implements InvocationHandler {

	private static Set<Integer> BLOB_TYPES = new HashSet<Integer>();  // 记录了超大长度的类型
	private boolean first = true;   // 是否是ResultSet结果集的第一行
	private int rows = 0;           // 统计行数	
	private ResultSet rs;           // 真正的ResultSet对象
	private Set<Integer> blobColumns = new HashSet<Integer>();  // 记录了超大字段的列编号
	
	static {
		BLOB_TYPES.add(Types.BINARY);
		BLOB_TYPES.add(Types.BLOB);
		BLOB_TYPES.add(Types.CLOB);
		BLOB_TYPES.add(Types.LONGNVARCHAR);
		BLOB_TYPES.add(Types.LONGVARBINARY);
		BLOB_TYPES.add(Types.LONGVARCHAR);
		BLOB_TYPES.add(Types.NCLOB);
		BLOB_TYPES.add(Types.VARBINARY);
	}
	
	private ResultSetLogger(ResultSet rs, Log statemeneLog, int queryStack) {
		super(statemeneLog, queryStack);
		this.rs = rs;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			}
			Object o = method.invoke(rs, args);
			// 针对ResultSet.next()方法的处理
			if ("next".equals(method.getName())) {				
				if ((Boolean) o) {  // 是否还存在下一行数据
					rows++;
					// 如果测试环境调试代码时想让日志打印出每条记录的数据，需要开启trace级别的日志，一般开发都默认debug，避免数据太多爆日志
					if (isTraceEnabled()) {   
						ResultSetMetaData rsmd = rs.getMetaData();
						final int columnCount = rsmd.getColumnCount(); // 获取数据集的列数
						if (first) {  // 如果是第一行数据，则输出表头
							first = false;
							// 除了输出表头，还会填充blobColumns几个，记录超大类型的列
							printColumnHeaders(rsmd, columnCount);
						}
						printColumnValues(columnCount);
					}
				} else {
					debug("     Total: " + rows, false);
				}
			}
			clearColumnInfo();
			return o;
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}

	// 输出第一行数据的表头(显示查询结果集的每条记录中都包含了哪些字段)
	// eg: "   Columns: id, roleName, note"
	private void printColumnHeaders(ResultSetMetaData rsmd, int columnCount) throws SQLException {
		StringBuilder row = new StringBuilder();
		row.append("   Columns: ");
		for (int i = 1; i <= columnCount; i++) {
			if (BLOB_TYPES.contains(rsmd.getColumnType(i))) {
				blobColumns.add(i);
			}
			String colname = rsmd.getColumnLabel(i);
			row.append(colname);
			if (i != columnCount) {
				row.append(", ");
			}
		}
		trace(row.toString(), false);
	}
	
	// 输出列中的值(输出楼上方法表示的每个字段的值，对于超大类型的字段，会被屏蔽不打印值)
	// eg: "       Row: 1, assister, 助理"
	private void printColumnValues(int columnCount) {
		StringBuilder row = new StringBuilder();
		row.append("       Row: ");
		for (int i = 1; i < columnCount; i++) {
			String colname;
			try {
				if (blobColumns.contains(i)) {
					colname = "<<BLOB>>";
				} else {
					colname = rs.getString(i);
				}
			} catch (SQLException e) {
				colname = "<<Cannot Display>>";
			}
			row.append(colname);
			if (i != columnCount) {
				row.append(", ");
			}
		}
		trace(row.toString(), false);
	}	
	
	public static ResultSet newInstance(ResultSet rs, Log statemeneLog, int queryStack) {
		InvocationHandler handler = new ResultSetLogger(rs, statemeneLog, queryStack);
		ClassLoader cl = ResultSet.class.getClassLoader();
		return (ResultSet) Proxy.newProxyInstance(cl, new Class[] { ResultSet.class }, handler);
	}
	
	public ResultSet getRs() {
		return rs;
	}
}
