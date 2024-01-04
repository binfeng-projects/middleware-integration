/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.samples.adapter.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Task {

	public boolean doWork(ChunkContext chunkContext) {
		chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("done", "yes");
		return true;
	}

	public static class TestBean {

		private String value;

		public TestBean setValue(String value) {
			this.value = value;
			return this;
		}

		public void execute(String strValue, Integer integerValue, double doubleValue) {
			assertEquals("foo", value);
			assertEquals("foo2", strValue);
			assertEquals(3, integerValue.intValue());
			assertEquals(3.14, doubleValue, 0.01);
		}

	}
}