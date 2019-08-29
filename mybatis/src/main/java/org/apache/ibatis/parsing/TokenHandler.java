package org.apache.ibatis.parsing;

/**
 * 占位符处理器
 * @author ahao
 *
 */
public interface TokenHandler {
	String handleToken(String content);
}
