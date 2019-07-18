package org.apache.ibatis.logging.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.logging.Log;

/**
 * JDBC调试日志抽象基类
 * mybatis创建JDBC的Connection、Statement、PreparedStatement、ResultSet等对象时，
 * 实际上是针对这些JDBC常用类创建了一个动态代理，并在动态代理类中嵌入了Logger，当调用这些JDBC对象的方
 * 法时，实际会进入动态代理类的invoke方法中，在invoke方法中实际调用JDBC对象的方法的前后会调用Logger
 * 打印对应的SQL日志，日志级别可调整，起到在测试阶段debug调试的目的
 */
public abstract class BaseJdbcLogger {
	
	// 记录了PreparedStatement接口中定义的常用的set*()方法
	protected static final Set<String> SET_METHODS = new HashSet<String>();
	
	// 记录了Statement接口和PreparedStatement接口中与执行SQL语句相关的方法
	protected static final Set<String> EXECUTE_METHODS = new HashSet<String>();
	
	// 记录了PreparedStatement.set*()方法设置的键值对
	private Map<Object, Object> columnMap = new HashMap<Object, Object>();
	
	// 记录了PreparedStatement.set*()方法设置的key值
	private List<Object> columnNames = new ArrayList<Object>();
	
	// 记录了PreparedStatement.set*()方法设置的value值
	private List<Object> columnValues = new ArrayList<Object>();
	
	protected Log statementLog;  // 用于输出日志的Log对象
	protected int queryStack;    // 记录了SQL的层数，用于格式化输出SQL
	
	public BaseJdbcLogger(Log log, int queryStack) {
		this.statementLog = log;
		if (queryStack == 0) {
			this.queryStack = 1;
		} else {
			this.queryStack = queryStack;
		}
	}
	
	static {
		SET_METHODS.add("setString");
		SET_METHODS.add("setNString");
	    SET_METHODS.add("setInt");
	    SET_METHODS.add("setByte");
	    SET_METHODS.add("setShort");
	    SET_METHODS.add("setLong");
	    SET_METHODS.add("setDouble");
	    SET_METHODS.add("setFloat");
	    SET_METHODS.add("setTimestamp");
	    SET_METHODS.add("setDate");
	    SET_METHODS.add("setTime");
	    SET_METHODS.add("setArray");
	    SET_METHODS.add("setBigDecimal");
	    SET_METHODS.add("setAsciiStream");
	    SET_METHODS.add("setBinaryStream");
	    SET_METHODS.add("setBlob");
	    SET_METHODS.add("setBoolean");
	    SET_METHODS.add("setBytes");
	    SET_METHODS.add("setCharacterStream");
	    SET_METHODS.add("setNCharacterStream");
	    SET_METHODS.add("setClob");
	    SET_METHODS.add("setNClob");
	    SET_METHODS.add("setObject");
	    SET_METHODS.add("setNull");

	    EXECUTE_METHODS.add("execute");
	    EXECUTE_METHODS.add("executeUpdate");
	    EXECUTE_METHODS.add("executeQuery");
	    EXECUTE_METHODS.add("addBatch");
	}
	
	protected void setColumn(Object key, Object value) {
		columnMap.put(key, value);
		columnNames.add(key);
		columnValues.add(value);
	}
	
	protected Object setColumn(Object key) {
		return columnMap.get(key);
	}
	
	// 参数值类型字符串
	protected String getParameterValueString() {
		List<Object> typeList = new ArrayList<Object>(columnValues.size());
		for (Object value : columnValues) {
			if (value == null) {
				typeList.add("null");
			} else {
				typeList.add(value + "(" + value.getClass().getSimpleName() + ")");
			}
		}
		final String parameters = typeList.toString(); // eg: [null, 10(Integer), test(String)]
		return parameters.substring(1, parameters.length() - 1);
	}
	
	protected String getColumnString() {
		return columnNames.toString();
	}
	
	protected void clearColumnInfo() {
		columnMap.clear();
		columnNames.clear();
		columnValues.clear();
	}
	
	/**
	 * 格式化SQL日志输出的内容，需要用到StringTokenizer去除字符串中多余的空格
	 * 
	 * eg:
	 * select *  from  table
	 * where cloumn = ?;
	 * 格式化为: select * from table where cloumn = ?; 
	 * 
	 */
	protected String removeBreakingWhitespace(String original) {
		// StringTokenizer的作用就是以空格作为分隔，将字符串分隔出来，并且过滤掉中间多余的空格，使用的语法有点类型迭代器
		StringTokenizer whitespaceStripper = new StringTokenizer(original);
		StringBuilder builder = new StringBuilder();
		while (whitespaceStripper.hasMoreTokens()) {
			builder.append(whitespaceStripper.nextToken());
			builder.append(" ");
		}
		return builder.toString();
	}
	
	protected boolean isDebugEnabled() {
		return statementLog.isDebugEnabled();
	}
	
	protected boolean isTraceEnabled() {
		return statementLog.isTraceEnabled();				
	}
	
	// text是日志实际要打印的内容，input表示是输入还是输出
	protected void debug(String text, boolean input) {
		if (statementLog.isDebugEnabled()) {
			statementLog.debug(prefix(input) + text);
		}
	}
	
	protected void trace(String text, boolean input) {
		if (statementLog.isTraceEnabled()) {
			statementLog.trace(prefix(input) + text);
		}
	}
	
	/**
	 * 用于格式化输出日志的前缀
	 * 
	 * eg: 
	 * ==>  Preparing: select column1, column2 from table where column3 = ? and column4 = ?;
	 * ==> Parameters: 1(Integer), 10(String)
	 * <==      Total: 0
	 * 
	 * 当isInput为true时，返回形如 "==> " 的字符串
	 * 当isInput为false时，返回形如 "<== " 的字符串
	 * 
	 * queryStack代表SQL查询层次，因为经常有多层子查询嵌套的情况
	 * 假如是第二层查询，则返回 "====> " / "<==== "，更多层数查询以此类推，不过一般不建议嵌套太多层 
	 */
	private String prefix(boolean isInput) {
		char[] buffer = new char[queryStack * 2 + 2];
		Arrays.fill(buffer, '=');
		buffer[queryStack * 2 + 1] = ' ';
		if (isInput) {
			buffer[queryStack * 2] = '>';
		} else {
			buffer[0] = '<';
		}
		return new String(buffer);
	}
}
