package org.apache.ibatis.datasource.pooled;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 带连接池的数据源
 */
public class PooledDataSource implements DataSource {

	private static final Log log = LogFactory.getLog(PooledDataSource.class);
	
	// 通过PoolState管理连接池的状态并记录统计信息
	private final PoolState state = new PoolState(this);
	
	// 记录UnpooledDataSource对象，用于生成真实的数据库连接对象，构造函数中会初始化该字段
	private final UnpooledDataSource dataSource;
	
	protected int poolMaximumActiveConnections = 10;        // 最大活跃连接数
	protected int poolMaximumIdleConnections = 5;           // 最大空闲连接数
	protected int poolMaximumCheckoutTime = 20000;          // 最大checkout时长: 取出连接到归还连接这段使用时间
	protected int poolTimeToWait = 20000;                   // 在无法获取连接时，线程需要等待的时间
	protected String poolPingQuery = "NO PING QUERY SET";  	// 在检测一个数据库连接是否可用时，会给数据库发送一个测试SQL语句
	protected boolean poolPingEnabled = false;              // 是否允许发送测试SQL语句
	protected int poolPingConnectionsNotUsedFor = 0;        // 当连接池超过多少毫秒未使用时，会发送一次测试SQL语句，检测连接是否正常
	
	private int expectedConnectionTypeCode;                 // 根据数据库的URL、用户名、密码生成的一个hash值，该哈希值用于标志着当前的数据库连接池，在构造函数中初始化	
	
	// 构造方法
	// [start]
	public PooledDataSource() {
		dataSource = new UnpooledDataSource();
	}
	
	public PooledDataSource(UnpooledDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public PooledDataSource(String driver, String url, String username, String password) {
		dataSource = new UnpooledDataSource(driver, url, username, password);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
	}
	
	public PooledDataSource(String driver, String url, Properties driverProperties) {
		dataSource = new UnpooledDataSource(driver, url, driverProperties);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());		
	}
	
	public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
		dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
	}	
	
	public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
		dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());		
	}	
	// [end]

	// 重写方法
	// [start]
	@Override
	public Connection getConnection() throws SQLException {
		return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return popConnection(username, password).getProxyConnection();
	}	
	
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		DriverManager.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {	
		return DriverManager.getLoginTimeout();
	}	
	
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		DriverManager.setLogWriter(out);
	}	
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {		
		return DriverManager.getLogWriter();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLException(getClass().getName() + " is not a wrapper");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}
	// [end]
	
	// Getters & Setters
	// 代理设置dataSource的驱动、URL、用户名、密码、是否自动提交、事务隔离级别、属性值等属性
	// [start]
	public void setDriver(String driver) {
		dataSource.setDriver(driver);
		forceCloseAll();
	}
	
	public void setUrl(String url) {
		dataSource.setUrl(url);
		forceCloseAll();
	}
	
	public void setUsername(String username) {
		dataSource.setUsername(username);
		forceCloseAll();
	}
	
	public void setPassword(String password) {
		dataSource.setPassword(password);
		forceCloseAll();
	}
	
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		dataSource.setAutoCommit(defaultAutoCommit);
		forceCloseAll();
	}
	
	public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
		dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
		forceCloseAll();
	}
	 
	public void setDefaultDriverProperties(Properties driverProps) {
		dataSource.setDriverProperties(driverProps);
		forceCloseAll();
	}
	// [end]	
	
	// 设置数据库连接池的最大连接数、最小连接数、最大连接时间、最大阻塞超时时间、心跳检测语句、是否开启心跳检测、心跳检测频率等属性
	// [start]
	public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
		this.poolMaximumActiveConnections = poolMaximumActiveConnections;
		forceCloseAll();
	}
	
	public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
		this.poolMaximumIdleConnections = poolMaximumIdleConnections;
		forceCloseAll();
	}
	
	public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
		this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
		forceCloseAll();
	}
	
	public void setPoolTimeToWait(int poolTimeToWait) {
		this.poolTimeToWait = poolTimeToWait;
		forceCloseAll();
	}	
	
	public void setPoolPingQuery(String poolPingQuery) {
		this.poolPingQuery = poolPingQuery;
		forceCloseAll();
	}
	
	public void setPoolPingEnabled(boolean poolPingEnabled) {
		this.poolPingEnabled = poolPingEnabled;
		forceCloseAll();
	}
	
	public void setPoolPingConnectionsNotUsedFor(int poolPingConnectionsNotUsedFor) {
		this.poolPingConnectionsNotUsedFor = poolPingConnectionsNotUsedFor;
		forceCloseAll();
	}	
	// [end]
	
	// 上述属性值获取
	// [start]
	public String getDriver() {
		return dataSource.getDriver();
	}
	
	public String getUrl() {
		return dataSource.getUrl();
	}
	
	public String getUsername() {
	    return dataSource.getUsername();
	}

    public String getPassword() {
	    return dataSource.getPassword();
	}

	public boolean isAutoCommit() {
	    return dataSource.isAutoCommit();
	}
	
	public Integer getDefaultTransactionIsolationLevel() {
	    return dataSource.getDefaultTransactionIsolationLevel();
	}

	public Properties getDriverProperties() {
	    return dataSource.getDriverProperties();
	}
	
	public int getPoolMaximumActiveConnections() {
	    return poolMaximumActiveConnections;
	}

	public int getPoolMaximumIdleConnections() {
	    return poolMaximumIdleConnections;
	}

	public int getPoolMaximumCheckoutTime() {
	    return poolMaximumCheckoutTime;
	}

	public int getPoolTimeToWait() {
	    return poolTimeToWait;
	}	
	
	public String getPoolPingQuery() {
		return poolPingQuery;
	}
	
	public boolean isPoolPingEnabled() {
		return poolPingEnabled;
	}

	public int getPoolPingConnectionsNotUsedFor() {
		return poolPingConnectionsNotUsedFor;
	}	
	// [end]

	public PoolState getPoolState() {
		return state;
	}	
	
	// 当修改时PooledDataSource的字段时，会调用本方法将连接池中的连接置为无效
	public void forceCloseAll() {
		synchronized (state) {
			expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
			// 对活跃连接逐个处理清空
			for (int i = 0; i < state.activeConnections.size(); i++) {
				try {
					PooledConnection conn = state.activeConnections.remove(i);
					conn.invalidate();
					
					Connection realConn = conn.getRealConnection();
					if (!realConn.getAutoCommit()) {
						realConn.rollback();
					}
					realConn.close();
				} catch (Exception e) {
					// ignore
				}
			}
			// 对失败连接逐个处理清空
			for (int i = 0; i < state.idleConnections.size(); i++) {
				try {
					PooledConnection conn = state.idleConnections.remove(i);
					conn.invalidate();
					
					Connection realConn = conn.getRealConnection();
					if (!realConn.getAutoCommit()) {
						realConn.rollback();
					}
					realConn.close();
				} catch (Exception e) {
					// ignore
				}
			}			
		}
		if (log.isDebugEnabled()) {
			log.debug("PooledDataSource forcefully closed/removed all connections.");
		}		
	}

	private int assembleConnectionTypeCode(String url, String username, String password) {
		return ("" + url + username + password).hashCode();
	}	
	
	// PooledConnection关闭数据库连接时，调用本方法，将活跃连接置为空闲连接
	// 注意: 连接在活跃和空闲之间转换时，不是通过重置PooledConnection对象来实现的，而是通过拿到对象中真正原始的Connection对象，并用它来创建一个新的PooledConnection
	protected void pushConncetion(PooledConnection conn) throws SQLException {
		synchronized(state) {
			// 从activeConnections集合中移除该PooledConnection对象
			state.activeConnections.remove(conn);
			if (conn.isValid()) {
				// 检测空闲连接数是否已达上线，是则真正关闭连接，否则放入空闲连接
				if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
					state.accumulatedCheckoutTime += conn.getCheckoutTime();
					if (!conn.getRealConnection().getAutoCommit()) {
						conn.getRealConnection().rollback();
					}
					PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
					state.idleConnections.add(newConn);				
					newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
					newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
					conn.invalidate();
					if (log.isDebugEnabled()) {
						log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
					}
					state.notifyAll();     // 唤醒所有阻塞的获取新连接的请求
				} else {
					// 空闲连接数达到上线或PooledConnection对象并不属于该连接池
					state.accumulatedCheckoutTime += conn.getCheckoutTime();  // 累计checkout时长
					if (!conn.getRealConnection().getAutoCommit()) {
						conn.getRealConnection().rollback();
					}
					conn.getRealConnection().close();  // 真正关闭数据库连接
					if (log.isDebugEnabled()) {
						log.debug("Close connection " + conn.getRealHashCode() + ".");
					}
					conn.invalidate();  // 将PooledConnection对象设置为无效					
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
				}
				// 是否会跟popConnection中的失效连接出现同一个连接在不同方法中重复统计的情况？
				// 不存在！因为统计失效连接时该连接已经被移出连接池
				state.badConnectionCount++;  // 统计无效PooledConnection对象个数				
			}
		}
	}
	
	/**
	 * 获取PooledConnection对象
	 * 总体流程:
	 * 1. 从空闲连接池中获取，如果有则拿到一条连接，并加入活跃连接池中
	 * 2. 如果空闲线程池为空，并且活跃连接池还没满，则直接创建一个新的连接
	 * 3. 如果活跃连接池已满，则先尝试从活跃连接池中回收已经超时的使用最久的连接，如果可回收则回收
	 * 4. 如果使用最久的活跃线程都还没超时，则需要阻塞等待（此时不允许创建新的连接）
	 */
	private PooledConnection popConnection(String username, String password) throws SQLException {
		boolean countedWait = false;
		PooledConnection conn = null;
		long t = System.currentTimeMillis();
		int localBadConnectionCount = 0;   // 用来记录获取连接时尝试的次数，如果获取到无效连接的次数 > (最大空闲连接数 + 3)，则抛出异常
		
		while (conn == null) {
			synchronized (state) {  // 同步
				// 如果连接池有空闲连接，则从空闲连接中取一个空闲连接使用即可
				if (!state.idleConnections.isEmpty()) {  
					// 如果有可用空闲连接则从空闲连接池中移除第一个连接
					conn = state.idleConnections.remove(0);
					if (log.isDebugEnabled()) {
						log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
					}					
				} else {
					/**
					 * 当空闲连接池没有可用的连接时
					 * 1) 如果当前活跃连接池还没满，则创建一个新的连接并加入到活跃连接池中
					 * 2) 如果当前活跃连接池已满，则先判断最早创建的活跃连接是否已经超时了，如果超时了则回收利用它，否则阻塞连接创建请求
					 */
					if (state.activeConnections.size() < poolMaximumActiveConnections) {
						conn = new PooledConnection(dataSource.getConnection(), this);
						if (log.isDebugEnabled()) {
							log.debug("Created connection " + conn.getRealHashCode() + ".");
						}
					} else {
						PooledConnection oldestActiveConnection = state.activeConnections.get(0);
						long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
						if (longestCheckoutTime > poolMaximumCheckoutTime) {
							/**
							 * 当检测到一个活跃连接超时时
							 * 连接池状态的连接超时数+1
							 * 累计超时时间增加（这里我觉得累加的时间应该是longestCheckoutTime - poolMaximumCheckoutTime）
							 * 累计使用连接时间增加
							 */
							state.claimedOverdueConnectionCount++;
							state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
							state.accumulatedCheckoutTime += longestCheckoutTime;
							state.activeConnections.remove(oldestActiveConnection);
							if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
								try {
									oldestActiveConnection.getRealConnection().rollback();
								} catch (SQLException e) {
									log.debug("Bad connection. Could not roll back");
								}
							}
							conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
							oldestActiveConnection.invalidate();   // 标记为已失效
							if (log.isDebugEnabled()) {
								log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
							}
						} else {
							try {
								if (!countedWait) {
									state.hadToWaitCount++;
									countedWait = true;
								}
								if (log.isDebugEnabled()) {
									log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
								}
								long wt = System.currentTimeMillis();
								state.wait(poolTimeToWait);   // 阻塞创建新的连接的请求，等待pushConnection唤醒所有等待的线程，唤醒之后进入新的循环
								state.accumulatedWaitTime += System.currentTimeMillis() - wt;
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}
				// 将新创建或移除的空闲连接加入活跃连接集合中
				if (conn != null) {
					if (conn.isValid()) {
						if (!conn.getRealConnection().getAutoCommit()) {
							conn.getRealConnection().rollback();   // 不是自动提交的，则把还没提交的先回滚一下
						}
						conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
						conn.setCheckoutTimestamp(System.currentTimeMillis());
						conn.setLastUsedTimestamp(System.currentTimeMillis());
						state.activeConnections.add(conn);
						state.requestCount++;
						state.accumulatedRequestTime += System.currentTimeMillis() - t;  // 计算从空闲连接池中拿到或创建一个新的连接消耗的时间				
					} else {
						if (log.isDebugEnabled()) {
							log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
						}
						state.badConnectionCount++;
						localBadConnectionCount++;
						conn = null;   // 重置为null，一边进入下一个循环接着尝试获取或创建连接
						if (localBadConnectionCount > (poolMaximumIdleConnections + 3)) {
							if (log.isDebugEnabled()) {
								log.debug("PooledDataSource: Could not get a good connection to the database.");
							}
							throw new SQLException("PooledDataSource: Could not a good connection to the database.");
						}
					}
				}
			}
		}
		
		if (conn == null) {
			if (log.isDebugEnabled()) {
				log.debug("PooledDataSource: Unknow server error condition. The connection pool returned a null connection.");
			}
			throw new SQLException("PooledDataSource: Unknow server error condition. The connection pool returned a null connection.");
		}
		
		return conn;
	}
	
	// 检测真正的数据库对象是否可以正常使用
	protected boolean pingConnection(PooledConnection conn) {
		boolean result = true;
		
		// 检测该数据库连接是否已被关闭
		try {
			result = !conn.getRealConnection().isClosed();			
		} catch (SQLException e) {
			if (log.isDebugEnabled()) {
				log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
			}
			result = false;
		}		
		
		if (result) {
			// 检测poolPingEnabled设置，是否运行执行测试SQL语句
			if (poolPingEnabled) {
				// 长时间(超过poolPingConnectionNotUserFor指定的时长)未使用的连接，才需要ping操作来检测数据库连接是否正常
				if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
					try {
						if (log.isDebugEnabled()) {
							log.debug("Testing connection " + conn.getRealHashCode() + " ...");
						}
						Connection realConn = conn.getRealConnection();
						Statement statement = realConn.createStatement();
						ResultSet rs = statement.executeQuery(poolPingQuery);
						rs.close();
						statement.close();
						if (!realConn.getAutoCommit()) {
							realConn.rollback();
						}
						result = true;
						if (log.isDebugEnabled()) {
							log.debug("Connection " + conn.getRealHashCode() + " is Good!");
						}
					} catch (Exception e) {  
						// 如果心跳检测失败会抛出异常
						log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
						try {
							conn.getRealConnection().close();
						} catch (Exception e2) {
							// ignore
						}
						result = false;
						if (log.isDebugEnabled()) {
							log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
						}
					}
				}
			}
		}
		return result;
	}	

	// 垃圾回收器准备释放内存的时候，会先调用finalize()
	// 这个是为了对象内存被回收时顺便清理线程池
	protected void finalize() throws Throwable {
		forceCloseAll();
		super.finalize();
	}
	
	// 从代理对象中获取真正的原始的Connection对象
	public static Connection unwrapConnection(Connection conn) {
		if (Proxy.isProxyClass(conn.getClass())) {
			InvocationHandler handler = Proxy.getInvocationHandler(conn);
			if (handler instanceof PooledConnection) {
				return ((PooledConnection) handler).getRealConnection();
			}
		}
		return conn;
	}
}
