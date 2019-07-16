package org.apache.ibatis.io;

import java.io.IOException;
import java.util.List;

/**
 * VFS(Virtual File System) 虚拟文件系统
 * (暂时留空)
 */
public abstract class VFS {
	private static VFS instance;
	
	public static VFS getInstance() {
		return null;
	}
	
	public List<String> list(String path) throws IOException {
		return null;
	}
}
