package org.apache.ibatis.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;

/**
 * 使用MyBatis时用到的主要的Java接口
 * 通过这个接口可以执行命令、获取mappers和管理事务
 */
public interface SqlSession extends Closeable {
	/**
	 * 通过statement key取回一条数据
	 * 一般是使用唯一索引查询查询数据表时使用，不过这里没有用到参数，可能是写死在sql里
	 */
	<T> T selectOne(String statement);
	
	/**
	 * 通过statement key和参数取回一条数据
	 * 一般是使用唯一索引字段查询，statement里使用了参数，所以这里要传参
	 */
	<T> T selectOne(String statement, Object parameter);
	
	// 查询数据表获得包含多条数据的一组数据，不使用参数
	<E> List<E> selectList(String statement);
	
	// 查询数据表获得包含多条数据的一组数据，使用参数
	<E> List<E> selectList(String statement, Object parameter);
	
	// 使用参数分页查询查询数据表获得包含多条数据的一组数据
	<E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);
	
	/**
	 * selectMap是一个特殊的例子，被设计用来将一组数据结果转化为一个Map
	 * 这个Map是基于结果对象中的属性
	 * eg: 返回一个Map[Integer, Author] for selectMap("selectAuthors", "id")
	 */
	<K, V> Map<K, V> selectMap(String statement, String mapKey);
	
	<K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);
	
	<K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);
	
	<T> Cursor<T> selectCursor(String statement);
	
	<T> Cursor<T> selectCursor(String statement, Object parameter);
	
	<T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);
	
	void select(String statement, Object parameter, ResultHandler handler);
	
	void select(String statement, ResultHandler handler);
	
	void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);
	
	int insert(String statement);
	
	int insert(String statement, Object parameter);
	
	int update(String statement);
	
	int update(String statement, Object parameter);
	
	int delete(String statement);
	
	int delete(String statement, Object parameter);
	
	void commit();
	
	void commit(boolean force);
	
	List<Object> flushStatements();
	
	void rollback();
	
	void rollback(boolean force);
	
	@Override
	void close();
	
	void clearCache();
	
	Configuration getConfiguration();
	
	<T> T getMapper(Class<T> type);
	
	Connection getConnection();
	
	
	
	
	
	
	
	
	
}
