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

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplication} when spring web is not on the classpath.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("spring-web*.jar")
class SpringApplicationNoWebTests {

	@Test
	void detectWebApplicationTypeToNone() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	void specificApplicationContextClass() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(StaticApplicationContext.class);
		ConfigurableApplicationContext context = application.run();
		assertThat(context).isInstanceOf(StaticApplicationContext.class);
		context.close();
	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConfig {

	}

}
