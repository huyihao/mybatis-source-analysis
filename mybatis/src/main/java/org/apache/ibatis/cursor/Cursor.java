package org.apache.ibatis.cursor;

import java.io.Closeable;

/**
 * 游标接口
 */
public interface Cursor<T> extends Closeable, Iterable<T> {
	
	boolean isOpen();
	
	boolean isConsumed();
	
	int getCurrentIndex();
}
