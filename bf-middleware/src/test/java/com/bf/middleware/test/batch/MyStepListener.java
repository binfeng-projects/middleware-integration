/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bf.middleware.test.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.annotation.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.file.multiline.Trade;

/**
 * @author Dave Syer
 *
 */
@Slf4j
public class MyStepListener {

	private static int processSkips;

	public static int getProcessSkips() {
		return processSkips;
	}

	public static void resetProcessSkips() {
		processSkips = 0;
	}
	@BeforeStep
	public void beforeStep(StepExecution stepExecution) {
		stepExecution.getExecutionContext().put("stepName", stepExecution.getStepName());
	}
	@AfterStep
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (!stepExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new ExitStatus("COMPLETED WITH SKIPS");
		}
		else {
			return null;
		}
	}
	@OnSkipInWrite
	public void skipWrite(Trade trade, Throwable t) {
		log.info("Skipped writing " + trade);
	}
	@OnSkipInProcess
	public void skipProcess(Trade trade, Throwable t) {
		processSkips++;
	}
	@BeforeChunk
	public void beforeChunk(ChunkContext context) {
	}

	@AfterChunk
	public void afterChunk(ChunkContext context) {
	}

	@AfterChunkError
	public void afterChunkError(ChunkContext context) {
	}

	@BeforeRead
	public void beforeRead() {
	}

	@AfterRead
	public void afterRead(CustomerCredit item) {
	}

	@OnReadError
	public void onReadError(Exception ex) {
	}

	@BeforeWrite
	public void beforeWrite(Chunk<? extends CustomerCredit> items) {
	}

	@AfterWrite
	public void afterWrite(Chunk<? extends CustomerCredit> items) {
	}

	@OnWriteError
	public void onWriteError(Exception exception, Chunk<? extends CustomerCredit> items) {
	}

	@BeforeProcess
	public void beforeProcess(CustomerCredit item) {
	}
	@AfterProcess
	public void afterProcess(CustomerCredit item, CustomerCredit result) {
	}
	@OnProcessError
	public void onProcessError(CustomerCredit item, Exception e) {
	}
}
