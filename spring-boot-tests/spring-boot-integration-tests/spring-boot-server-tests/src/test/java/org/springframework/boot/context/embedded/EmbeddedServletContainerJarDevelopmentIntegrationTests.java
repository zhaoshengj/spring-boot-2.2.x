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

package org.springframework.boot.context.embedded;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support when developing
 * a jar application.
 *
 * @author Andy Wilkinson
 */
@EmbeddedServletContainerTest(packaging = "jar",
		launchers = { BootRunApplicationLauncher.class, IdeApplicationLauncher.class })
class EmbeddedServletContainerJarDevelopmentIntegrationTests {

	@TestTemplate
	void metaInfResourceFromDependencyIsAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/nested-meta-inf-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@TestTemplate
	@DisabledOnOs(OS.WINDOWS)
	void metaInfResourceFromDependencyWithNameThatContainsReservedCharactersIsAvailableViaHttp(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity(
				"/nested-reserved-%21%23%24%25%26%28%29%2A%2B%2C%3A%3D%3F%40%5B%5D-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("encoded-name");
	}

	@TestTemplate
	void metaInfResourceFromDependencyIsAvailableViaServletContext(RestTemplate rest) {
		ResponseEntity<String> entity = rest.getForEntity("/servletContext?/nested-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
