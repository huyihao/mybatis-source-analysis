package org.apache.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * 一般SQL中都需要传参设置查询筛选条件，这时就需要用到PreparedStatement对象来设置SQL中占位符'?'对应的参数类型和值
 * PreparedStatement在设置完参数之后执行SQL，接着获取结果集
 * PreparedStatementLogger为上述这些方法执行时代理打印日志
 */
public class PreparedStatementLogger extends BaseJdbcLogger implements InvocationHandler {

	private PreparedStatement statement;
	
	private PreparedStatementLogger(PreparedStatement stmt, Log statementLog, int queryStack) {
		super(statementLog, queryStack);
		this.statement = stmt;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, params);
			}
			// 判断反射调用的方法是否执行SQL语句的方法
			if (EXECUTE_METHODS.contains(method.getName())) {
				if (isDebugEnabled()) {
					// 日志输出参数值和参数类型
					debug("Parameters: " + getParameterValueString(), true);
				}
				clearColumnInfo();  // 清空BaseJdbcLogger中定义的三个column*集合
				if ("executeQuery".equals(method.getName())) {
					// 如果调用的是executeQuery()方法，则为ResultSet创建代理对象
					ResultSet rs = (ResultSet) method.invoke(statement, params);
					return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
				} else {
					// 不是executeQuery()方法则直接返回结果
					return method.invoke(statement, params);
				}
			}
			// 判断反射调用的方法是否PreparedStatement接口中定义的常用的set*()方法
			else if (SET_METHODS.contains(method.getName())) {
				if ("setNull".equals(method.getName())) {
					setColumn(params[0], null);
				} else {
					setColumn(params[0], params[1]);
				}
				return method.invoke(statement, params);
			}			
			else if ("getResultSet".equals(method.getName())) {
				// 如果调用getResultSet方法，则为ResultSet创建代理对象
				ResultSet rs = (ResultSet) method.invoke(statement, params);
				return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
			}			
			else if ("getUpdateCount".equals(method.getName())) {
				// 如果调用getUpdateCount()方法，则通过日志框架输出其结果
				int updateCount = (Integer) method.invoke(statement, params);
				if (updateCount != -1) {
					debug("  Updates: " + updateCount, false);
				}
				return updateCount;
			}		
	        else {
	            return method.invoke(statement, params);
	        }			
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}

	public static PreparedStatement newInstance(PreparedStatement stmt, Log statementLog, int queryStack) {
		InvocationHandler handler = new PreparedStatementLogger(stmt, statementLog, queryStack);
		ClassLoader cl = PreparedStatement.class.getClassLoader();
		return (PreparedStatement) Proxy.newProxyInstance(cl, new Class[] { PreparedStatement.class, CallableStatement.class }, handler);
	}
	
	public PreparedStatement getPreparedStatement() {
		return statement;
	}
}
