package com.nokia.ftpreader;

import com.nokia.common.CDRException;
import com.nokia.common.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class StandardFtpHelper extends FtpHelper {
	private static final Logger LOG = LoggerFactory.getLogger(StandardFtpHelper.class);
	FTPClient ftpClient = null;

	private Set<FtpFileBean> scanFtpAllFiles = null;

	private String host;
	private String username;
	private String password;
	private int port;
	private int timeout;
	private String connectMode;

	// 当前扫描路径
	private String scanFilePath;

	public StandardFtpHelper(String host, String username, String password, int port, int timeout, String connectMode) {
		this.host = host;
		this.username = username;
		this.password = password;
		this.port = port;
		this.timeout = timeout;
		this.connectMode = connectMode;
	}

	@Override
	public void loginFtpServer() {

		ftpClient = new FTPClient();
		try {
			// 连接
			ftpClient.connect(host, port);
			// 登录
			ftpClient.setConnectTimeout(timeout);
			ftpClient.setDataTimeout(timeout);
			ftpClient.login(username, password);
			LOG.info("FTP{},登录成功", host);
			// 不需要写死ftp server的OS TYPE,FTPClient getSystemType()方法会自动识别
			// ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
			if ("PASV".equals(connectMode)) {
				ftpClient.enterRemotePassiveMode();
				ftpClient.enterLocalPassiveMode();
			} else if ("PORT".equals(connectMode)) {
				ftpClient.enterLocalActiveMode();
				// ftpClient.enterRemoteActiveMode(host, port);
			}
			int reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftpClient.disconnect();
				String message = String.format("与ftp服务器建立连接失败,请检查用户名和密码是否正确: [%s]",
						"message:host =" + host + ",username = " + username + ",port =" + port);
				LOG.error(message);
				throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_LOGIN, message);
			}
//			String fileEncoding = System.getProperty("file.encoding");
//			ftpClient.setControlEncoding(fileEncoding);
			ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);

			ftpClient.setBufferSize(1024 * 1024);
		} catch (UnknownHostException e) {
			String message = String.format("请确认ftp服务器地址是否正确，无法连接到地址为: [%s] 的ftp服务器", host);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
		} catch (IllegalArgumentException e) {
			String message = String.format("请确认连接ftp服务器端口是否正确，IP为：[%s]，错误的端口: [%s] ", host, port);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
		} catch (Exception e) {
			String message = String.format("与ftp服务器建立连接失败 : [%s]",
					"message:host =" + host + ",username = " + username + ",port =" + port);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
		}

	}

	@Override
	public void logoutFtpServer() {
		if (ftpClient.isConnected()) {
			try {
				//todo ftpClient.completePendingCommand();//打开流操作之后必须，原因还需要深究
				ftpClient.logout();
				LOG.info("{} FTP连接退出登录成功..", currentIP());
			} catch (IOException e) {
				String message = "与ftp服务器:"+ currentIP() +"断开连接失败";
				LOG.error(message);
				throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_DISCONNECT, message, e);
			}finally {
				if(ftpClient.isConnected()){
					try {
						ftpClient.disconnect();
						LOG.info("FTP服务器{}断开连接成功..", currentIP());
					} catch (IOException e) {
						String message = "与ftp服务器" + currentIP() +"断开连接失败";
						LOG.error(message);
						throw CDRException.asCDRXException(FtpReaderErrorCode.FAIL_DISCONNECT, message, e);
					}
				}

			}
		}
	}

	@Override
	public boolean isDirExist(String directoryPath) {
		try {
			return ftpClient.changeWorkingDirectory(new String(directoryPath.getBytes(),FTP.DEFAULT_CONTROL_ENCODING));
		} catch (IOException e) {
			String message = String.format("FTP服务:[%s], 进入目录：[%s]时发生I/O异常,请确认与ftp服务器的连接正常", currentIP(), directoryPath);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
		}
	}

	
	
	@Override
	public boolean isFileExist(String filePath,List<String> fileSuffix) {
		boolean isExitFlag = false;
		try {
			FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(),FTP.DEFAULT_CONTROL_ENCODING),new UserFtpFileFilter(fileSuffix));
			if (ftpFiles.length == 1 && ftpFiles[0].isFile()) {
				isExitFlag = true;
			}
		} catch (IOException e) {
			String message = String.format("FTP服务:[%s],获取文件：[%s] 属性时发生I/O异常,请确认与ftp服务器的连接正常", currentIP(), filePath);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
		}
		return isExitFlag;
	}

	@Override
	public boolean isSymbolicLink(String filePath,List<String> fileSuffix) {
		boolean isExitFlag = false;
		try {
			FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(),FTP.DEFAULT_CONTROL_ENCODING),new UserFtpFileFilter(fileSuffix));
			if (ftpFiles.length == 1 && ftpFiles[0].isSymbolicLink()) {
				isExitFlag = true;
			}
		} catch (IOException e) {
			String message = String.format("FTP服务:[%s],获取文件：[%s] 属性时发生I/O异常,请确认与ftp服务器的连接正常",currentIP(), filePath);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
		}
		return isExitFlag;
	}

	HashSet<String> sourceFiles = null;
	@Override
	public HashSet<String> getListFiles(String directoryPath, int parentLevel, int maxTraversalLevel,List<String> fileSuffix) {
		sourceFiles = new HashSet<String>();
		if(parentLevel < maxTraversalLevel){
			String parentPath = null;// 父级目录,以'/'结尾
			int pathLen = directoryPath.length();
			// 暂不支持正则
//			if (directoryPath.contains("*") || directoryPath.contains("?")) {
//				// path是正则表达式
//				String subPath  = UnstructuredStorageReaderUtil.getRegexPathParentPath(directoryPath);
//				if (isDirExist(subPath)) {
//					parentPath = subPath;
//				} else {
//					String message = String.format("不能进入目录：[%s]," + "请确认您的配置项path:[%s]存在，且配置的用户有权限进入", subPath,
//							directoryPath);
//					LOG.error(message);
//					throw CDRException.asCDRXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
//				}
//			} else
			if (isDirExist(directoryPath)) {
				// path是目录
				if (directoryPath.charAt(pathLen - 1) == '/') {
					parentPath = directoryPath;
				} else {
					parentPath = directoryPath + '/';
				}
			} else if (isFileExist(directoryPath,fileSuffix)) {
				// path指向具体文件
				sourceFiles.add(directoryPath);
				return sourceFiles;
			} else if(isSymbolicLink(directoryPath,fileSuffix)){
				//path是链接文件
				String message = String.format("FTP服务:[%s],文件:[%s]是链接文件，当前不支持链接文件的读取", currentIP(), directoryPath);
				LOG.error(message);
				throw CDRException.asCDRXException(FtpReaderErrorCode.LINK_FILE, message);
			}else {
				String message = String.format("FTP服务:[%s], 请确认您的配置项path:[%s]存在，且配置的用户有权限读取", currentIP(), directoryPath);
				LOG.error(message);
				throw CDRException.asCDRXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
			}

			try {

				FTPFile[] fs = ftpClient.listFiles(new String(directoryPath.getBytes(),FTP.DEFAULT_CONTROL_ENCODING),new UserFtpFileFilter(fileSuffix));
				for (FTPFile ff : fs) {
					String strName = ff.getName();
					String filePath = parentPath + strName;
					// 暂时屏蔽递归相关代码
//					if (ff.isDirectory()) {
//						if (!(strName.equals(".") || strName.equals(".."))) {
//							//递归处理
//							getListFiles(filePath, parentLevel+1, maxTraversalLevel,fileSuffix);
//						}
//					} else
					if (ff.isFile()) {
						// 是文件
//						LOG.info("FTP服务:{}, 扫描到文件：{}, 文件大小为：{}, 落地时间：{}", currentIP(), filePath, ff.getSize(), ff.getTimestamp().getTimeInMillis());
						sourceFiles.add(filePath);
					} else if(ff.isSymbolicLink()){
						//是链接文件
						String message = String.format("FTP服务:[%s],文件:[%s]是链接文件，当前不支持链接文件的读取", currentIP(), filePath);
						LOG.error(message);
						throw CDRException.asCDRXException(FtpReaderErrorCode.LINK_FILE, message);
					}else {
						String message = String.format("FTP服务:[%s],请确认path:[%s]存在，且配置的用户有权限读取", currentIP(), filePath);
						LOG.error(message);
						throw CDRException.asCDRXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
					}
				}

				// 在此处备份ftp文件信息，主要是落地时间, 文件大小，用于统计数据
				FtpFileBean ftpFileBean = null;
				String ftpFileName = null;
				Long fileModifyTime = null;
				long size = 0l;
				scanFtpAllFiles = new HashSet<>();
				for (FTPFile f : fs) {
					ftpFileName = f.getName();
					fileModifyTime = f.getTimestamp().getTimeInMillis();
					size = f.getSize();
					ftpFileBean = new FtpFileBean(ftpFileName, fileModifyTime, size);
					scanFtpAllFiles.add(ftpFileBean);
				}

				// 打印当前线程扫描到的所有
				LOG.info("FTP: {}, 当前读取的FTP扫描列表：文件总数：{}， 文件总大小为：{}", currentIP(), fs.length, Arrays.stream(fs).mapToLong(f -> f.getSize()).sum());
			} catch (IOException e) {
				String message = String.format("FTP服务:[%s], 获取path：[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常", currentIP(), directoryPath);
				LOG.error(message);
				throw CDRException.asCDRXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
			}

			return sourceFiles;
			
		} else{
			//超出最大递归层数
			String message = String.format("获取path：[%s] 下文件列表时超出最大层数,请确认路径[%s]下不存在软连接文件", directoryPath, directoryPath);
			LOG.error(message);
			throw CDRException.asCDRXException(FtpReaderErrorCode.OUT_MAX_DIRECTORY_LEVEL, message);
		}
	}

	@Override
	public InputStream getInputStream(String filePath) {
		try {
			// 每次数据连接之前，ftp client告诉ftp server开通一个端口来传输数据
			// 因为ftp server可能每次开启不同的端口来传输数据，但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞。
			ftpClient.enterLocalPassiveMode();
			return ftpClient.retrieveFileStream(new String(filePath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
		} catch (IOException e) {
			String message = String.format("FTP服务:[%s], 读取文件 : [%s] 时出错,请确认文件：[%s]存在且配置的用户有权限读取", currentIP(), filePath, filePath);
			LOG.error(message, e);
			throw CDRException.asCDRXException(FtpReaderErrorCode.OPEN_FILE_ERROR, message);
		}
	}

	public Long getfileModifyTime(String fileName){
		for(FtpFileBean ff : this.scanFtpAllFiles){
			if(fileName.equals(ff.getFileName())){
				return ff.getLastModifyTime();
			}
		}
		return null;
	}


	@Override
	public void moveFtpFiles(HashSet<String> files, String targetPath) {
		if(files == null || files.size() == 0){
			files = this.sourceFiles;
		}
		if (files.size() == 0){
			LOG.error("当前FTP未读取到文件，文件移动失败");
			return;
		}
//		if(!targetPath.endsWith(File.separator)){
//			targetPath = targetPath + File.separator;
//		}

		for(String file : files){
			this.moveFtpFile(file, targetPath + FilenameUtils.getName(file));
		}
	}

	@Override
	public void moveFtpFile(String file, String target) {
		try{
			// 目标目录，不存在则自动创建
			if(!this.isDirExist(Constants.ftpBakPath)){
				ftpClient.makeDirectory(Constants.ftpBakPath);
			}
			// 如果目标目录已经有同名文件，则移动失败
			if(this.isFileExist(target, null)){
				LOG.error("FTP服务:{}, 备份目录已存在同名文件，文件移动失败。{}", currentIP(), target);
				return;
			}
			LOG.info("FTP服务:{}, 当前开始移动文件：{}", currentIP(), file);
			// 原名移动到指定目录
			ftpClient.rename(file, target); //移动文件到新目录
			// 若是大文件移动出现问题可参考 https://bbs.csdn.net/topics/390373219
			LOG.info("FTP服务:{}, FTP文件移动完成! 目标路径：{}",currentIP(), target);
		} catch (IOException e) {
			LOG.error("FTP服务:{}, FTP文件移动失败：{}", currentIP(), e);
		}
	}

	@Override
	public FTPClient getFtpClient(){
		return this.ftpClient;
	}

    @Override
    public String currentIP() {
        return this.host;
    }

	@Override
	public void setScanPath(String path) {
		this.scanFilePath = path;
	}

	@Override
	public String getScanPath() {
		return this.scanFilePath;
	}

	@Override
	public Set<FtpFileBean> getScanFtpAllFilesInfo() {
		return this.scanFtpAllFiles;
	}

	@Override
	public boolean isConnect(){
		int reply = ftpClient.getReplyCode();
		if (FTPReply.isPositiveCompletion(reply)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean reConnect() {
		try {
			this.logoutFtpServer();
			this.loginFtpServer();
		}catch (Exception e){
			LOG.error("FTP:{}, 重连接失败", this.currentIP());
			return false;
		}
		LOG.info("FTP:{}, 重连接成功", this.currentIP());
		return true;
	}

	/**
     * 改名FTP上的文件
     */
    
//    public boolean renameFile(String srcFname,String targetFname){  
//        boolean flag = false;  
//        if( ftpClient!=null ){  
//            try {  
//                flag = ftpClient.rename(srcFname,targetFname);  
//            } catch (IOException e) {  
//            	LOG.error(e,e);
//            	logoutFtpServer();  
//            }  
//        }  
//        return flag;  
//    }  
	public static void main(String[] args) {

	}
}
