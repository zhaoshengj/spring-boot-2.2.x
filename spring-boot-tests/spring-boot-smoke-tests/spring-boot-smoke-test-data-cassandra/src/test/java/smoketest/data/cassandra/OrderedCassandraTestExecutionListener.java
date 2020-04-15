/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.data.cassandra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionTestExecutionListener;

import org.springframework.core.Ordered;

public class OrderedCassandraTestExecutionListener extends CassandraUnitDependencyInjectionTestExecutionListener {

	private static final Log logger = LogFactory.getLog(OrderedCassandraTestExecutionListener.class);

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	protected void cleanServer() {
		try {
			super.cleanServer();
		}
		catch (Exception ex) {
			logger.warn("Failure during server cleanup", ex);
		}
	}

}
