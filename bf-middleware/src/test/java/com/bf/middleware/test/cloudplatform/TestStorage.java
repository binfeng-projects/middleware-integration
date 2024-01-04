package com.bf.middleware.test.cloudplatform;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.bf.framework.boot.support.storage.StorageProxy;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public class TestStorage implements BaseCoreTest {
    @Autowired
    @Qualifier("bf.storage.sh_StorageProxy")
    private StorageProxy proxy;
    @Test
    public void testTencent() throws Exception {
        proxy.download("test");
    }
}
