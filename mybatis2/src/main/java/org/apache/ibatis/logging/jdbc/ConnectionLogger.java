package org.apache.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * Connection对象的代理和Logger
 */
public class ConnectionLogger extends BaseJdbcLogger implements InvocationHandler {

	private Connection connection;
	
	private ConnectionLogger(Connection connection, Log statementLog, int queryStack) {
		super(statementLog, queryStack);
		this.connection = connection;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
		try {
			// 如果调用的是从Object继承的方法，则直接调用，不做任何其他处理
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, params);
			}
			
			// 如果调用的是preparedStatement()方法、prepareCall()方法或createStatement()方法，
			// 则在创建相应的statement对象后，为其创建代理对象并返回该代理对象
			if ("prepareStatement".equals(method.getName())) {
				if (isDebugEnabled()) {
					debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
				}
				// 调用底层封装的Connection对象的prepareStatement()方法，得到PreparedStatement对象
				PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
				// 为该PreparedStatement对象创建代理对象
				// 为什么这里不直接返回还是接着代理？因为代理里执行方法前后还有处理会打日志！
				stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
				return stmt;
			} else if ("prepareCall".equals(method.getName())) {
				if (isDebugEnabled()) {
					debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
				}
				PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
				stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
				return stmt;
			} else if ("createStatement".equals(method.getName())) {
				Statement stmt = (Statement) method.invoke(connection, params);
				stmt = StatementLogger.newInstance(stmt, statementLog, queryStack);
				return stmt;
			} else {
				// 其他方法则直接调用底层Connection对象的相应方法
				return method.invoke(connection, params);
			}			
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}

	// 创建Connection的代理对象，当调用这个代理对象的方法时，都会先进入ConnectionLogger的invoke方法中
	public static Connection newInstance(Connection conn, Log statementLog, int queryStack) {
		InvocationHandler handler = new ConnectionLogger(conn, statementLog, queryStack);
		ClassLoader cl = Connection.class.getClassLoader();
		return (Connection) Proxy.newProxyInstance(cl, new Class[]{ Connection.class }, handler);
	}
	
	// 获取真正的数据库连接对象
	public Connection getConnection() {
		return connection;
	}
}
