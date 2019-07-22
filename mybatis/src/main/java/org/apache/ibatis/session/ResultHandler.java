package org.apache.ibatis.session;

public interface ResultHandler<T> {
	void handleResult(ResultContext<? extends T> resultContext);
}
