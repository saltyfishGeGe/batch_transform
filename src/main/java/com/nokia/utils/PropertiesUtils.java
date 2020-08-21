package com.nokia.utils;


import com.nokia.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class PropertiesUtils {

    private static Logger LOG = LoggerFactory.getLogger(PropertiesUtils.class);

    public static HashSet<String> getFtpServers(){
        Properties properties = PropertiesUtils.readPropertiesFile(Constants.ftpServerProperties);
        HashSet<String> result = new HashSet<>();
        Set<Object> names = properties.keySet();
        for(Object o : names){
            if(((String)o).contains("ftp")){
                String ip = properties.getProperty(o.toString());
                result.add(ip);
            }
        }

        if(result.size() == 0){
            LOG.error("当前不存在有效的FTP服务器地址，请检查配置文件ftpServer.properties是否配置正确");
            System.exit(0);
        }
        LOG.info("当前FTP服务器IP地址为：{}", result.toString());
        return result;
    }

    public static Properties readPropertiesFile(String filePath){
        Properties properties = new Properties();
        InputStream inputStream = Object.class.getResourceAsStream(filePath);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void main(String[] args) {
        HashSet<String> ftpServers = PropertiesUtils.getFtpServers();
        System.out.println("ftp----" + ftpServers);
    }

}
