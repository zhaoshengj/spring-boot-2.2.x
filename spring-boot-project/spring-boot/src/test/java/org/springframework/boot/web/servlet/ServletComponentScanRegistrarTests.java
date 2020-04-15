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

package org.springframework.boot.web.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ServletComponentScanRegistrar}
 *
 * @author Andy Wilkinson
 */
class ServletComponentScanRegistrarTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void packagesConfiguredWithValue() {
		this.context = new AnnotationConfigApplicationContext(ValuePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	void packagesConfiguredWithValueAsm() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("valuePackages", new RootBeanDefinition(ValuePackages.class.getName()));
		this.context.refresh();
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	void packagesConfiguredWithBackPackages() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	void packagesConfiguredWithBasePackageClasses() {
		this.context = new AnnotationConfigApplicationContext(BasePackageClasses.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains(getClass().getPackage().getName());
	}

	@Test
	void packagesConfiguredWithBothValueAndBasePackages() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(ValueAndBasePackages.class))
				.withMessageContaining("'value'").withMessageContaining("'basePackages'")
				.withMessageContaining("com.example.foo").withMessageContaining("com.example.bar");
	}

	@Test
	void packagesFromMultipleAnnotationsAreMerged() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class, AdditionalPackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar", "com.example.baz");
	}

	@Test
	void withNoBasePackagesScanningUsesBasePackageOfAnnotatedClass() {
		this.context = new AnnotationConfigApplicationContext(NoBasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactly("org.springframework.boot.web.servlet");
	}

	@Test
	void noBasePackageAndBasePackageAreCombinedCorrectly() {
		this.context = new AnnotationConfigApplicationContext(NoBasePackages.class, BasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("org.springframework.boot.web.servlet",
				"com.example.foo", "com.example.bar");
	}

	@Test
	void basePackageAndNoBasePackageAreCombinedCorrectly() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class, NoBasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("org.springframework.boot.web.servlet",
				"com.example.foo", "com.example.bar");
	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan({ "com.example.foo", "com.example.bar" })
	static class ValuePackages {

	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan(basePackages = { "com.example.foo", "com.example.bar" })
	static class BasePackages {

	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan(basePackages = "com.example.baz")
	static class AdditionalPackages {

	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan(basePackageClasses = ServletComponentScanRegistrarTests.class)
	static class BasePackageClasses {

	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan(value = "com.example.foo", basePackages = "com.example.bar")
	static class ValueAndBasePackages {

	}

	@Configuration(proxyBeanMethods = false)
	@ServletComponentScan
	static class NoBasePackages {

	}

}
