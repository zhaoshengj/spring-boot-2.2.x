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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link PrometheusScrapeEndpoint}.
 *
 * @author Andy Wilkinson
 */
class PrometheusScrapeEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void prometheus() throws Exception {
		this.mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andDo(document("prometheus"));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		PrometheusScrapeEndpoint endpoint() {
			CollectorRegistry collectorRegistry = new CollectorRegistry(true);
			PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry((key) -> null, collectorRegistry,
					Clock.SYSTEM);
			new JvmMemoryMetrics().bindTo(meterRegistry);
			return new PrometheusScrapeEndpoint(collectorRegistry);
		}

	}

}
