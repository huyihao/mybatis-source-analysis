package org.apache.ibatis.datasource.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;

public class PooledDataSourceTest {
	// 同时连接数据库的线程数，模拟并发条件下多个交易事务请求数据库连接的情况
	public static final int CONCURRENT_THREADS = 11;
	public static final String TRADE_SQL = "insert into test_datasource (username, pwd) values (?, ?)";
	public static AtomicInteger COUNT = new AtomicInteger(0);
	public static Connection[] conns = new Connection[CONCURRENT_THREADS];
	
	public static void main(String[] args) {
		try {
			DataSourceFactory dataSourceFactory = new PooledDataSourceFactory();
			Properties properties = new Properties();
			properties.put("driver", "com.mysql.jdbc.Driver");
			properties.put("url", "jdbc:mysql://localhost:3306/mybatis?useSSL=false");
			properties.put("username", "root");
			properties.put("password", "root");
			dataSourceFactory.setProperties(properties);
			DataSource dataSource = dataSourceFactory.getDataSource();
			
			Thread[] threads = new Thread[CONCURRENT_THREADS];
			for (int i = 0; i < CONCURRENT_THREADS; i++) {
				threads[i] = new Thread() {
					@Override
					public void run() {						
						try {						
							// 因为数据库活跃连接池默认最大连接数为10，在前面10个线程还没释放连接的情况下，第11个线程获取数据库连接时会阻塞，知道有线程释放数据库连接
							Connection conn = dataSource.getConnection();
							int index = COUNT.incrementAndGet();
							System.out.println("[Thread-" + index + " get connection success!]\n" + 
											   "数据库连接池实时状态:" + ((PooledDataSource) dataSource).getPoolState().toString());
							conns[index - 1] = conn;
							PreparedStatement stmt = conn.prepareStatement(TRADE_SQL);
							System.out.println("[Thread-" + index + " insert start!]");
							stmt.setString(1, "username inserted by thread-" + (index));
							stmt.setString(2, "password inserted by thread-" + (index));
							// get到了Connection之后实际上就在活跃连接池中新增一条连接					
							int row = stmt.executeUpdate();
							if (row == 1) 
								System.out.println("insert a data into test_datasource by thread-" + index + " successfully!");
							System.out.println("[Thread-" + index + " insert end!]");
							System.out.println();
							stmt.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}						
					}
				};
				threads[i].start();
			}
			
			Thread.sleep(10000);   // 休眠10秒后再释放每个线程的数据库连接，休眠期间10个已经拿到连接资源的线程应该已经执行完毕
			for (int i = 0; i < CONCURRENT_THREADS; i++) {
				Connection conn = conns[i];
				if (conn != null && !conn.isClosed()) {
					System.out.println("释放Thread-" + (i+1) + "的数据库连接");
					conn.close();					
				}
			}
			
			// 等待所有线程执行完毕
			for (int i = 0; i < CONCURRENT_THREADS; i++) {
				threads[i].join();
			}
			System.out.println("全部线程都已经连接数据库执行SQL完毕!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printInfo(DataSource dataSource, Connection conn) throws SQLException {
		if (conn instanceof PooledDataSource) {
			PooledDataSource ds = (PooledDataSource) dataSource;
			System.out.println("driver: " + ds.getDriver());
			System.out.println("url: " + ds.getUrl());
			System.out.println("username: " + ds.getUsername());
			System.out.println("password: " + ds.getPassword());		
		}
		System.out.println("autoCommit ? " + conn.getAutoCommit());
		System.out.println("transactionIsolationLevel: " + conn.getTransactionIsolation());
	}	
}
