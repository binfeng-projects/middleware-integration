package com.bf.middleware.test.hive;

import com.bf.middleware.test.batch.TestBatch;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.bf.framework.autoconfigure.batch.BatchProxy;
import org.bf.framework.autoconfigure.hadoop.HadoopProxy;
import org.bf.framework.autoconfigure.hive.support.HiveRunnerTasklet;
import org.bf.framework.autoconfigure.hive.support.HiveScript;
import org.bf.framework.autoconfigure.hive.support.HiveTemplate;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.hadoop.fs.FsShell;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * bin/hadoop fs -mkdir /tmp
 * bin/hadoop fs -chmod a+w /tmp
 * bin/hadoop fs -mkdir -p /user/hive/warehouse
 * bin/hadoop fs -chmod a+w /user/hive/warehouse
 */
public class TestHive implements BaseCoreTest {

    @Autowired
    @Qualifier("bf.hadoop.default_HiveTemplate")
    private HiveTemplate defaultTemplate;
    @Autowired
    @Qualifier("bf.hadoop.default_HadoopProxy")
    private HadoopProxy hadoopProxy;

    @Autowired
    @Qualifier("bf.batch.default_BatchProxy")
    private BatchProxy proxy;

    @Test
    public void testTemplate() throws IOException {
        Configuration localCfg = hadoopProxy.getConfiguration();
        FsShell localShell = new FsShell(localCfg);
        String inputDir = "/user/hive/input";
        if (!localShell.test(inputDir)) {
            localShell.mkdir(inputDir);
            localShell.chmod(700, inputDir);
        }
        localShell.rm(inputDir+"/*");
        localShell.copyFromLocal("src/test/resources/data/passwd", inputDir);

        Map parameters = new HashMap();
        parameters.put("inputFile", "/user/hive/input/passwd");
        defaultTemplate.query("classpath:password-analysis.hql", parameters);
    }
    @Test
    public void testContribJar() throws Exception {
        Configuration localCfg = hadoopProxy.getConfiguration();
        FsShell localShell = new FsShell(localCfg);
        String inputDir = "/user/hive/input";
        if (!localShell.test(inputDir)) {
            localShell.mkdir(inputDir);
            localShell.chmod(700, inputDir);
        }
        localShell.rm(inputDir+"/*");
        localShell.copyFromLocal("src/test/resources/data/apache", inputDir);
        //hive-contrib有些扩展函数
        localShell.copyFromLocal("/Users/bf/.m2/repository/org/apache/hive/hive-contrib/3.1.3/hive-contrib-3.1.3.jar", inputDir);
//        localShell.copyFromLocal("src/test/resources/data/hive-contrib-3.1.3.jar", inputDir);

        String outputDir = "/user/hive/out";
        if (!localShell.test(outputDir)) {
            localShell.mkdir(outputDir);
            localShell.chmod(700, outputDir);
        }
        localShell.rm(outputDir+"/*");
        HiveRunnerTasklet runner = new HiveRunnerTasklet();
        runner.setHiveTemplate(defaultTemplate);
        Map parameters = new HashMap();
        parameters.put("hiveContribJar", "hdfs://" + inputDir + "/hive-contrib-3.1.3.jar");
        parameters.put("localInPath", inputDir + "/apache");
        HiveScript script = new HiveScript(new ClassPathResource("apache-log-simple.hql"),parameters);
        runner.setScripts(Lists.newArrayList(script)); //可以传入多个脚本
        runner.call();
    }
    @Test
    public void testBatch() throws Exception {
        Configuration localCfg = hadoopProxy.getConfiguration();
        FsShell localShell = new FsShell(localCfg);
        String inputDir = "/tweets/input";
        if (!localShell.test(inputDir)) {
            localShell.mkdir(inputDir);
            localShell.chmod(700, inputDir);
        }
        localShell.rm(inputDir+"/*");
        localShell.copyFromLocal("src/test/resources/data/nbatweets-small.txt", inputDir);

        String tmpDir = "/tmp";
        if (!localShell.test(tmpDir)) {
            localShell.mkdir(tmpDir);
            localShell.chmod("a+w", tmpDir);
        }

        String wareHouseDir = "/user/hive/warehouse";
        if (!localShell.test(wareHouseDir)) {
            localShell.mkdir(wareHouseDir);
            localShell.chmod("a+w", wareHouseDir);
        }
        String outPut = "/tweets/hiveout";
        HiveRunnerTasklet hiveTasklet = new HiveRunnerTasklet();
        hiveTasklet.setHiveTemplate(defaultTemplate);
        HiveScript hiveScript = new HiveScript(new ClassPathResource("tweet-influencers.hql"));
        hiveTasklet.setScripts(Collections.singleton(hiveScript));

        //spring-batch
        Step step = new StepBuilder("step1", proxy.getJobRepository())
                .tasklet(hiveTasklet, proxy.getTransactionManager())
                .build();

        CallableTaskletAdapter scriptTasklet = new CallableTaskletAdapter();
        scriptTasklet.setCallable(() -> {
            //groovy脚本怎么运行？
//            scriptEvaluator.evaluate(new ResourceScriptSource(new ClassPathResource("results.groovy")),Map.of("outputDir",outPut,"fsh",localShell));
            //脚本中内容用Java实现
            File old = new File("results.txt");
            if( old.exists() ) {
                old.delete();
            }
            localShell.get(outPut + "/*", "results.txt");
            String fileContents = FileUtils.readFileToString(new File("results.txt"));
            System.out.println(fileContents);
            return RepeatStatus.FINISHED;
        });

        Step step2 = new StepBuilder("step2", proxy.getJobRepository())
                .tasklet(scriptTasklet, proxy.getTransactionManager())
                .build();
        Job job = new JobBuilder("hiveBatchJob", proxy.getJobRepository())
                .start(step)
                .next(step2)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, TestBatch.getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }
}
