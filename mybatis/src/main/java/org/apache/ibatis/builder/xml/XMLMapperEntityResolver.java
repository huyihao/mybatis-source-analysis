package org.apache.ibatis.builder.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.ibatis.io.Resources;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 当xml需要开启校验时，需要实体解析器引入dtd校验文件
 * EntityResolver接口让我们从其他地方引入dtd文件而不一定要通过URL从网络上加载
 * （有些公司的生产服务器为了安全部署在内网与外网隔离，这时无法从网络上加载获取dtd文件，
 *  而且即使能连接外网，如果dtd所在资源站点发生故障，会导致dtd文件加载失败，与其冒着
 *  安全和失败非风险，不如先下载到本地目录中，需要加载时从本地目录中读取即可）
 */
public class XMLMapperEntityResolver implements EntityResolver {

	// 指定mybatis-config.xml文件和映射文件对应的DTD的SystemId
	private static final String IBATIS_CONFIG_SYSTEM = "ibatis-3-config.dtd";
	private static final String IBATIS_MAPPER_SYSTEM = "ibatis-3-mapper.dtd";
	private static final String MYBATIS_CONFIG_SYSTEM = "mybatis-3-config.dtd";
	private static final String MYBATIS_MAPPER_SYSTEM = "mybatis-3-mapper.dtd";
	
	// 指定mybatis-config.xml文件和映射文件对应的DTD文件的具体位置
	private static final String MYBATIS_CONFIG_DTD = "org/apache/ibatis/builder/xml/mybatis-3-config.dtd";
	private static final String MYBATIS_MAPPER_DTD = "org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd";	
	
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		try {
			if (systemId != null) {
				String lowerCaseSystemId = systemId.toLowerCase(Locale.ENGLISH);
				// 判断mybatis-config.xml中用的是config.dtd还是mapper.dtd
				if (lowerCaseSystemId.contains(MYBATIS_CONFIG_SYSTEM) || lowerCaseSystemId.contains(IBATIS_CONFIG_SYSTEM)) {
					return getInputSource(MYBATIS_CONFIG_DTD, publicId, systemId);
				} else if (lowerCaseSystemId.contains(MYBATIS_MAPPER_SYSTEM) || lowerCaseSystemId.contains(IBATIS_MAPPER_SYSTEM)) {
					return getInputSource(MYBATIS_MAPPER_DTD, publicId, systemId);
				}
			}
			return null;
		} catch (Exception e) {
			throw new SAXException(e.toString());
		}
	}

	private InputSource getInputSource(String path, String publicId, String systemId) {
		InputSource source = null;
		if (path != null) {
			try {
				InputStream in = Resources.getResourceAsStream(path);
				source = new InputSource(in);
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			} catch (IOException e) {
				// ignore, null is ok
			}
		}
		return source;
	}	
}
