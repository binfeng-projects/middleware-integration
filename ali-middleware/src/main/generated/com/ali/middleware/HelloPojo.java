package com.ali.middleware;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.bf.framework.autoconfigure.yarn.YarnProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.hadoop.fs.FsShell;
import org.springframework.yarn.annotation.OnContainerStart;
import org.springframework.yarn.annotation.YarnComponent;

@YarnComponent
@Slf4j
public class HelloPojo {

	@Autowired
//	@Qualifier("bf.yarn.default_YarnProxy")
	private YarnProxy proxy;

	@OnContainerStart
	public void publicVoidNoArgsMethod() {
		log.info("Hello from HelloPojo");
		log.info("About to list from hdfs root content");

		@SuppressWarnings("resource")
		FsShell shell = new FsShell(proxy.getYarnConfiguration());
		for (FileStatus s : shell.ls(false, "/")) {
			log.info(String.valueOf(s));
		}
	}

}
