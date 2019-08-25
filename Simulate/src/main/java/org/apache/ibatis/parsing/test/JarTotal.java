package org.apache.ibatis.parsing.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/**
 * 统计指定路径下面jar包文件中所有*.class 文件的代码行数
 */
public class JarTotal {
 
	//jar包存放的仓库位置
	public static String jarPath = "C:/Users/ahao/Desktop/count"; 
	//存放所有的jar的包路径和名称
	public static Set<String> jarList = new HashSet<String>();
	//统计jar包总代码行数
	public static int countCode = 0;
	
	public static int runJarTotal() {
		try {
			File filetxtPath = new File("C:/Users/ahao/Desktop/testjarFileCount.txt");//输出要统计的文件信息
			PrintWriter pw = new PrintWriter(new FileWriter(filetxtPath));
			File file = new File(jarPath);
				findAllJarFiles(file);
			for (String jarName : jarList) {
				pw.println(jarName); //将jar文件写入txt中
				Set<String> findAllJarClassfiles = findAllJarClassfiles(jarName);
				for (String jarClassFileName : findAllJarClassfiles) {
						countJarLine(jarName,jarClassFileName);
				}
			}
			pw.println("总共jar文件数量 ：" + jarList.size());
			pw.close();
			System.err.println("jar包文件数量 ：  "+ jarList.size());
			System.err.println("jar包中总代码行数 ：  "+ countCode);
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return countCode;
	}
	
	/**
	 * 遍历获取所有的jar包文件路径和名称
	 * @param dir 目标路径
	 */
	 public static void findAllJarFiles(File dir) {
		 try {
			 //获取当前文件夹下的所有文件和文件夹
			 File[] files = dir.listFiles();
			 for(int i = 0; i < files.length; i++){
				 // System.out.println(fs[i].getAbsolutePath());
				  String jspPath = files[i].getAbsolutePath().replace("\\", "/");
				  if(jspPath.endsWith(".jar")){
					  //System.out.println(jspPath);
					  jarList.add(jspPath);
				  }
				  //如果是文件夹，递归
				  if(files[i].isDirectory()){
					  findAllJarFiles(files[i]);
				  }
				
			 }
		} catch (Exception e) {
			System.err.println("获取所有的jar包路径和名称出错！");
		}
		
	 }
	 
	/**
	 * 获取jar包目录下所有的class文件
	 * @param jarName jar包的路径和名称
	 * @return  返回对应jar包下所有.class 文件的集合
	 */
	 public static Set<String> findAllJarClassfiles(String jarName){
		//存放jar包下对应的文件路径和名称
		Set<String> jarFileList = new HashSet<String>();
		 try {
			JarFile jarFile = new JarFile(jarName);
			Enumeration<JarEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()){
				JarEntry jarEntry = entries.nextElement();
				String fileName = jarEntry.getName();
				if(fileName.endsWith(".class")){
					//System.out.println(fileName);
					jarFileList.add(fileName);
				}
			}
		} catch (IOException e) {
			System.err.println("获取jar包下的所有class出错！");
		}
		 return jarFileList;
	 }
	 
	/**
	 * 构造URI/URL格式的文件路径<br/>
	 * 统计所有jar包中所有class文件的代码行数
	 * @param jarName	jar包的路径和名称
	 * @param jarClassFileName	jar包下所有文件.class 文件的路径和名称
	 * @throws	IOException
	 */
	 public static void countJarLine(String jarName,String jarClassFileName) {
		try {
			URL url = new URL("jar:file:/"+jarName+"!/"+jarClassFileName+""); 
			//System.out.println(url); 
			InputStream is=url.openStream(); 
			BufferedReader br=new BufferedReader(new InputStreamReader(is));
			String line = "";
			while((line = br.readLine())!=null){
				countCode ++;
			}
		} catch (Exception e) {
			System.err.println("统计jar包总代码数出错!");
		}
	}
	 
//==========================================================================================//		
	public static void main(String[] args) throws Exception {
		long start = System.nanoTime();
		runJarTotal();
		long end = System.nanoTime();
		System.out.print("cost: " + (end - start)/1e9 + " seconds");
	}
}
 
