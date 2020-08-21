package com.nokia.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
* @Description: 全局变量
* @Author: xianyu
* @Date: 16:09
*/
public class Constants {

    public static String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    public static final String ftpSrcPath = "/data2/esbdata/GZ/WCDMA/MOBILE/EVERSEC/OMC/CXDR/Gb/ZY/20200729";

    public static final String ftpBakPath = "/home/storm/CDR_TEST/bak/";

    public static final String localPath = "/app/bighead/CDR_Transform/data/input/";

    public static final String localBakPath = "/app/bighead/CDR_Transform/data/bak/" + today + "/";

    public static final String hdfsRootPath = "hdfs://clusterb/tmp/ODM2/";

    // 当前是测试，hdfs路径暂时自定义
    public static String hdfsPath = hdfsRootPath + "CDR_TEST/" + today + "/";

    public static final String localErrorPath = "/app/bighead/CDR_Transform/data/errorPath/" + today + "/";

    public static final Integer theadNum = 1;

    // ftp服务器ip地址资源文件
    public static final String ftpServerProperties = "/ftpServer.properties";
}
