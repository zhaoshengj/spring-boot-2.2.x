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

package org.springframework.boot.autoconfigure.data.couchbase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfigurer;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.data.couchbase.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CouchbaseRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void couchbaseNotAvailable() {
		load(null);
		assertThat(this.context.getBeansOfType(CityRepository.class)).hasSize(0);
	}

	@Test
	void defaultRepository() {
		load(DefaultConfiguration.class);
		assertThat(this.context.getBeansOfType(CityRepository.class)).hasSize(1);
	}

	@Test
	void reactiveRepositories() {
		load(DefaultConfiguration.class, "spring.data.couchbase.repositories.type=reactive");
		assertThat(this.context.getBeansOfType(CityRepository.class)).hasSize(0);
	}

	@Test
	void disabledRepositories() {
		load(DefaultConfiguration.class, "spring.data.couchbase.repositories.type=none");
		assertThat(this.context.getBeansOfType(CityRepository.class)).hasSize(0);
	}

	@Test
	void noRepositoryAvailable() {
		load(NoRepositoryConfiguration.class);
		assertThat(this.context.getBeansOfType(CityRepository.class)).hasSize(0);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(context);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class, CouchbaseAutoConfiguration.class,
				CouchbaseDataAutoConfiguration.class, CouchbaseRepositoriesAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class CouchbaseNotAvailableConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@Import(CouchbaseTestConfigurer.class)
	static class DefaultConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@Import(CouchbaseTestConfigurer.class)
	static class NoRepositoryConfiguration {

	}

}
