package com.nokia;

import com.nokia.common.Constants;
import com.nokia.ftpreader.FtpHelper;
import com.nokia.ftpreader.StandardFtpHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;

import static com.nokia.common.FtpParameter.*;

/**
* @Description: 单元测试类
* @Author: xianyu
* @Date: 9:49
*/
public class TestJunit {

    private Logger LOG = LoggerFactory.getLogger(TestJunit.class);

    public static void main(String[] args) {
        TestJunit tj = new TestJunit();
        tj.manyClientOpenTheSameFile();
    }

    public void manyClientOpenTheSameFile(){
        FtpHelper helper1 = new StandardFtpHelper("172.17.8.229",
                "storm",
                "Storm@chpw0325",
                port,
                timeout,
                connectMode);
        helper1.loginFtpServer();

        FtpHelper helper2 = new StandardFtpHelper("172.17.8.229",
                "storm",
                "Storm@chpw0325",
                port,
                timeout,
                connectMode);
        helper2.loginFtpServer();

        new Thread(() -> {
            HashSet<String> all = helper1.getListFiles("/home/storm/terrace_root_test", 0, 100, null);
            helper1.getFtpClient().enterLocalPassiveMode();
            FileOutputStream fos = null;
            try {
                helper1.getFtpClient().setFileType(FTP.BINARY_FILE_TYPE);
            String currentFile = all.iterator().next();
                String fileName = FilenameUtils.getName(currentFile);

            fos = new FileOutputStream(new File(Constants.localPath + fileName));
            if (helper1.getFtpClient().retrieveFile(currentFile, fos)) {

                fos.flush();
                LocalDateTime end = LocalDateTime.now();
                LOG.info("文件下载完成:{}", fileName);
            } else {
                LOG.error("文件下载失败:{}", fileName);
            }
            } catch (IOException e) {
                LOG.error("IO异常了：{}", e);
            }
        }).start();

        new Thread(() -> {
            HashSet<String> all = helper2.getListFiles("/home/storm/terrace_root_test", 0, 100, null);
            helper2.getFtpClient().enterLocalPassiveMode();
            FileOutputStream fos = null;
            try {
                helper2.getFtpClient().setFileType(FTP.BINARY_FILE_TYPE);
                String currentFile = all.iterator().next();
                String fileName = FilenameUtils.getName(currentFile);

                fos = new FileOutputStream(new File(Constants.localPath + fileName));
                if (helper2.getFtpClient().retrieveFile(currentFile, fos)) {

                    fos.flush();
                    LocalDateTime end = LocalDateTime.now();
                    LOG.info("文件下载完成:{}", fileName);
                } else {
                    LOG.error("文件下载失败:{}", fileName);
                }
            } catch (IOException e) {
                LOG.error("IO异常了：{}", e);
            }
        }).start();
    }
}
