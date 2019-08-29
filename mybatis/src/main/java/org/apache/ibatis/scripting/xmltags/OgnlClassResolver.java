package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.io.Resources;

import ognl.ClassResolver;

public class OgnlClassResolver implements ClassResolver {

	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	@Override
	public Class classForName(String className, Map context) throws ClassNotFoundException {
		Class<?> result = null;
		if ((result = classes.get(className)) == null) {
			try {
				result = Resources.classForName(className);
			} catch (ClassNotFoundException e) {
				if (className.indexOf(".") == -1) {
					result = Resources.classForName("java.lang." + className);
					classes.put("java.lang." + className, result);
				}
			}
			classes.put(className, result);
		}
		return result;
	}

}
