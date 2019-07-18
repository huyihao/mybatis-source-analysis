package org.apache.ibatis.datasource.pooled;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

// 继承了非连接池数据源工厂，因为PooledDataSource实际上是对UnpooledDataSource的包装使用
public class PooledDataSourceFactory extends UnpooledDataSourceFactory {
	
	public PooledDataSourceFactory() {
		this.dataSource = new PooledDataSource();
	}
	
}
