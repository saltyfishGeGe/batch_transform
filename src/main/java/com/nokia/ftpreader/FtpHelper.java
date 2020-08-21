package com.nokia.ftpreader;

import org.apache.commons.net.ftp.FTPClient;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FtpHelper {
	/**
	 * 
	* @Title: LoginFtpServer 
	* @Description: 与ftp服务器建立连接
	* @param @param host
	* @param @param username
	* @param @param password
	* @param @param port
	* @param @param timeout
	* @param @param connectMode     
	* @return void 
	* @throws
	 */
	public abstract void loginFtpServer() ;
	/**
	 * 
	* @Title: LogoutFtpServer 
	* todo 方法名首字母
	* @Description: 断开与ftp服务器的连接 
	* @param      
	* @return void 
	* @throws
	 */
	public abstract void logoutFtpServer();
	/**
	 * 
	* @Title: isDirExist 
	* @Description: 判断指定路径是否是目录
	* @param @param directoryPath
	* @param @return     
	* @return boolean 
	* @throws
	 */
	public abstract boolean isDirExist(String directoryPath);
	/**
	 * 
	* @Title: isFileExist 
	* @Description: 判断指定路径是否是文件
	* @param @param filePath
	* @param @param fileSuffix
	* 
	* @param @return     
	* @return boolean 
	* @throws
	 */
	public abstract boolean isFileExist(String filePath,List<String> fileSuffix);
	/**
	 * 
	* @Title: isSymbolicLink 
	* @Description: 判断指定路径是否是软链接
	* @param @param filePath
	* @param @return     
	* @return boolean 
	* @throws
	 */
	public abstract boolean isSymbolicLink(String filePath,List<String> fileSuffix);
	/**
	 * 
	* @Title: getListFiles 
	* @Description: 递归获取指定路径下符合条件的所有文件绝对路径
	* @param @param directoryPath
	* @param @param parentLevel 父目录的递归层数（首次为0）
	* @param @param maxTraversalLevel 允许的最大递归层数
	* @param @param fileSuffix 处理文件后缀
	* @param @return     
	* @return HashSet<String> 
	* @throws
	 */
	public abstract HashSet<String> getListFiles(String directoryPath, int parentLevel, int maxTraversalLevel,List<String> fileSuffix);
	
	/**
	 * 
	* @Title: getInputStream 
	* @Description: 获取指定路径的输入流
	* @param @param filePath
	* @param @return     
	* @return InputStream 
	* @throws
	 */
	public abstract InputStream getInputStream(String filePath);
	
	/**
	 * 
	* @Title: getAllFiles 
	* @Description: 获取指定路径列表下符合条件的所有文件的绝对路径  
	* @param @param srcPaths 路径列表
	* @param @param parentLevel 父目录的递归层数（首次为0）
	* @param @param maxTraversalLevel 允许的最大递归层数
	* @param @return     
	* @return HashSet<String> 
	* @throws
	 */
	public HashSet<String> getAllFiles(List<String> srcPaths, int parentLevel, int maxTraversalLevel,List<String> fileSuffix){
		HashSet<String> sourceAllFiles = new HashSet<String>();
		if (!srcPaths.isEmpty()) {
			for (String eachPath : srcPaths) {
				sourceAllFiles.addAll(getListFiles(eachPath, parentLevel, maxTraversalLevel,fileSuffix));
			}
		}
		return sourceAllFiles;
	}

	/**
	* @Description: 获取文件最新修改时间
	* @Param:
	* @return:
	* @Author: xianyu
	* @Date: 11:21
	*/
	public abstract Long getfileModifyTime(String fileName);

	/**
	* @Description: 将文件移动到指定目录下
	* @Param:
	* @return:
	* @Author: xianyu
	* @Date: 11:24
	*/
	public abstract void moveFtpFiles(HashSet<String> files, String targetPath);

	/**
	 * @Description: 将文件移动到指定目录下
	 * @Param:
	 * @return:
	 * @Author: xianyu
	 * @Date: 11:24
	 */
	public abstract void moveFtpFile(String file, String target);

	/** 
	* @Description: 获取FTPClient客户端 
	* @Param:  
	* @return:  
	* @Author: xianyu
	* @Date: 14:26 
	*/
	public abstract FTPClient getFtpClient();

	/** 
	* @Description: 获取当前扫描ftp的IP地址 
	* @Param:  
	* @return:  
	* @Author: xianyu
	* @Date: 14:27 
	*/
	public abstract String currentIP();

	/** 
	* @Description: 设置扫描路径 
	* @Param:  
	* @return:  
	* @Author: xianyu
	* @Date: 14:27 
	*/
	public abstract void setScanPath(String path);

	/** 
	* @Description: 获取扫描路径 
	* @Param:
	* @return:  
	* @Author: xianyu
	* @Date: 14:27 
	*/
	public abstract String getScanPath();

	/**
	* @Description: 获取当前ftp连接扫描到的所有文件基础信息
	* @Param:
	* @return:
	* @Author: xianyu
	* @Date: 10:27
	*/
	public abstract Set<FtpFileBean> getScanFtpAllFilesInfo();

	/** 
	* @Description: 当前ftp是否连接有效 
	* @Param:  
	* @return:  
	* @Author: xianyu
	* @Date: 14:39 
	*/
	public abstract boolean isConnect();

	/** 
	* @Description: 断开当前连接，重新连接FTP服务器 
	* @Param:  
	* @return:  
	* @Author: xianyu
	* @Date: 16:54 
	*/
	public abstract boolean reConnect();
}
