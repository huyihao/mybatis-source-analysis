package org.apache.ibatis.parsing.upd;

import org.apache.ibatis.parsing.TokenHandler;

/**
 * 通用占位符解析器
 * @author ahao
 *
 */
public class GenericTokenParserUpd {
	
	private final String openToken;      // 占位符的开始标记，eg: ${、#{
	private final String closeToken;     // 占位符的结束标记，eg: }
	private final TokenHandler handler;  // 占位符处理器
	
	public GenericTokenParserUpd(String openToken, String closeToken, TokenHandler handler) {
		this.openToken = openToken;     
		this.closeToken = closeToken;
		this.handler = handler;
	}
	
	public String parse(String text) {
		final StringBuilder builder = new StringBuilder();
		final StringBuilder expression = new StringBuilder();
		if (text != null & text.length() > 0) {
			char[] src = text.toCharArray();   // 将字符串转化成一个字符数组，方便解析占位符位置
			int offset = 0;        // 初始占位符偏移量，随着对数组的遍历和查找会不断变化，偏移量之前的字符为已经处理过的数据
			int start = text.indexOf(openToken, offset);   // 查找占位符开始标记位置
			while (start > -1) {               // 大于-1证明找到了占位符标记，当对文本查找到最后一个占位符后再次搜索时也返回-1
				// 有时候节点文本值可能有带转义占位符，表示虽然我是个占位符，但我不是用来占位用的，eg: \\${ 表示普通的"${"
				// PS. Java中'\\'实际上只代表一个反斜杠，为了对其进行转义，所以又加了个反斜杠
				if (start > 0 && src[start - 1] == '\\') {
					// eg: text = "123\\${var}"  => "123${var}"
					// offset = 0, start = 4, builder.append("123").append("${")
					builder.append(src, offset, start - offset - 1).append(openToken);
					// 偏移量往前移，如上面的例子，处理完之后偏移量就变成4 + 2 = 6，第6个字符是'v'
					offset = start + openToken.length();
				} else {
					// 找完开始标记之后接着找结束标记，对结束标记的定位和处理和上面类似
					expression.setLength(0);
					builder.append(src, offset, start - offset);
					offset = start + openToken.length();
					int end = text.indexOf(closeToken, offset);
					while (end > -1) {
						if (end > offset && src[end - 1] == '\\') {
							expression.append(src, offset, end - offset - 1).append(closeToken);
							offset = end + closeToken.length();
							end = text.indexOf(closeToken, offset);
						} else {
							// 将开始标记和结束标记之间的字符串追加到expression中保存
							expression.append(src, offset, end - offset);
							offset = end + closeToken.length();
							break;
						}
					}
					// 如果没有找到结束标记，默认是这种情况"${database.user"
					if (end == -1) {
						/**
						 *  TODO: 针对"${database.user\\}"这种情况
						 *  解析为"${database.user}"
						 */
						if (expression.length() != 0) {
							builder.append(openToken).append(expression);
						} else {
							builder.append(src, start, src.length - start);
							offset = src.length;
						}
					} else {
						builder.append(handler.handleToken(expression.toString()));
						offset = end + closeToken.length();
					}
				}
				start = text.indexOf(openToken, offset);
			}
			/**
			 * TODO: 针对"aaa\\${xx\\}"这种情况
			 * 解析为"${xx}"
			 */
			while (true) {
				int end = text.indexOf(closeToken, offset);
				if (end > -1) {
					if (end > offset && src[end - 1] == '\\') {
						builder.append(src, offset, end - offset - 1).append(closeToken);
						offset = end + closeToken.length();
					} else {
						builder.append(src, offset, end - offset).append(closeToken);
						offset = end + closeToken.length();
					}
				} else {
					break;
				}
			}
			if (offset < src.length) {
				builder.append(src, offset, src.length - offset);
			}
		}
		return builder.toString();
	}
}
