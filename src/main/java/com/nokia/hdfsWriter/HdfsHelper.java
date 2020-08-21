package com.nokia.hdfsWriter;
import com.nokia.common.Constants;
import com.nokia.common.HdfsParameter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;

import static com.nokia.common.HdfsParameter.*;

public class HdfsHelper {

    public static final Logger LOG = LoggerFactory.getLogger(HdfsHelper.class);

    private FileSystem hdfsClient = null;

    /**
    * @Description: 初始化
    * @Param:
    * @return:
    * @Author: xianyu
    * @Date: 14:57
    */
    public void init(){

        Configuration config;
        try  {
//            Config.parseLog4jConfigXML("conf/log4j.xml");
            LOG.info("正在初始化HDFS连接");
            String fs = "fs.defaultFS";
            String url = hdfs_url;
            String haName = hdfs_haName;
            String nn1 = hdfs_nn1;
            String nn2 = hdfs_nn2;
            config = new Configuration();
            config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
            config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            config.set(fs, url);
            config.set("dfs.nameservices", haName);
            config.set("dfs.ha.namenodes." + haName, "nn1,nn2");
            config.set("dfs.namenode.rpc-address." + haName + ".nn1", nn1);
            config.set("dfs.namenode.rpc-address." + haName + ".nn2", nn2);
            config.set("dfs.client.failover.proxy.provider." + haName, "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
            //hdfs kerberos
            config.set("hadoop.security.authentication", authentication);
            config.set("dfs.namenode.kerberos.principal", nn_kbs_principal);
            config.set("dfs.datanode.kerberos.principal", dn_kbs_principal);
            config.set("dfs.journalnode.kerberos.principal", jn_kbs_principal);
            //使用设置的用户登陆
            UserGroupInformation.setConfiguration(config);
            UserGroupInformation.loginUserFromKeytab(keytab_user,keytab_path);
            hdfsClient = FileSystem.get(config);
            LOG.info("连接HDFS成功");
        }
        catch (Exception e)
        {
            LOG.error("hdfs 连接错误,", e);
        }
    }

    /** 
    * @Description: 上传文件API 
    * @Param:
    * @return:  
    * @Author: xianyu
    * @Date: 15:10 
    */
    public void upload2HDFS(String src, String target){
        try {
            LocalDateTime start = LocalDateTime.now();
            hdfsClient.copyFromLocalFile(false, false, new Path(src), new Path(target));
            LocalDateTime end = LocalDateTime.now();
            LOG.info("文件上传至HDFS成功 本地路径：{}, HDFS路径：{}, 耗时:{}, 文件大小：{}", src, target, Duration.between(start, end).toMillis(), new File(src).length());
        } catch (IOException e) {
            LOG.error("上传文件失败，源文件：{} ,问题原因：{}", src, e);

        }
    }
    
    /** 
    * @Description: 文件是否存在
    * @Param:
    * @return:
    * @Author: xianyu
    * @Date: 11:47 
    */
    public boolean isFileExist(String file){
        try {
            return hdfsClient.exists(new Path(file));
        } catch (IOException e) {
            LOG.error("文件是否存在错误 ,问题原因：{}", e);
        }
        // 若是出现错误，则全部文件移动到errorPath
        return true;
    }
    

    /**
    * @Description: 关闭
    * @Author: xianyu
    * @Date: 14:05
    */
    public void close(){
        if(hdfsClient != null){
            try {
                hdfsClient.close();
                LOG.info("HDFS连接关闭成功");
            } catch (IOException e) {
                LOG.error("HDFS连接关闭异常");
            }
        }
    }


    public static void main(String[] args) {
        HdfsHelper hdfsHelper = new HdfsHelper();
    }
}
