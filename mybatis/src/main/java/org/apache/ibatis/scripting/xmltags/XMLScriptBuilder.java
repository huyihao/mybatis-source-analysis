package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLScriptBuilder extends BaseBuilder {

	private XNode context;
	private boolean isDynamic;
	private Class<?> parameterType;
	
	public XMLScriptBuilder(Configuration configuration, XNode context) {
		this(configuration, context, null);
	}
	
	public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
		super(configuration);
		this.context = context;
		this.parameterType = parameterType;
	}

	// 解析mapper.xml中的sql节点并创建SqlSource对象
	public SqlSource parseScriptNode() {
		// 判断当前的节点是不是有动态SQL，动态SQL会包括占位符或是动态SQL的相关节点
		List<SqlNode> contents = parseDynamicTags(context);
	    MixedSqlNode rootSqlNode = new MixedSqlNode(contents);
	    SqlSource sqlSource = null;
	    if (isDynamic) {    // 根据是否是动态SQL，创建相应的SqlSource对象
	    	sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
	    } else {
	    	sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
	    }
	    return sqlSource;
	}
	
	List<SqlNode> parseDynamicTags(XNode node) {
		List<SqlNode> contents = new ArrayList<SqlNode>();   // 用于记录生成的SqlNode集合
		NodeList children = node.getNode().getChildNodes();  // 获取SQL节点的所有子节点
		for (int i = 0; i < children.getLength(); i++) {
			// 创建XNode，该过程会将能解析掉的"${}"都解析掉
			XNode child = node.newXNode(children.item(i));
			// 对文本节点的处理
			if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
				String data = child.getStringBody("");
				TextSqlNode textSqlNode = new TextSqlNode(data);
				// 解析SQL语句，如果含有未解析的"${}"占位符，则为动态SQL
				if (textSqlNode.isDynamic()) {
					contents.add(textSqlNode);
					isDynamic = true;
				} else {
					contents.add(new StaticTextSqlNode(data));
				}
			} else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) {
				// 如果子节点是一个标签，那么一定是动态SQL，并且根据不同的动态标签生成不同的NodeHandler
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlers(nodeName);   // 根据节点名创建对应的NodeHandler
				if (handler == null) {    // 如果未能创建节点，说明使用的SQL节点名写错了
					throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
				}
				// 处理动态SQL，并将解析得到的SqlNode对象放入contents集合中保存
		        handler.handleNode(child, contents);
		        isDynamic = true;
			}
		}
		return contents;
	}
	
	// 根据节点名创建对应的NodeHandler
	NodeHandler nodeHandlers(String nodeName) {
		Map<String, NodeHandler> map = new HashMap<String, NodeHandler>();
		map.put("trim", new TrimHandler());
		map.put("where", new WhereHandler());
		map.put("set", new SetHandler());
		map.put("foreach", new ForEachHandler());
		map.put("if", new IfHandler());
		map.put("choose", new ChooseHandler());
		map.put("when", new IfHandler());
		map.put("otherwise", new OtherwiseHandler());
		map.put("bind", new BindHandler());
		return map.get(nodeName);
	}
	
	private interface NodeHandler {
	    void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
	}
	
	// <bind>节点处理器
	private class BindHandler implements NodeHandler {
		public BindHandler() {}
		
		/**
		 * <select id="selectBlogsLike" resultType="Blog">
		 *     <bind name="pattern" value="'%' + _parameter.getTitle() + '%'" />
		 * 	   SELECT * FROM BLOG WHERE title LIKE #{pattern}
		 * </select>
		 */
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			final String name = nodeToHandle.getStringAttribute("name");
			final String expression = nodeToHandle.getStringAttribute("value");
			final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
			targetContents.add(node);
		}		
	}
	
	// <trim>节点处理器
	private class TrimHandler implements NodeHandler {
		public TrimHandler() {}
		
		/**
		 * <trim prefix="WHERE" prefixOverrides="AND |OR " suffixOverrides="aa">
		 *    <if>and xxx</if>
		 *    <if>and yyy aa</if>
		 * </trim>
		 */
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> contents = parseDynamicTags(nodeToHandle);   // 因为<trim>节点一般有嵌套的节点，这里递归调用
			MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
			String prefix = nodeToHandle.getStringAttribute("prefix");
	        String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
	        String suffix = nodeToHandle.getStringAttribute("suffix");
	        String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
	        TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
	        targetContents.add(trim);
		}
		
	}
	
	// <where>节点处理器: 去除前缀 "AND | OR"
	private class WhereHandler implements NodeHandler {
		public WhereHandler() {}

		/**
		 * <select id="findActiveBlogLike" resultType="Blog">
		 * SELECT * FROM BLOG
		 * 	  <where>
		 * 		  <if test="state != null">
		 * 		      state = #{state}
		 * 		  </if>
		 *        <if test="title != null">
		 *            AND title like #{title}
		 *        </if>
    	 *        <if test="author != null and author.name != null">
         *            AND author_name like #{author.name}
         *        </if>
         *    </where>
         * </select>
		 */
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> contents = parseDynamicTags(nodeToHandle);
			MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
			WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
			targetContents.add(where);
		}
	}
	
	// <set>节点处理器: 去除后缀 ","
	private class SetHandler implements NodeHandler {
		public SetHandler() {}

		/* <update id="updateAuthorIfNecessary">
		 * update Author
		 *   <set>
		 *     <if test="username != null">username=#{username},</if>
		 *     <if test="password != null">password=#{password},</if>
		 *     <if test="email != null">email=#{email},</if>
		 *     <if test="bio != null">bio=#{bio}</if>
		 *   </set>
		 * where id=#{id}
		 * </update>
		 */
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> contents = parseDynamicTags(nodeToHandle);
			MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
			SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
			targetContents.add(set);
		}
	}

	// <foreach>节点处理器
	private class ForEachHandler implements NodeHandler {
		public ForEachHandler() {}
		
		/*
		 * <select id="selectPostIn" resultType="domain.blog.Post">
		 * 	SELECT *
		 * 	FROM POST P
		 * 	WHERE ID in
		 * 	<foreach item="item" index="index" collection="list"
		 *     open="(" separator="," close=")">
		 *       #{item}
		 * 	</foreach>
		 * </select>	
		 */	
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
		    List<SqlNode> contents = parseDynamicTags(nodeToHandle);
		    MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
		    String collection = nodeToHandle.getStringAttribute("collection");
		    String item = nodeToHandle.getStringAttribute("item");
		    String index = nodeToHandle.getStringAttribute("index");
		    String open = nodeToHandle.getStringAttribute("open");
		    String close = nodeToHandle.getStringAttribute("close");
		    String separator = nodeToHandle.getStringAttribute("separator");
		    ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
		    targetContents.add(forEachSqlNode);		    
		}
	}
	
	// <if>节点处理器
	private class IfHandler implements NodeHandler {
		public IfHandler() {}

		/**
		 *  <if test="username != null">
		 *  	username = #{username}
		 *  </if>
		 */
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> contents = parseDynamicTags(nodeToHandle);
			MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
			String test = nodeToHandle.getStringAttribute("test");
			IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
			targetContents.add(ifSqlNode);
		}
	}

	// <otherwise>节点处理器
	private class OtherwiseHandler implements NodeHandler {
		public OtherwiseHandler() {}

		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> contents = parseDynamicTags(nodeToHandle);
			MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
			targetContents.add(mixedSqlNode);
		}
	}
	
	// <choose>节点处理器
	private class ChooseHandler implements NodeHandler {
		public ChooseHandler() {}
		
		/**
		 * <select id="findActiveBlogLike" resultType="Blog">
		 * 	SELECT * FROM BLOG WHERE state = ‘ACTIVE’
		 * 		<choose>
		 *   		<when test="title != null">
		 *     			AND title like #{title}
		 *   		</when>
		 *   		<when test="author != null and author.name != null">
		 *     			AND author_name like #{author.name}
		 *   		</when>
		 *   		<otherwise>
		 *     			AND featured = 1
		 *   		</otherwise>
		 * 		</choose>
		 * </select>	
		 */	
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> whenSqlNodes = new ArrayList<SqlNode>();
			List<SqlNode> otherwiseSqlNodes = new ArrayList<SqlNode>();
			handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
			SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
			ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
			targetContents.add(chooseSqlNode);
		}

		private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
			List<XNode> children = chooseSqlNode.getChildren();
			for (XNode child : children) {
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlers(nodeName);
				if (handler instanceof IfHandler) {
					handler.handleNode(child, ifSqlNodes);
				} else if (handler instanceof OtherwiseHandler) {
					handler.handleNode(child, defaultSqlNodes);
				}
			}
		}

		private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
			SqlNode defaultSqlNode = null;
			if (defaultSqlNodes.size() == 1) {
				defaultSqlNode = defaultSqlNodes.get(0);
			} else if (defaultSqlNodes.size() > 1) {
				throw new BuilderException("Too many default (otherwise) elements in choose statement.");
			}
			return defaultSqlNode;
		}
	}
}
