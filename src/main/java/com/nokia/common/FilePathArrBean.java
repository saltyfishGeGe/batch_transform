package com.nokia.common;

/**
* @Description: 文件路径类
* @Author: xianyu
* @Date: 17:05
*/
public class FilePathArrBean {

    private String ftpPath;

    private String localPath;

    private String hdfsPath;

    public String getFtpPath() {
        return ftpPath;
    }

    public void setFtpPath(String ftpPath) {
        this.ftpPath = ftpPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getHdfsPath() {
        return hdfsPath;
    }

    public void setHdfsPath(String hdfsPath) {
        this.hdfsPath = hdfsPath;
    }
}
