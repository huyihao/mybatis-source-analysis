package org.apache.ibatis.session;

/**
 * MyBatis内置的查询分页组件
 */
public class RowBounds {
	public static final int NO_ROW_OFFSET = 0;
	public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;
	public static final RowBounds DEFAULT = new RowBounds();
	
	private int offset;  // 数据偏移量
	private int limit;   // 每次取数条数限制
	
	public RowBounds() {
		this.offset = NO_ROW_OFFSET;
		this.limit = NO_ROW_LIMIT;
	}
	
	public RowBounds(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLimit() {
		return limit;
	}
}
