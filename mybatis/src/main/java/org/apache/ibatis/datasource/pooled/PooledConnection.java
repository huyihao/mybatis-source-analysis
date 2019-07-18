package org.apache.ibatis.datasource.pooled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.reflection.ExceptionUtil;


/**
 * 数据库连接池连接管理，PooledConnection主要是对连接池中每个连接进行管理，包括:
 * 1) 检查数据库连接是否有效
 * 2) 计算数据库连接的创建时间、使用时间、取用时间
 * 3) 判断两个连接对象是否为同一个
 * 4) 拦截数据库连接的关闭方法，将连接返回给连接池处理
 */
public class PooledConnection implements InvocationHandler {

	private static final String CLOSE = "close";   // 执行关闭connect时的方法名
	private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };	
	
	private int hashCode = 0;            // 在对比两个数据库连接是否属于同一个时用到
	// 记录当前PooledConnection对象所在的PooledDataSource对象。该PooledConnection是从
	// 该PooledDataSource中获取的；当调用close()方法时会将PooledConnection放回该PooledConnection中，并不会实际关闭连接	
	private PooledDataSource dataSource;
	private Connection realConnection;   // 真正的数据库连接
	private Connection proxyConnection;  // 数据库连接的代理对象，在代理对象方法内拦截"close"方法
	// 下面三个时间戳主要用于判断数据库连接是否超时以及数据统计
	private long checkoutTimestamp;      // 从连接池中取出该连接的时间戳
	private long createdTimestamp;       // 该连接创建的时间戳
	private long lastUsedTimestamp;      // 最后一次被使用的时间戳
	// 由数据库URL、用户名和密码计算出来的hash值，可用于标识该连接所在的连接池
	private int connectionTypeCode;	     
	// 检测当前PooledConnection是否有效，主要是为了防止程序通过close方法将连接归还给连接池之后，依然通过该连接操作数据库导致出错
	private boolean valid;	
	
	public PooledConnection(Connection connection, PooledDataSource dataSource) {
		this.hashCode = connection.hashCode();
		this.dataSource = dataSource;
		this.realConnection = connection;
		this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
		this.createdTimestamp = System.currentTimeMillis();
		this.lastUsedTimestamp = System.currentTimeMillis();
		this.valid = true;		
	}
	
	public void invalidate() {
		valid = false;
	}
	
	public boolean isValid() {
		// 满足下面三个条件的数据库连接才能被认定为是有效的
		// 1) 有效状态位位 true
		// 2) 真正的数据库连接对象非空
		// 3) 检测数据库连接的ping心跳是否正常
		return valid && realConnection != null && dataSource.pingConnection(this);
	}
	
	public Connection getRealConnection() {
		return realConnection;
	}
	
	public Connection getProxyConnection() {
		return proxyConnection;
	}
	
	public int getRealHashCode() {
	    return realConnection == null ? 0 : realConnection.hashCode();
	}
	
	public int getConnectionTypeCode() {
	    return connectionTypeCode;
	}	
	
	public void setConnectionTypeCode(int connectionTypeCode) {
		this.connectionTypeCode = connectionTypeCode;
	}	
	
	public long getCreatedTimestamp() {
		return createdTimestamp;
	}
	
	public void setCreatedTimestamp(long createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}
		
	public long getLastUsedTimestamp() {
		return lastUsedTimestamp;
	}

	public void setLastUsedTimestamp(long lastUsedTimestamp) {
		this.lastUsedTimestamp = lastUsedTimestamp;
	}
	
	public long getCheckoutTimestamp() {
		return checkoutTimestamp;
	}

	public void setCheckoutTimestamp(long checkoutTimestamp) {
		this.checkoutTimestamp = checkoutTimestamp;
	}
	
	// 获取数据库连接的最后一次使用的已使用时间
	public long getTimeElapsedSinceLastUse() {
		return System.currentTimeMillis() - lastUsedTimestamp;
	}
	
	// 获取数据库连接的已创建时间
	public long getAge() {
		return System.currentTimeMillis() - createdTimestamp;
	}
	
	// 获取连接的已取出使用时间
	public long getCheckoutTime() {
		return System.currentTimeMillis() - checkoutTimestamp;
	}

	public static String getClose() {
		return CLOSE;
	}

	public static Class<?>[] getIfaces() {
		return IFACES;
	}
	
    @Override
    public int hashCode() {
    	return hashCode;
    }	
	
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof PooledConnection) {
    		return realConnection.hashCode() == ((PooledConnection) obj).realConnection.hashCode();
    	} else if (obj instanceof Connection) {
    		return hashCode == obj.hashCode();
    	} else {
    		return false;
    	}
    }	
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		// 当调用Connection的close()方法关闭数据库连接时，代理对象在此处拦截，将连接交给连接池处理
		// 如果连接池活跃数据库连接已满，则真正关闭连接，否则将连接放入空闲的连接池中
		if (CLOSE.hashCode() == method.hashCode() && CLOSE.equals(methodName)) {
			dataSource.pushConncetion(this);
			return null;
		} else {
			try {
				// 如果调用的是Connection里的方法，则先检测数据库连接是否已放回连接池
				if (!Object.class.equals(method.getDeclaringClass())) {
					checkConnection();
				}
				return method.invoke(realConnection, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		}
	}

	protected void checkConnection() throws SQLException {
		if (!valid) {
			throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
		}
	}	
}
