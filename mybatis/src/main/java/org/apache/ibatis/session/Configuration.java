package org.apache.ibatis.session;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;

public class Configuration {

	public MappedStatement getMappedStatement(String id) {
		return null;
	}
	
	// 根据SQL语句的名称，检测配置是否有加载该SQL
	public boolean hasStatement(String statementName) {
		return true;
	}
	
	public boolean isUseActualParamName() {
		return true;
	}
	
	public ObjectFactory getObjectFactory() {
		return null;
	}
	
	public MetaObject newMetaObject(Object object) {
		return null;
	}
}
