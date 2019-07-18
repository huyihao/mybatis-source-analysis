package org.apache.ibatis.exceptions;

/**
 * mybatis中所有运行时异常的祖类
 * 运行时异常不要求代码必须捕获处理
 */
@Deprecated
public class IbatisException extends RuntimeException {
	
	private static final long serialVersionUID = 4197389552824706251L;

	public IbatisException() {
		super();
	}
	
	public IbatisException(String message) {
		super(message);
	}
	
	public IbatisException(Throwable cause) {
		super(cause);
	}
	
	public IbatisException(String message, Throwable cause) {
		super(message, cause);
	}
}
