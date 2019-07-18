package org.apache.ibatis.datasource.unpooled;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 不带连接池的数据源工厂
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

	// 数据库驱动属性前缀
	private static final String DRIVER_PROPERTY_PREFIX = "driver.";
	// 数据库驱动属性前缀长度
	private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();
	
	protected DataSource dataSource;
	
	public UnpooledDataSourceFactory() {
		this.dataSource = new UnpooledDataSource();
	}	
	
	@Override
	public void setProperties(Properties properties) {
		Properties driverProperties = new Properties();
		MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
		for (Object key : properties.keySet()) {
			String propertyName = (String) key;			
			if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
				String value = properties.getProperty(propertyName);
				driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
			} else if (metaDataSource.hasSetter(propertyName)) {
				String value = properties.getProperty(propertyName);
				// 将属性值类型进行类型转换，主要是Integer、Long、Boolean三种类型的转换
				Object convertedValue = convertValue(metaDataSource, propertyName, value);
				metaDataSource.setValue(propertyName, convertedValue);				
			} else {
				throw new DataSourceException("Unknown DataSource property: " + propertyName);
			}
		}
		// 如果设置的是数据库驱动的属性，则将其挑选出来设置 UnpooledDataSource 的 driverProperties 属性
		if (driverProperties.size() > 0) {
			metaDataSource.setValue("driverProperties", driverProperties);
		}
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	private Object convertValue(MetaObject metaSource, String propertyName, String value) {
		Object convertedValue = value;
		Class<?> targetType = metaSource.getSetterType(propertyName);
		if (targetType == Integer.class || targetType == int.class) {
			convertedValue = Integer.valueOf(value);
		} else if (targetType == Long.class || targetType == long.class) {
			convertedValue = Long.valueOf(value);
		} else if (targetType == Boolean.class || targetType == boolean.class) {
			convertedValue = Boolean.valueOf(value);
		}
		return convertedValue;
	}	
}
