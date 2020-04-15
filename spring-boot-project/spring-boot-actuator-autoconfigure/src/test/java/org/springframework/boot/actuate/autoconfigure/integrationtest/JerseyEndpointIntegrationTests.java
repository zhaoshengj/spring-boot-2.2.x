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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Integration tests for the Jersey actuator endpoints.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class JerseyEndpointIntegrationTests {

	@Test
	void linksAreProvidedToAllEndpointTypes() {
		testJerseyEndpoints(new Class[] { EndpointsConfiguration.class, ResourceConfigConfiguration.class });
	}

	@Test
	void actuatorEndpointsWhenUserProvidedResourceConfigBeanNotAvailable() {
		testJerseyEndpoints(new Class[] { EndpointsConfiguration.class });
	}

	protected void testJerseyEndpoints(Class<?>[] userConfigurations) {
		FilteredClassLoader classLoader = new FilteredClassLoader(DispatcherServlet.class);
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withClassLoader(classLoader)
				.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, JerseyAutoConfiguration.class,
						EndpointAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
						WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
						BeansEndpointAutoConfiguration.class))
				.withUserConfiguration(userConfigurations)
				.withPropertyValues("management.endpoints.web.exposure.include:*", "server.port:0").run((context) -> {
					int port = context
							.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
							.getWebServer().getPort();
					WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
					client.get().uri("/actuator").exchange().expectStatus().isOk().expectBody().jsonPath("_links.beans")
							.isNotEmpty().jsonPath("_links.restcontroller").doesNotExist().jsonPath("_links.controller")
							.doesNotExist();
				});
	}

	@ControllerEndpoint(id = "controller")
	static class TestControllerEndpoint {

	}

	@RestControllerEndpoint(id = "restcontroller")
	static class TestRestControllerEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointsConfiguration {

		@Bean
		TestControllerEndpoint testControllerEndpoint() {
			return new TestControllerEndpoint();
		}

		@Bean
		TestRestControllerEndpoint testRestControllerEndpoint() {
			return new TestRestControllerEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfig testResourceConfig() {
			return new ResourceConfig();
		}

	}

}
