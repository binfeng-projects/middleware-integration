## hadoop
1. bin/hdfs namenode -format
2. sbin/start-dfs.sh
http://localhost:9870/
3. bin/hdfs dfs -mkdir -p /user/u1
4. sbin/start-yarn.sh
http://localhost:8088/
5. sbin/stop-yarn.sh


## hive
1. 拷贝mysql驱动到lib下面
2. bin/schematool -initSchema -dbType mysql
3. bin/hiveserver2    (tmux)


## hbase
1. bin/start-hbase.sh
2. bin/hbase shell
3. create_namespace 'test'
4. create 'test:log', 'f'


## batch
1. CREATE DATABASE `test_batch` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
2. idea 搜索schema- 执行 classpath:org/springframework/batch/core/schema-@@platform@@.sql
3. idea 搜索schema- 执行 classpath:org/springframework/batch/samples/common/business-schema-@@platform@@.sql

## flink log冲突排错
一般springboot应用默认使用logback,提交job到web界面集群后会报错
1. 官方说，需要把发行版flink/lib下加入logback-core和logback-classic（注意根据官方文档需要版本小于1.3）,但是如果版本过低，可能和应用使用的logback会冲突，所以，没有执行
2. 本地shade-plugin <exclude>ch.qos.logback:*</exclude> ,排除logback包 (必须执行),logback存在的情况下，无论如何都报错
3. 删除flink/lib下所有日志相关的jar,可以正常运行，console会输出，log API不生效

