package com.bf.middleware.test.hadoop.mapreduce;

import com.bf.middleware.test.batch.TestBatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.WordCount;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.bf.framework.autoconfigure.batch.BatchProxy;
import org.bf.framework.autoconfigure.hadoop.HadoopProxy;
import org.bf.framework.autoconfigure.hadoop.batch.JobTasklet;
import org.bf.framework.boot.util.SpringUtil;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.hadoop.fs.FsShell;
import org.springframework.data.hadoop.mapreduce.JobRunner;

import java.io.File;
import java.util.concurrent.Callable;

import static org.apache.hadoop.fs.FileSystem.FS_DEFAULT_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class TestMapReduce implements BaseCoreTest {
    @Autowired
    @Qualifier("bf.hadoop.default_HadoopProxy")
    private HadoopProxy hadoopProxy;
    /**
     *  需要配置
            bf.hadoop.resourceManagerHost: ''
            bf.hadoop.resourceManagerPort: ''
     */

    @Autowired
    @Qualifier("bf.batch.default_BatchProxy")
    private BatchProxy proxy;
    @Test
    public void testWordCount() throws Exception {
        Configuration localCfg = hadoopProxy.getConfiguration();
        String inputDir = "/user/gutenberg/input/word";
        String outputDir = "/user/gutenberg/output/word";
        FsShell localShell = new FsShell(localCfg);
        JobRunner runner = new JobRunner();
        Job job = Job.getInstance(localCfg,"test-job-name");
        job.setJarByClass(WordCount.class);
//        job.setJar("file:/Users/bf/.m2/repository/org/apache/hadoop/hadoop-mapreduce-examples/3.3.6/hadoop-mapreduce-examples-*.jar");
        job.setMapperClass(WordCount.TokenizerMapper.class);
        job.setReducerClass(WordCount.IntSumReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
//        job.waitForCompletion(true);
        FileInputFormat.setInputPaths(job, new Path(localCfg.get(FS_DEFAULT_NAME_KEY) + inputDir));
        FileOutputFormat.setOutputPath(job, new Path(localCfg.get(FS_DEFAULT_NAME_KEY) + outputDir));
        runner.setJob(job);
        runner.setBeanFactory(SpringUtil.getContext());
        runner.afterPropertiesSet();
        runner.setPreAction(Lists.newArrayList((Callable<Object>) () -> {
            if (!localShell.test(inputDir)) {
                localShell.mkdir(inputDir);
                localShell.chmod(700, inputDir);
            }
            localShell.rm(inputDir+"/*");
            localShell.copyFromLocal("src/test/resources/data/nietzsche-chapter-1.txt", inputDir);
            if (localShell.test(outputDir)) {
                localShell.rmr(outputDir);
            }
            return null;
        }));

//        runner.setWaitForCompletion(false);
        runner.call();
    }

    /**
     * hadoopJob和spring-batch结合
     * 其中入口类需要打出jar才能运行，否则单元测试中执行不通过???
     * @throws Exception
     */
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

        String outPut = "/tweets/output";
        if (localShell.test(outPut)) {
            localShell.rmr(outPut);
        }
        Job hadoopJob = Job.getInstance(localCfg,"test-job-name");
        hadoopJob.setJarByClass(HashtagCount.class);
        hadoopJob.setMapperClass(HashtagCount.TokenizerMapper.class);
//        hadoopJob.setCombinerClass(HashtagCount.LongSumReducer.class);
        hadoopJob.setReducerClass(HashtagCount.LongSumReducer.class);
        hadoopJob.setOutputKeyClass(Text.class);
        hadoopJob.setOutputValueClass(LongWritable.class);
        FileInputFormat.setInputPaths(hadoopJob, new Path(localCfg.get(FS_DEFAULT_NAME_KEY) + inputDir));
        FileOutputFormat.setOutputPath(hadoopJob, new Path(localCfg.get(FS_DEFAULT_NAME_KEY) + outPut));

        //spring-batch
        JobTasklet hadoopJobTasklet = new JobTasklet();
        hadoopJobTasklet.setJob(hadoopJob);
        Step step = new StepBuilder("step1", proxy.getJobRepository())
                .tasklet(hadoopJobTasklet, proxy.getTransactionManager())
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
            localShell.get(outPut + "/part-r-*", "results.txt");
            String fileContents = FileUtils.readFileToString(new File("results.txt"));
            System.out.println(fileContents);
            return RepeatStatus.FINISHED;
        });

        Step step2 = new StepBuilder("step2", proxy.getJobRepository())
                .tasklet(scriptTasklet, proxy.getTransactionManager())
                .build();
        org.springframework.batch.core.Job job = new JobBuilder("mrBatchJob", proxy.getJobRepository())
                .start(step)
                .next(step2)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, TestBatch.getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }
}
