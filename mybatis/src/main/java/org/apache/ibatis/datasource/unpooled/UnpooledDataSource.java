package org.apache.ibatis.datasource.unpooled;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;

/**
 *  不带连接池的数据源
 */
public class UnpooledDataSource implements DataSource {

	private ClassLoader driverClassLoader;  // 加载 Driver 类的类加载器
	private Properties driverProperties;    // 数据库连接驱动的相关配置
	// 缓存所有已注册(DriverManager)的数据库连接驱动
	private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<String, Driver>();
	
	// 驱动属性配置
	private String driver;    // 数据库连接的驱动名称
	private String url;       // 数据库URL
	private String username;  // 用户名
	private String password;  // 密码
	
	private Boolean autoCommit;  // 是否自动提交
	private Integer defaultTransactionIsolationLevel;  // 事务隔离级别
	
	// 静态代码块，在类加载的时候就将DriverManager中注册的JDBC Driver复制一份到registeredDrivers集合中
	static {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			registeredDrivers.put(driver.getClass().getName(), driver);
		}
	}
	
	public UnpooledDataSource() {}
	
	public UnpooledDataSource(String driver, String url, String username, String password) {
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
	}
	
	public UnpooledDataSource(String driver, String url, Properties driverProperties) {
		this.driver = driver;
		this.url = url;
		this.driverProperties = driverProperties;
	}
	
	public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
		this.driverClassLoader = driverClassLoader;
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
	}	
	
	public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
		this.driverClassLoader = driverClassLoader;
		this.driver = driver;
		this.url = url;
		this.driverProperties = driverProperties;
	}		
	
	@Override
	public Connection getConnection() throws SQLException {
		return doGetConnection(username, password);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return doGetConnection(username, password);
	}		
	
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return DriverManager.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		DriverManager.setLogWriter(out);		
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
	
	// Getter & Setter
	// [start]
	public ClassLoader getDriverClassLoader() {
		return driverClassLoader;
	}

	public void setDriverClassLoader(ClassLoader driverClassLoader) {
		this.driverClassLoader = driverClassLoader;
	}

	public Properties getDriverProperties() {
		return driverProperties;
	}

	public void setDriverProperties(Properties driverProperties) {
		this.driverProperties = driverProperties;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(Boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	public Integer getDefaultTransactionIsolationLevel() {
		return defaultTransactionIsolationLevel;
	}

	public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
		this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
	}
	// [end]

	private Connection doGetConnection(String username, String password) throws SQLException {
		Properties props = new Properties();
		if (driverProperties != null) {
			props.putAll(driverProperties);
		}
		if (username != null) {
			props.setProperty("user", username);
		}
		if (password != null) {
			props.setProperty("password", password);
		}
		return doGetConnection(props);
	}
	
	private Connection doGetConnection(Properties properties) throws SQLException {
		initializeDriver();
		long st = System.currentTimeMillis();
		Connection connection = DriverManager.getConnection(url, properties);
		System.out.println("创建一个数据库连接花费了:" + (System.currentTimeMillis() - st) + "ms!");
		configureConncetion(connection);
		return connection;
	}	
	
	// 初始化驱动，如果尚未注册则注册驱动并对其初始化，枷锁保证线程安全
	private synchronized void initializeDriver() throws SQLException {
		if (!registeredDrivers.containsKey(driver)) { // 检测驱动是否已注册
			Class<?> driverType;
			try {
				if (driverClassLoader != null) {
					driverType = Class.forName(driver, true, driverClassLoader);
				} else {
					driverType = Resources.classForName(driver);
				}
				// 创建driver对象
				Driver driverInstance = (Driver) driverType.newInstance();
				// 注册驱动
				DriverManager.registerDriver(new DriverProxy(driverInstance));
				registeredDrivers.put(driver, driverInstance);
			} catch (Exception e) {
				throw new SQLException("Error setting driver on UnpolledDataSource. Cause: " + e);
			}
		}
	}	
	
	// 配置数据库连接参数
	private void configureConncetion(Connection conn) throws SQLException {
		// 设置事务是否自动提交
		if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
			conn.setAutoCommit(autoCommit);
		}
		// 设置事务隔离级别
		if (defaultTransactionIsolationLevel != null) {
			conn.setTransactionIsolation(defaultTransactionIsolationLevel);
		}
	}
	
	// Driver的静态代理类（貌似没有做什么特殊的处理，为什么要代理？）
	private static class DriverProxy implements Driver {

		private Driver driver;
		
		DriverProxy(Driver d) {
			this.driver = d;
		}
		
		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			return this.driver.connect(url, info);
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return this.driver.acceptsURL(url);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return this.driver.getPropertyInfo(url, info);
		}

		@Override
		public int getMajorVersion() {
			return this.driver.getMajorVersion();
		}

		@Override
		public int getMinorVersion() {
			return this.driver.getMinorVersion();
		}

		@Override
		public boolean jdbcCompliant() {
			return this.driver.jdbcCompliant();
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}		
	}
}
