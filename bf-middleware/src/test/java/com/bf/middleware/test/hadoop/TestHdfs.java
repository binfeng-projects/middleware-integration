package com.bf.middleware.test.hadoop;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.bf.framework.autoconfigure.hadoop.HadoopProxy;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.hadoop.fs.FsShell;

@Slf4j
public class TestHdfs implements BaseCoreTest {
    @Autowired
    @Qualifier("bf.hadoop.default_HadoopProxy")
    private HadoopProxy proxy;
    @Test
    public void testCreateDir() {
        String inputDir = "/app/yarn-boot-simple";
        FsShell localShell = new FsShell(proxy.getConfiguration());
        if (!localShell.test(inputDir)) {
            localShell.mkdir(inputDir);
            localShell.chmod(700, inputDir);
        }
    }
    @Test
    public void testLocalHdfs() throws Exception {
        FsShell localShell = new FsShell(proxy.getConfiguration());
        for (FileStatus s : localShell.lsr("/")) {
            System.out.println("> " + s.getPath());
        }
    }
    @Test
    public void testRemoveHdfs() throws Exception {
        FsShell localShell = new FsShell(proxy.getConfiguration());
        localShell.rmr("/syarn");
        localShell.rmr("/app");
    }
    @Test
    public void testDownloadHdfs() throws Exception {
        FsShell localShell = new FsShell(proxy.getConfiguration());
        localShell.copyToLocal("/app/yarn-boot-simple/appmaster.jar","/Users/bf/Downloads/tmp");
    }
    @Test
    public void testShell() {
        String inputDir = "/app";
        FsShell localShell = new FsShell(proxy.getConfiguration());
        if (localShell.test(inputDir)) {
            localShell.chmod(true,777, inputDir);
        }
    }

}
