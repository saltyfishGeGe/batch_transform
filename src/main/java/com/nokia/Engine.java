package com.nokia;

import com.nokia.common.Constants;
import com.nokia.ftpreader.FtpHelper;
import com.nokia.ftpreader.StandardFtpHelper;
import com.nokia.hdfsWriter.HdfsHelper;
import com.nokia.utils.LogBackConfigLoader;
import com.nokia.utils.PropertiesUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.nokia.common.FtpParameter.*;

/**
 * _oo0oo_
 * o8888888o
 * 88" . "88
 * (| -_- |)
 * 0\  =  /0
 * ___/`---'\___
 * .' \\|     | '.
 * / \\|||  :  ||| \
 * / _||||| -:- |||||- \
 * |   | \\\  -  / |   |
 * | \_|  ''\---/''  |_/ |
 * \  .-\__  '-'  ___/-. /
 * ___'. .'  /--.--\  `. .'___
 * ."" '<  `.___\_<|>_/___.' >' "".
 * | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 * \  \ `_.   \_ __\ /__ _/   .-` /  /
 * =====`-.____`.___ \_____/___.-`___.-'=====
 * `=---='
 * <p>
 * <p>
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * <p>
 * 佛祖保佑         永无BUG
 **/

public class Engine implements Job {
    /**
     * 思路：
     * 1. 初始化 ftpClient 和 hdfsClient
     * 2. 获取ftp下同步的所有文件数量，（暂定一天2T数据量,一小时83G，五分钟一个批次调度，暂定使用5个线程大小的线程池进行调用）
     * 3. 每个子线程从FTP拉文件到本地目录，下载成功后进行move操作。hdfsClient从本地目录将文件同步到hdfs上，同步完成后从input迁移至bak
     *
     * @param args
     */

    public static final Logger LOG = LoggerFactory.getLogger(Engine.class);

    private Set<FtpHelper> ftpHelperArr = new HashSet<>();

    private Map<String, Set<String>> ftpFilesMap = new HashMap<>();

    private HdfsHelper hdfsHelper = null;

    private Map<String, Set<String>> unDownloadFileMap = new ConcurrentHashMap<>();


    /**
     * @Description: 程序启动入口
     * @Param:
     * @return:
     * @Author: xianyu
     * @Date: 16:16
     */
    public void start(String[] args) {

        // 初始化FTP和hadoop相关连接和配置
        this.init();

        // 获取所有文件
        this.prepare();

        LOG.info("Engine execute...");
        //启多线程下载上传, 以一个FTP服务器为一个线程去处理
        ExecutorService executorService = Executors.newFixedThreadPool(this.ftpHelperArr.size());
        for (FtpHelper ftpHelper : this.ftpHelperArr) {
            Set<String> ffs = ftpFilesMap.get(ftpHelper.currentIP());

            // ftp连接不成功 或 目录下无文件时，ffs会为null或0
            if (ffs != null && ffs.size() > 0) {
                executorService.execute(new TaskRunner(ftpHelper, hdfsHelper, ffs));
            }
        }

        executorService.shutdown();
        while (true) {
            if (executorService.isTerminated()) {
                this.logout();
                LOG.info("CDR同步结束时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss")));
                break;
            }
        }
    }

    /**
     * @Description: 初始化方法
     * @Param:
     * @return:
     * @Author: xianyu
     * @Date: 16:17
     */
    private void init() {
        LOG.info("Engine init...");
        // 获取多个FTP IP地址
        HashSet<String> ftpServers = PropertiesUtils.getFtpServers();

        LOG.info("初始化ftp服务器连接");

        FtpHelper ftpHelper = null;
        for (String ftpInfo : ftpServers) {
            String ftpIp = ftpInfo.split(",")[0];
            String scanPath = ftpInfo.split(",")[1];

            // ftp连接是否创建，若未创建则创建
            Optional<FtpHelper> exist = this.ftpHelperArr.stream().filter(f -> ftpIp.equals(f.currentIP())).findFirst();
            // 若已存在则跳过
            if (exist.isPresent()) {
                // ftpHelper已存在，判断是否登录正常
                FtpHelper existHelper = exist.get();
                LOG.info("当前FTP连接：{}已存在", existHelper.currentIP());
                // 之前的FTP连接已退出，则重新登录
                if (!existHelper.getFtpClient().isConnected()) {
                    LOG.info("当前FTP连接：{}已退出登录，将重新登录", existHelper.currentIP());
                    existHelper.loginFtpServer();
                }
                continue;
            } else {
                ftpHelper = new StandardFtpHelper(ftpIp,
                        username,
                        password,
                        port,
                        timeout,
                        connectMode);
                // 目前使用同一套用户名、密码、文件路径
                ftpHelper.loginFtpServer();

                LOG.info("创建新FTP连接：{}", ftpHelper.currentIP());
                // 设置当前线程扫描路径
                ftpHelper.setScanPath(scanPath);

                // 初始化完成添加进集合中
                this.ftpHelperArr.add(ftpHelper);

                // 初始化FTP文件数量
                ftpFilesMap.put(ftpIp, null);
                // 用于存放各个连接，每次定时任务处理不完的文件:即上一次的任务没处理完遗留到下一次任务启动
                // 防止后续任务重复处理该任务将未处理完的文件存在该集合中
                unDownloadFileMap.put(ftpIp, new HashSet<String>());
            }
        }
        LOG.info("初始化ftp服务器连接配置完成");

        boolean allFail = true;
        for (FtpHelper conn : this.ftpHelperArr) {
            if (conn.isConnect()) {
                allFail = false;
            }
        }
        if (allFail) {
            LOG.error("当前所有FTP连接均无法成功连接，请检查FTP连接参数是否存在异常");
        }

        // hdfs 连接初始化
        if(this.hdfsHelper == null){
            LOG.info("初始化HDFS连接...");
            this.hdfsHelper = new HdfsHelper();
            this.hdfsHelper.init();
        } else {

        }

        //初始化本地路径
        LOG.info("初始化本地目录");
        File localPath = new File(Constants.localPath);
        File localBakPath = new File(Constants.localBakPath);
        File localErrorPath = new File(Constants.localErrorPath);

        // 初始化本地路径
        if (!localPath.exists()) {
            localPath.mkdirs();
        }

        if (!localBakPath.exists()) {
            localBakPath.mkdirs();
        }

        if (!localErrorPath.exists()) {
            localErrorPath.mkdirs();
        }
    }


    private void prepare() {

        LOG.info("Engine prepare...");
        for (FtpHelper ftpHelper : this.ftpHelperArr) {
            if (!ftpHelper.isDirExist(ftpHelper.getScanPath())) {
                LOG.error("{} FTP服务器不存在路径{}, 请检查路径是否正确", ftpHelper.currentIP(), ftpHelper.getScanPath());
                continue;
            }

            LOG.info("FTP：{}，下载路径为：{}", ftpHelper.currentIP(), ftpHelper.getScanPath());
            // 取出原有下载文件集合，与当前文件清单进行过滤，防止文件重复处理
            // 由于存在move机制，通过一个定时任务去更新重复文件集合中的信息
            // 下一次运行时取出原有数据结合的数据，这部分数据是会出现重复下载的情况
            Set<String> beforeFiles = ftpFilesMap.get(ftpHelper.currentIP());

            if (beforeFiles != null && beforeFiles.size() > 0) {
                LOG.info("上一个任务存在遗留文件未处理，后续任务将过滤该文件清单，FTP:{}, 文件总数：{}， 文件列表：{}", ftpHelper.currentIP(), beforeFiles.size(), beforeFiles);
            }

            Set<String> currentFtpBeforeFiles = this.unDownloadFileMap.get(ftpHelper.currentIP());

            if(currentFtpBeforeFiles.size() > 0){
                LOG.info("FTP:{}, 存在历史未处理完毕文件，文件数：{}， 文件列表:{}", currentFtpBeforeFiles.size(), currentFtpBeforeFiles);
            }

            if (beforeFiles != null && beforeFiles.size() > 0) {
                currentFtpBeforeFiles.addAll(beforeFiles);
                this.unDownloadFileMap.put(ftpHelper.currentIP(), currentFtpBeforeFiles);
            }

            HashSet<String> ftpFiles = ftpHelper.getAllFiles(Arrays.asList(ftpHelper.getScanPath()),
                    0,
                    100,
                    Arrays.asList("tmp")); // 过滤后缀为.tmp的文件

            if (ftpFiles == null || ftpFiles.size() == 0) {
                LOG.error("{} FTP指定目录未扫描到可下载文件", ftpHelper.currentIP());
            } else {
                // 文件清单过滤掉重复处理的文件
                LOG.info("FTP:{}, 本次扫描文件总数：{}", ftpHelper.currentIP(), ftpFiles.size());
                ftpFiles = (HashSet<String>) ftpFiles.stream().filter(f -> !currentFtpBeforeFiles.contains(f)).collect(Collectors.toSet());
                LOG.info("FTP:{}, 过滤历史未处理文件，本次实际下载文件总数：{}",ftpHelper.currentIP(), ftpFiles.size());
                // 覆盖初始化时给的null值
                ftpFilesMap.put(ftpHelper.currentIP(), ftpFiles);
            }
        }

    }

    private void logout() {
        // 关闭连接时，要判断全局是否文件已经完全处理完，要注意两次定时任务存在同时执行的情况，公用同一套helper会导致另外一个任务被中断
        // 当前任务的文件清单未处理完毕，不允许关闭连接
        LOG.info("准备退出FTP/HDFS连接");
        for (Set<String> files : ftpFilesMap.values()) {
            if (files != null && files.size() > 0) {
                LOG.info("当前任务存在未处理完毕的文件，FTP连接关闭失败");
                return;
            }
        }
        LOG.info("当前任务所有文件已处理完毕");
        boolean canClose = true;
        // 判断FTP的未处理完的文件是否处理完毕  -- 这个判断是用于历史任务存在未处理完的文件，防止本次文件处理完毕关闭连接影响到其他任务的进行
        Iterator<FtpHelper> ftpHelperArrIterator = ftpHelperArr.iterator();
        while (ftpHelperArrIterator.hasNext()) {
            FtpHelper helper = ftpHelperArrIterator.next();

            // 最新ftp文件清单
            HashSet<String> currentFiles = helper.getAllFiles(Arrays.asList(helper.getScanPath()),
                    0,
                    100,
                    Arrays.asList("tmp")); // 过滤后缀为.tmp的文件
            Set<String> unDownloadAllFiles = unDownloadFileMap.get(helper.currentIP());

            // 判断未处理完的文件是否还存在， -- 这儿因为处理完会move ftp的文件，也可以从localPath去判断是否处理完了文件
            Set<String> unDownloadFiles = unDownloadAllFiles.stream().filter(re -> currentFiles.contains(re)).collect(Collectors.toSet());
            if (unDownloadFiles != null && unDownloadFiles.size() > 0) {
                // 更新未处理文件清单
                unDownloadFileMap.put(helper.currentIP(), unDownloadFiles);
                canClose = false;
                LOG.info("当前存在历史任务未下载完的文件，FTP关闭失败：{}, 文件数量：{}", helper.currentIP(), unDownloadFiles.size());
                return;
            } else {
                // 未处理文件已全部处理完
                LOG.info("历史未处理文件已全部处理完毕，连接准备退出");
                unDownloadFileMap.put(helper.currentIP(), new HashSet<>());
                // 退出当前FTP连接
                helper.logoutFtpServer();
                // 当前集合移除FTP连接
                ftpHelperArrIterator.remove();
            }
        }
        if (canClose && this.hdfsHelper != null) {
            this.hdfsHelper.close();
            this.hdfsHelper = null;
        } else {
            LOG.info("当前存在正在处理的FTP连接，HDFS无法退出");
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        LocalDateTime start = LocalDateTime.now();
        LOG.info("CDR同步开始时间：" + start.format(DateTimeFormatter.ofPattern("yyyyMMdd hh:mm:ss")));

        // 每次调度独立生成一套日志
        LogBackConfigLoader.load(Engine.class.getClassLoader().getResource("logback.xml").getPath());

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Engine engine = (Engine) jobDataMap.get("engine");
        engine.start(null);

        LOG.info("本次同步耗时:{}毫秒", Duration.between(start, LocalDateTime.now()).toMillis());
    }

    public static void main(String[] args) throws SchedulerException, InterruptedException {

        // 实例化当前工程实例
        Engine engine = new Engine();
//        engine.start(args);
        // 1、创建调度器Scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("engine", engine);

        // 2、创建JobDetail实例，并与任务绑定
        JobDetail cdrJob = JobBuilder.newJob(Engine.class)
                .usingJobData("CDR_JOB_Detail", "CDR_JOB")
                .setJobData(jobDataMap) //任务使用同一个实例进行
                .withIdentity("CDR_JOB", "CDR_JOB_GROUP").build();

        // 3、构建Trigger实例,每隔1s执行一次
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity("CDR_Trigger", "CDR_TriggerGroup")
                .usingJobData("trigger", "CDR_JOB trigger")
                .startNow()//立即生效
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/2 * * * ?")) //每5分钟执行一次
                .build();


        //4、执行
        scheduler.scheduleJob(cdrJob, cronTrigger);
        System.out.println("--------cdr scheduler start ! ------------");
        scheduler.start();
    }
}
