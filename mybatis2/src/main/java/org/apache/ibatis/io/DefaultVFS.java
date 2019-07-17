package org.apache.ibatis.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 主要目的就是从包中加载对应的类路径列表并返回
 * eg: [com/xxx/yyy/a.class, com/xxx/yyy/b.class, com/xxx/yyy/c.class,]
 */
public class DefaultVFS extends VFS {

	private static final Log log = LogFactory.getLog(DefaultVFS.class);
	
	// 如果URL表示一个JAR(ZIP)文件，则将URL转化为InputStream之后，stream的前四个字节为[80, 75, 3, 4]
	// 至于这是什么原理，暂时没搞懂，感觉是不是跟文件后缀有关系？又为何不直接根据文件后缀扩展名判断是否为JAR呢？
	private static final byte[] JAR_MAGIC = {'P', 'K', 3, 4};
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	protected List<String> list(URL url, String path) throws IOException {
		InputStream is = null;
		try {
			List<String> resources = new ArrayList<String>();
			
			// 如果url指向的资源在一个Jar包中，则获取该Jar包对应的URL，否则返回null
			URL jarUrl = findJarForResource(url);
			if (jarUrl != null) {
				is = jarUrl.openStream();
		        if (log.isDebugEnabled()) {
		        	log.debug("Listing " + url);
			    }
		        // 遍历Jar中的资源，并返回以path开头的资源列表
		        resources = listResources(new JarInputStream(is), path);
			}
			else {
				List<String> children = new ArrayList<String>();
				try {
					if (isJar(url)) {
						is = url.openStream();
						JarInputStream jarInput = new JarInputStream(is);
						if (log.isDebugEnabled()) {
							log.debug("Listing " + url);
						}
						// 遍历url指向的目录，将其下资源名称记录到children集合中
			            for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null;) {
			                if (log.isDebugEnabled()) {
			                  log.debug("Jar entry: " + entry.getName());
			                }
			                children.add(entry.getName());
			            }	
			            jarInput.close();						
					}
					else {
						is = url.openStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			            List<String> lines = new ArrayList<String>();
			            for (String line; (line = reader.readLine()) != null;) {
			                if (log.isDebugEnabled()) {
			                    log.debug("Reader entry: " + line);
			                }
			                lines.add(line);
			                if (getResources(path + "/" + line).isEmpty()) {
			                	lines.clear();
			                	break;
			                }
			            }
			            
			            if (!lines.isEmpty()) {
			                if (log.isDebugEnabled()) {
			                    log.debug("Listing " + url);
			                }
			                children.addAll(lines);
			            }						
					}
				} catch (FileNotFoundException e) {
					if ("file".equals(url.getProtocol())) {
						File file = new File(url.getFile());
			            if (log.isDebugEnabled()) {
			            	log.debug("Listing directory " + file.getAbsolutePath());
			            }
			            if (file.isDirectory()) {
			                if (log.isDebugEnabled()) {
			                    log.debug("Listing " + url);
			                }
			                children = Arrays.asList(file.list());
			            }
			        }
			        else {
			            throw e;
			        }
				}
				
		        String prefix = url.toExternalForm();
		        if (!prefix.endsWith("/")) {
		          prefix = prefix + "/";
		        }
		        
		        for (String child : children) {
		            String resourcePath = path + "/" + child;
		            resources.add(resourcePath);
		            URL childUrl = new URL(prefix + child);
		            resources.addAll(list(childUrl, resourcePath));  // 这里嵌套递归，因为找到的文件可能是个Jar文件，需要解析Jar文件中的类
		        }			        
			}
			return resources;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {					
				}
			}
		}
	}
	
	protected List<String> listResources(JarInputStream jar, String path) throws IOException {
		// 在路径的前后补上"/"
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		
	    List<String> resources = new ArrayList<String>();
	    for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
	    	if (!entry.isDirectory()) {
	    		String name = entry.getName();
	    		if (!name.startsWith("/" )) {
	    			name = "/" + name;
	    		}
	    		
	    		if (name.startsWith(path)) {
	    			if (log.isDebugEnabled()) {
	    				log.debug("Found resource: " + name);
	    			}
	    			resources.add(name.substring(1));
	    		}
	    	}
	    }
		return resources;
	}
	
	protected URL findJarForResource(URL url) throws MalformedURLException {
		if (log.isDebugEnabled()) {
			log.debug("Find JAR URL: " + url);
		}
		
		// 如果URL的文件部分本身是URL，则该URL可能指向JAR
		try {
			for (;;) {
				url = new URL(url.getFile());
				if (log.isDebugEnabled()) {
					log.debug("Inner URL: " + url);
				}
			}
		} catch (MalformedURLException e) {
		}
		
		// 检测文件扩展名是否为 ".jar"
		StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
		int index = jarUrl.lastIndexOf(".jar");
		if (index >= 0) {
			jarUrl.setLength(index + 4);
			if (log.isDebugEnabled()) {
				log.debug("Extracted JAR URL: " + jarUrl);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Not a JAR: " + jarUrl);
			}
			return null;
		}
		
		// 尝试打开并且测试jar包中的类
		try {
			URL testUrl = new URL(jarUrl.toString());
			if (isJar(testUrl)) {
				return testUrl;
			}
			else {
				// WebLogin修复：检测URL文件是否存在文件系统中
				if (log.isDebugEnabled()) {
					log.debug("Not a JAR: " + jarUrl);
				}
				jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
				File file = new File(jarUrl.toString());
				
				// 文件名可能被转码了
				if (!file.exists()) {
					try {
						file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
					}
				}
				
				if (file.exists()) {
					if (log.isDebugEnabled()) {
						log.debug("Trying real file: " + file.getAbsolutePath());
					}
					testUrl = file.toURI().toURL();
					if (isJar(testUrl)) {
						return testUrl;
					}
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Invalid JAR URL: " + jarUrl);
		}
		
	    if (log.isDebugEnabled()) {
	        log.debug("Not a JAR: " + jarUrl);
	    }
	    return null;		
	}

	// 根据包名获取包路径
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	protected boolean isJar(URL url) {
		return isJar(url, new byte[JAR_MAGIC.length]);
	}
	
	// 判断一个URL是不是指向一个JAR文件
	protected boolean isJar(URL url, byte[] buffer) {
		InputStream is = null;
		try {
			is = url.openStream();
			is.read(buffer, 0, JAR_MAGIC.length);
			if (Arrays.equals(buffer, JAR_MAGIC)) {
				if (log.isDebugEnabled()) {
					log.debug("Found JAR: " + url);
				}
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {				
				}
			}
		}
		return false;
	}
}
