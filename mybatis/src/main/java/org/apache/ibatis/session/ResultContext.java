package org.apache.ibatis.session;

public interface ResultContext<T> {
	
	T getResultObject();
	
	int getResultCount();
	
	boolean isStopped();
	
	void stop();
}
