package xml.study.dom.validate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 自定义实体解析器
 * @author ahao
 *
 */
public class MyEntityResolver implements EntityResolver {

	public static final String CONFIG_SYSTEM = "config.dtd";
	public static final String CONFIG_DTD = "xml/study/dom/validate/config.dtd";
	
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		try {
			if (systemId != null) {
				String lowerCaseSystemId = systemId.toLowerCase(Locale.ENGLISH);
				if (lowerCaseSystemId.contains(CONFIG_SYSTEM)) {
					return getInputSource(CONFIG_DTD, publicId, lowerCaseSystemId);
				}
			}
			return null;
		} catch (Exception e) {
			throw new SAXException(e.toString());
		}
	}

	public InputSource getInputSource(String path, String publicId, String systemId) {
		InputSource source = null;
		if (path != null) {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			source = new InputSource(in);
			source.setPublicId(publicId);
			source.setSystemId(systemId);
		}
		return source;
	}
}
