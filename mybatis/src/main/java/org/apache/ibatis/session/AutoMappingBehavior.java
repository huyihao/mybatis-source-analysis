package org.apache.ibatis.session;

/**
 * 指定MyBatis应如何自动映射到字段或属性
 */
public enum AutoMappingBehavior {
	/**
	 * 不允许自动映射
	 */
	NONE,
	/**
	 * 只自动映射不在内部定义的结果映射
	 */
	PARTIAL,
	/**
	 * 完全自动映射（包含内部定义）
	 */
	FULL
}
