package com.nokia.ftpreader;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class UserFtpFileFilter implements FTPFileFilter {

	private static final Logger LOG = LoggerFactory.getLogger(UserFtpFileFilter.class);
	
	private List<String> ls;
	
	public UserFtpFileFilter(List<String> ls) {
		this.ls = ls;
	}
	
	@Override
	public boolean accept(FTPFile file) {
		String tmpName = file.getName();
		
		if(ls != null && ls.size() > 0){//代表用户有指定过滤文件名
			for(String s : ls){
				if(s.equals("*")){
					return true;
				}
				boolean isMatch = Pattern.compile(s).matcher(tmpName).find();
				// 这里修改成按文件名过滤掉对应的文件
				if(isMatch){
					return false;
				}
			}
		}
		return true;
	}
	

}
