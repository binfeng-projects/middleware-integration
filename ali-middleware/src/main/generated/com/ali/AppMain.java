package com.ali;


import lombok.extern.slf4j.Slf4j;
import org.bf.framework.autoconfigure.yarn.YarnProxy;
import org.bf.framework.boot.util.CommandLineUtil;
import org.bf.framework.common.util.CollectionUtils;
import org.bf.framework.common.util.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.yarn.config.annotation.EnableYarn;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.bf.framework.boot.constant.MiddlewareConst.PREFIX_YARN;

/**
 * 单元测试本地启动相当于激活client和default的schema
 * java -jar 本地运行相当于,啥参数也不传，相当于激活client和yarn-store-groups的schema
 * java -jar xxx.jar --bf.yarn.mode=APPMASTER --bf.yarn.enabled=default 命令行参数可以覆盖任何默认参数(方便调试)
 */
@Slf4j
public class AppMain {
    private static final String YARN_MODE_KEY = "bf.yarn.mode";
    public static void main(String[] args) {
        Map<String,String> paramMap = CommandLineUtil.resolveArgs(args);
        //激活默认schema，可以被命令行覆盖。
        List<String> params = CommandLineUtil.argActiveSingleSchema(PREFIX_YARN,"yarn-store-groups",paramMap);
//        List<String> params = CommandLineUtil.argActiveSingleSchema(PREFIX_YARN,"yarn-batch-app",paramMap);
        String modeStr = paramMap.get(YARN_MODE_KEY);
        //都是同一个jar运行，判断运行模式
        EnableYarn.Enable mode = EnableYarn.Enable.CLIENT;
        if(StringUtils.isBlank(modeStr)) { //默认yarn mode client
            params.add("--" + YARN_MODE_KEY + "=" + EnableYarn.Enable.CLIENT.name());
        } else {
            for (EnableYarn.Enable m : EnableYarn.Enable.values()) {
                if(modeStr.equalsIgnoreCase(m.name())) {
                    mode = m;
                    break;
                }
            }
        }
        String[] finalArgs = CollectionUtils.toArray(params,String.class);
        ConfigurableApplicationContext ctx = BootStarter.run(null,finalArgs);
        YarnProxy yarnProxy = ctx.getBean(YarnProxy.class);
        try {
            if(EnableYarn.Enable.CLIENT.equals(mode)) {
                yarnProxy.getYarnClient().submitApplication();
            } else if(EnableYarn.Enable.APPMASTER.equals(mode)) {
                if(yarnProxy.getYarnJobLauncher() != null) {
                    Step step = new StepBuilder("step1", yarnProxy.getBatchProxy().getJobRepository())
                            .tasklet((contribution, chunkContext) -> {
                                System.out.println("Hello world!");
                                return RepeatStatus.FINISHED;
                            }, yarnProxy.getBatchProxy().getTransactionManager())
                            .build();
                    Job job = new JobBuilder("HelloWorldJob", yarnProxy.getBatchProxy().getJobRepository())
                            .start(step)
                            .build();
                    yarnProxy.getYarnJobLauncher().setJobs(Collections.singleton(job));
                }
                yarnProxy.getAppmasterLauncherRunner().run(finalArgs);
            } else {
                yarnProxy.getContainerLauncherRunner().run(finalArgs);
            }
        } catch (Exception e) {
            log.error("Yarn run error{}",finalArgs,e);
        }
    }
}
