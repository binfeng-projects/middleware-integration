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
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.PatternMatcher;

/**
 * @author Dave Syer
 *
 */
@Slf4j
public class MyStepListener2 implements StepExecutionListener {

	private String[] keys = null;

	private String[] statuses = new String[] { ExitStatus.COMPLETED.getExitCode() };

	private boolean strict = false;

	public void setKeys(String[] keys) {
		this.keys = keys;
	}

	public void setStatuses(String[] statuses) {
		this.statuses = statuses;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		ExecutionContext stepContext = stepExecution.getExecutionContext();
		ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
		String exitCode = stepExecution.getExitStatus().getExitCode();
		for (String statusPattern : statuses) {
			if (PatternMatcher.match(statusPattern, exitCode)) {
				for (String key : keys) {
					if (stepContext.containsKey(key)) {
						jobContext.put(key, stepContext.get(key));
					}
					else {
						if (strict) {
							throw new IllegalArgumentException(
									"The key [" + key + "] was not found in the Step's ExecutionContext.");
						}
					}
				}
				break;
			}
		}

		return null;
	}

}
