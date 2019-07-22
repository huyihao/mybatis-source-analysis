package org.apache.ibatis.mapping;

import java.sql.ResultSet;

/**
 * 定义了ResultSet类型的枚举类
 * 分别是只允许向前移动的类型、允许滑动不敏感类型、允许滑动敏感类型
 */
public enum ResultSetType {
	FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY),
	SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE),
	SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);
	
	private int value;
	
	ResultSetType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
