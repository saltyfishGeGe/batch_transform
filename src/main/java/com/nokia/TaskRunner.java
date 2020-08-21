package com.nokia;

import com.nokia.common.Constants;
import com.nokia.ftpreader.FtpFileBean;
import com.nokia.ftpreader.FtpHelper;
import com.nokia.hdfsWriter.HdfsHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.io.CopyStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @Description: 任务执行器
 * @Author: xianyu
 * @Date: 15:43
 */
public class TaskRunner implements Runnable {

    private Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private FtpHelper ftpHelper;

    private HdfsHelper hdfsHelper;

    private Set<String> allFiles;

    private long totalDownlaodFileSize = 0l;

    private int totalDownloadFileNum = 0;

    private long totalUploadHdfsFileSize = 0l;

    private int totalUploadHdfsFileNum = 0;

    public TaskRunner(FtpHelper ftpHelper, HdfsHelper hdfsHelper, Set<String> allFiles) {
        this.ftpHelper = ftpHelper;
        this.hdfsHelper = hdfsHelper;
        this.allFiles = allFiles;
    }

    @Override
    public void run() {

        LOG.info("线程{},开始启动。", Thread.currentThread().getName());

        Set<FtpFileBean> scanAllFilesInfo = this.ftpHelper.getScanFtpAllFilesInfo();

        // 当前处理的文件
        String currentFile;

        Iterator<String> ftpFileIterator = allFiles.iterator();
        // 这里不采用平均分配的原则，哪个线程执行完接着搞
        while (ftpFileIterator.hasNext()) {
            currentFile = ftpFileIterator.next();
            // 删除allFiles中文件信息
            // 下载成功与否都要移除
            ftpFileIterator.remove();

            LOG.info("线程{}，开始处理文件{}", Thread.currentThread().getName(), currentFile);

            // 接下来处理文件

            // 1. 从FTP将文件下载到本地,保存至本地
            // InputStream inputStream = null;
            FileOutputStream fos = null;
            String fileName = FilenameUtils.getName(currentFile);
            String localPath = Constants.localPath + fileName;

            // 获取扫描的ftp文件基础信息
            FtpFileBean ftpFileInfo = scanAllFilesInfo.stream().filter(f -> f.equals(new FtpFileBean().setFileName(fileName))).findFirst().get();
            // 本地文件
            File ff = new File(localPath);
            try {
                LocalDateTime start = LocalDateTime.now();
                LOG.info("{}开始下载文件：{}, 文件大小：{}", Thread.currentThread().getName(), fileName, ftpFileInfo.getFileSize());

                if (!ff.exists()) {
                    ff.createNewFile();
                }
//
                // 每次数据连接之前，ftp client告诉ftp server开通一个端口来传输数据
                // 因为ftp server可能每次开启不同的端口来传输数据，但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞。
                this.ftpHelper.getFtpClient().enterLocalPassiveMode();
                this.ftpHelper.getFtpClient().setFileType(FTP.BINARY_FILE_TYPE);
                fos = new FileOutputStream(ff);
                if (this.ftpHelper.getFtpClient().retrieveFile(currentFile, fos)) {

                    fos.flush();
                    LocalDateTime end = LocalDateTime.now();
                    LOG.info("文件下载完成:{}，本次下载消耗时间为{}毫秒, 文件大小：{}", fileName, Duration.between(start, end).toMillis(), ftpFileInfo.getFileSize());

                    totalDownloadFileNum ++;
                    totalDownlaodFileSize += ftpFileInfo.getFileSize();
                } else {
                    LOG.error("文件下载失败:{}", fileName);
                    // 这种错误一般是网络问题，重新跑多一次就可以了
                    if(ff.exists()){
                        if(ff.delete()){
                            LOG.error("删除下载错误文件成功：{}", ff);
                        } else{
                            LOG.error("删除下载错误文件失败，该文件将残留在本地目录中：{}", ff);
                        }
                    }
                    continue;
                }

            } catch (FileNotFoundException e) {
                LOG.error("FTP文件{}不存在，下载到本地失败，理由：{}", currentFile, e);
                continue;
            } catch (IOException e) {
                // 在此处偶尔会出现 IOException caught while copying. Read timed out等异常，貌似是因为网络问题
                // 若是出现频繁可以考虑加入异常重连机制来解决网络短时间不稳定的问题
                LOG.error("文件:{}, 下载到本地出错,错误信息：{}", currentFile, e);
                // 下载失败的残留文件删除
                if(ff.exists()){
                    if(ff.delete()){
                        LOG.error("删除下载错误文件成功：{}", ff);
                    } else{
                        LOG.error("删除下载错误文件失败，该文件将残留在本地目录中：{}", ff);
                    }
                }
                continue;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 3. 移动FTP文件至已完成目录
//            try{
//                ftpHelper.moveFtpFile(currentFile, Constants.ftpBakPath + fileName);
//                LOG.info("FTP文件移动至备份目录成功，文件名：{}, 源文件:{}, 备份后文件：{}", fileName, currentFile, Constants.ftpBakPath + fileName);
//            }catch (Exception e){
//                e.printStackTrace();
//                LOG.error("FTP文件:{}, 移动到备份目录出错,错误信息：{}", fileName, e);
//                continue;
//            }

            // 4. 上传文件到HDFS
            File source = new File(localPath);
            if (!hdfsHelper.isFileExist(Constants.hdfsPath + fileName)) {
                try {
                    hdfsHelper.upload2HDFS(localPath, Constants.hdfsPath + fileName);
                    totalUploadHdfsFileNum ++;
                    totalUploadHdfsFileSize += source.length();
                } catch (Exception e) {
                    LOG.error("本地文件:{}, 上传至HDFS出错,错误信息：{}", localPath, e);
                    continue;
                }

                // 5. 移动本地文件至备份目录
                try {
                    File bak = new File(Constants.localBakPath + fileName);
                    // 文件移动
                    source.renameTo(bak);
                    LOG.info("HDFS文件上传成功，移动本地文件至备份目录:{}", bak);
                } catch (Exception e) {
                    LOG.error("本地文件:{}, 移动至备份目录出错,错误信息：{}", localPath, e);
                    continue;
                }
            } else {
                // 若是HDFS已存在文件，则存放在本地错误目录
                try {
                    File error = new File(Constants.localErrorPath + fileName);
                    // 文件移动
                    source.renameTo(error);
                    LOG.info("HDFS文件已存在：{}，移动本地文件至错误目录:{}", Constants.hdfsPath + fileName, error);
                } catch (Exception e) {
                    LOG.error("本地文件:{}, 移动至错误目录出错,错误信息：{}", localPath, e);
                    continue;
                }
            }

//            try {
//                for(int i = 0 ; i< 2 ; i++){
//                    LOG.info("休息10s....");
//                    Thread.sleep(10000);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        LOG.info("FTP服务{}，处理线程 {}退出！", ftpHelper.currentIP(), Thread.currentThread().getName());
        LOG.info("FTP服务{}, 本次扫描文件数：{}", ftpHelper.currentIP(), scanAllFilesInfo.size() );
        LOG.info("FTP服务{}，本次总下载文件数：{}, 总下载文件大小：{}", ftpHelper.currentIP(), totalDownloadFileNum, totalDownlaodFileSize);
        LOG.info("FTP服务{}，本次上传HDFS总文件数：{}, 上传总文件大小：{}", ftpHelper.currentIP(), totalUploadHdfsFileNum, totalUploadHdfsFileSize);
    }
}
