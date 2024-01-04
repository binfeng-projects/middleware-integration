package com.ali.middleware.test.yarn;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.bf.framework.autoconfigure.yarn.YarnProxy;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TestYarn implements BaseCoreTest {
    @Autowired
//    @Qualifier("bf.yarn.default_YarnProxy")
    private YarnProxy proxy;

    /**
     * 单元测试本地启动相当于激活client和default的schema
     * java -jar 本地运行相当于,啥参数也不传，相当于激活client和yarn-store-groups的schema
     * java -jar xxx.jar --bf.yarn.mode=APPMASTER --bf.yarn.enabled=default 命令行参数可以覆盖任何默认参数
     */
    @Test
    public void testSubmit() throws Exception {
        ApplicationId appId = proxy.getYarnClient().submitApplication();
        System.out.println(appId.toString());
    }
}
