# batch_transform
FTP同步到HDFS工具
  1. 在main中启动定时任务去调度TaskRunner
  2. 支持连接多个FTP服务，每个FTP客户端启动一个线程进行文件拉取
  3. 文件先存放在本地目录，在将本地文件上传至HDFS
