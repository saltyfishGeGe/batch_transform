package com.nokia.ftpreader;

import java.util.Objects;

/**
 * @Description: ftp文件类 ，除去文件路径新拓展文件落地时间
 * @Author: xianyu
 * @Date: 15:15
 */
public class FtpFileBean {

    private String fileName;

    private Long lastModifyTime;

    private long fileSize;

    public String getFileName() {
        return fileName;
    }

    public FtpFileBean setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Long getLastModifyTime() {
        return lastModifyTime;
    }

    public FtpFileBean setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
        return this;
    }

    public long getFileSize() {
        return fileSize;
    }

    public FtpFileBean setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public FtpFileBean() {
    }

    public FtpFileBean(String fileName, Long lastModifyTime, long fileSize) {
        this.fileName = fileName;
        this.lastModifyTime = lastModifyTime;
        this.fileSize = fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FtpFileBean that = (FtpFileBean) o;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }
}
