package org.apache.ibatis.datasource.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

public class UnpooledDataSourceTest {
	public static void main(String[] args) {
		try {
			//String pingSql = "select * from test_datasource";
			String pingSql = "insert into test_datasource (username, pwd) values('root4', 'root4')";
			//String pingSql = "insert into test_datasource (username, pwd) values(?, ?)";
			
			DataSourceFactory dataSourceFactory = new UnpooledDataSourceFactory();
			Properties properties = new Properties();
			properties.put("driver", "com.mysql.jdbc.Driver");
			properties.put("url", "jdbc:mysql://localhost:3306/mybatis?useSSL=false");
			properties.put("username", "root");
			properties.put("password", "root");
			dataSourceFactory.setProperties(properties);
			DataSource dataSource = dataSourceFactory.getDataSource();
			Connection connection = dataSource.getConnection();
			Statement stmt = connection.createStatement();
			//ResultSet rs = stmt.executeQuery(pingSql);
			int row = stmt.executeUpdate(pingSql);
			
			/*PreparedStatement stmt = connection.prepareStatement(pingSql);
			stmt.setString(1, "root5");
			stmt.setString(2, "root5");
			int row = stmt.executeUpdate();*/
			
			System.out.println("insert success, row = " + row);
			System.out.println("Connection " + connection + "is good!");

			printInfo(dataSource, connection);
			//rs.close();
			stmt.close();
			connection.close();			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void printInfo(DataSource dataSource, Connection conn) throws SQLException {
		if (dataSource instanceof UnpooledDataSource) {
			UnpooledDataSource ds = (UnpooledDataSource) dataSource;
			System.out.println("driver: " + ds.getDriver());
			System.out.println("url: " + ds.getUrl());
			System.out.println("username: " + ds.getUsername());
			System.out.println("password: " + ds.getPassword());			
		}
		System.out.println("autoCommit ? " + conn.getAutoCommit());
		System.out.println("transactionIsolationLevel: " + conn.getTransactionIsolation());
	}
}
