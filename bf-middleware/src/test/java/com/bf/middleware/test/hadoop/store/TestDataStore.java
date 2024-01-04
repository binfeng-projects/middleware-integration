package com.bf.middleware.test.hadoop.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.bf.framework.autoconfigure.hadoop.HadoopProxy;
import org.bf.framework.common.util.IOUtils;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.kitesdk.data.Formats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.hadoop.store.DataStoreWriter;
import org.springframework.data.hadoop.store.StoreException;
import org.springframework.data.hadoop.store.dataset.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  服务端删除/tmp下面所有文件，再stop-all。再重新按照步骤启动
 */
@Slf4j
public class TestDataStore implements BaseCoreTest {
    @Autowired
    @Qualifier("bf.hadoop.default_HadoopProxy")
    private HadoopProxy proxy;
    private DatasetOperations datasetOperations;

    private DataStoreWriter<FileInfo> writer;

    private long count;

    protected static DatasetDefinition fileInfoDatasetDefinition() {
        DatasetDefinition definition = new DatasetDefinition();
        definition.setFormat(Formats.AVRO.getName());
        definition.setTargetClass(FileInfo.class);
        definition.setAllowNullValues(false);
        return definition;
    }

    /**
     *
     * Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException): Permission denied: user=bf, access=WRITE, inode="/user/u1":root:supergroup:drwxr-xr-x
     * 环境变量中添加export HADOOP_USER_NAME=root
     *
     *
     * There are 1 datanode(s) running and 1 node(s) are excluded in this operation
     */
    @Test
    public void testLocalHdfs() throws Exception {
        String basePath = "/user/u1";
        Configuration cfg = proxy.getConfiguration();
        initHadoopCfg(cfg,basePath);
        doProcess("/Users/bf/Downloads/tmp");
    }

    public void doProcess(String localFilePath) {
        File f = new File(localFilePath);
        try {
            processFile(f);
        } catch (IOException e) {
            throw new StoreException("Error writing FileInfo", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        final AtomicLong count = new AtomicLong();
        datasetOperations.read(FileInfo.class, new RecordCallback<FileInfo>() {
            @Override
            public void doInRecord(FileInfo record) {
                count.getAndIncrement();
            }
        });
        System.out.println("File count: " + count.get());
        System.out.println("Done!");
    }
    public void initHadoopCfg(Configuration cfg,String basePath) {
//        1.定义DatasetRepositoryFactory,需要配置信息
        DatasetRepositoryFactory fac = new DatasetRepositoryFactory();
        fac.setConf(cfg);
        fac.setBasePath(basePath);
        fac.setNamespace("test");
        try {
            fac.afterPropertiesSet();
        } catch (Exception e){
            log.error("init DatasetRepositoryFactory error",e);
            throw new RuntimeException("init DatasetRepositoryFactory error");
        }
//        2. 定义DatasetDefinition，有点类似数据库的schema。定义怎么写入数据，数据字段，格式等（这里是Avro序列化方式）
        DatasetDefinition ddf = fileInfoDatasetDefinition();
//        3.定义 DataStoreWriter<FileInfo>, 真正写入
        writer = new AvroPojoDatasetStoreWriter<FileInfo>(FileInfo.class, fac, ddf);
//        4.定义 DatasetOperations, 执行一些回调？？
        DatasetTemplate datasetOperations = new DatasetTemplate();
        datasetOperations.setDatasetDefinitions(Arrays.asList(ddf));
        datasetOperations.setDatasetRepositoryFactory(fac);
        this.datasetOperations = datasetOperations;
    }

    private void processFile(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                processFile(f);
            }
        } else {
            if (++count % 10000 == 0) {
                System.out.println("Writing " + count + " ...");
            }
            FileInfo fileInfo = new FileInfo(file.getName(), file.getParent(), (int)file.length(), file.lastModified());
            writer.write(fileInfo);
        }
    }
}
