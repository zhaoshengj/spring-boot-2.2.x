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

package org.springframework.boot.web.reactive.result.view;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Brian Clozel
 */
class MustacheViewResolverTests {

	private final String prefix = "classpath:/" + getClass().getPackage().getName().replace(".", "/") + "/";

	private MustacheViewResolver resolver = new MustacheViewResolver();

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setPrefix(this.prefix);
		this.resolver.setSuffix(".html");
	}

	@Test
	void resolveNonExistent() {
		assertThat(this.resolver.resolveViewName("bar", null).block(Duration.ofSeconds(30))).isNull();
	}

	@Test
	void resolveExisting() {
		assertThat(this.resolver.resolveViewName("template", null).block(Duration.ofSeconds(30))).isNotNull();
	}

}
