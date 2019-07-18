package org.apache.ibatis.datasource.pooled;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库连接池状态管理类
 */
public class PoolState {
	// 连接池关联的数据源
	protected PooledDataSource dataSource;
	
	// 空闲的PooledConnection集合
	protected final List<PooledConnection> idleConnections = new ArrayList<PooledConnection>();
	// 活跃的PooledConnection集合
	protected final List<PooledConnection> activeConnections = new ArrayList<PooledConnection>();
	
	protected long requestCount = 0;            // 请求数据库连接的次数
	
	protected long accumulatedRequestTime = 0;  // 获取连接的累积时间
	
	// checkoutTime表示应用从连接池中取出连接，到归还连接这段时长，
	// accumulatedRequestTime记录了所有连接累积的checkoutTime时长
	protected long accumulatedCheckoutTime = 0;
	
	// 当连接长时间未归还给连接池时，会被认为该连接超时，
	// claimedOverdueConnectionCount记录了超时的连接个数
	protected long claimedOverdueConnectionCount = 0;
	
	protected long accumulatedCheckoutTimeOfOverdueConnections = 0;  // 累计超时时间
	
	protected long accumulatedWaitTime = 0;     // 累积阻塞等待时间
	
	protected long hadToWaitCount = 0;          // 阻塞等待的连接数
	
	protected long badConnectionCount = 0;      // 无效的连接数	
	
	public PoolState(PooledDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	// 获取当前数据源的累计连接请求次数
	public synchronized long getRequestCount() {
		return requestCount;
	}
	
	// 获取数据库连接的平均请求时间
	public synchronized long getAverageRequestTime() {
		return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
	}
	
	// 获取当前数据源的累计连接等待次数
	public synchronized long getHadToWaitCount() {
		return hadToWaitCount;
	}
	
	// 获取数据库连接的平均等待时间
	public synchronized long getAverageWaitTime() {
		return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;
	}	
	
	// 获取当前数据源的累计无效连接数
	public synchronized long getBadConnectionCount() {
		return badConnectionCount;
	}
	
	// 获取当前数据源的累计连接使用超时数
	public synchronized long getClaimedOverdueConnectionCount() {
		return claimedOverdueConnectionCount;
	}
	
	// 获取数据库连接超时的平均超时时间
	public synchronized long getAverageOverdueCheckoutTime() {
		return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
	}
	
	// 获取数据库连接的平均使用时间
	public synchronized long getAverageCheckoutTime() {
		return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
	}
	
	// 获取当前数据库连接池中空闲的连接数
	public synchronized int getIdleConnectionCount() {
		return idleConnections.size();
	}
	
	// 获取当前数据库连接池中活跃的连接数
	public synchronized int getActiveConnectionCount() {
		return activeConnections.size();
	}
	
	// 打印数据库连接池的状态信息
	@Override
	public synchronized String toString() {
	    StringBuilder builder = new StringBuilder();
	    builder.append("\n===CONFINGURATION==============================================");
	    builder.append("\n jdbcDriver                     ").append(dataSource.getDriver());
	    builder.append("\n jdbcUrl                        ").append(dataSource.getUrl());
	    builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
	    builder.append("\n jdbcPassword                   ").append((dataSource.getPassword() == null ? "NULL" : "************"));
	    builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
	    builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
	    builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
	    builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
	    builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
	    builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
	    builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
	    builder.append("\n ---STATUS-----------------------------------------------------");
	    builder.append("\n activeConnections              ").append(getActiveConnectionCount());
	    builder.append("\n idleConnections                ").append(getIdleConnectionCount());
	    builder.append("\n requestCount                   ").append(getRequestCount());
	    builder.append("\n averageRequestTime             ").append(getAverageRequestTime());
	    builder.append("\n averageCheckoutTime            ").append(getAverageCheckoutTime());
	    builder.append("\n claimedOverdue                 ").append(getClaimedOverdueConnectionCount());
	    builder.append("\n averageOverdueCheckoutTime     ").append(getAverageOverdueCheckoutTime());
	    builder.append("\n hadToWait                      ").append(getHadToWaitCount());
	    builder.append("\n averageWaitTime                ").append(getAverageWaitTime());
	    builder.append("\n badConnectionCount             ").append(getBadConnectionCount());
	    builder.append("\n===============================================================");
	    return builder.toString();
	}		
}
