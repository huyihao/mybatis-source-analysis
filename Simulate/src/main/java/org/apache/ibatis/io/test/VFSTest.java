package org.apache.ibatis.io.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VFSTest {
	
	private static final byte[] JAR_MAGIC = { 'P', 'K', 3, 4 };
	
	public static void main(String[] args) throws IOException {
		String path = "org/apache/ibatis/io/test/helper/ant-1.9.6.jar";
		List<URL> urlList = Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
		System.out.println(urlList);
		
		for (URL url : urlList) {
			isJar(url, new byte[JAR_MAGIC.length]);
		}
		
	}
	
	protected static boolean isJar(URL url, byte[] buffer) {
		InputStream is = null;
		try {
			is = url.openStream();
			is.read(buffer, 0, JAR_MAGIC.length);
			if (Arrays.equals(buffer, JAR_MAGIC)) {
				return true;
			}
		} catch (Exception e) {
		      // Failure to read the stream means this is not a JAR
		} finally {
		    if (is != null) {
			    try {
			       is.close();
			    } catch (Exception e) {
			       // Ignore
			    }
		    }
		}
		return false;
	}	
}
