package org.apache.ibatis.datasource;

import java.util.Properties;
import javax.sql.DataSource;

public interface DataSourceFactory {
	// 设置DataSource的相关属性，一般紧跟在初始化完成之后
	void setProperties(Properties props);
	
	// 获取DataSource对象
	DataSource getDataSource();
}
