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

package org.springframework.boot.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerFactoryCustomizerBeanPostProcessor}.
 *
 * @author Phillip Webb
 */
class WebServerFactoryCustomizerBeanPostProcessorTests {

	private WebServerFactoryCustomizerBeanPostProcessor processor = new WebServerFactoryCustomizerBeanPostProcessor();

	@Mock
	private ListableBeanFactory beanFactory;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.processor.setBeanFactory(this.beanFactory);
	}

	@Test
	void setBeanFactoryWhenNotListableShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.processor.setBeanFactory(mock(BeanFactory.class)))
				.withMessageContaining(
						"WebServerCustomizerBeanPostProcessor can only be used with a ListableBeanFactory");
	}

	@Test
	void postProcessBeforeShouldReturnBean() {
		addMockBeans(Collections.emptyMap());
		Object bean = new Object();
		Object result = this.processor.postProcessBeforeInitialization(bean, "foo");
		assertThat(result).isSameAs(bean);
	}

	@Test
	void postProcessAfterShouldReturnBean() {
		addMockBeans(Collections.emptyMap());
		Object bean = new Object();
		Object result = this.processor.postProcessAfterInitialization(bean, "foo");
		assertThat(result).isSameAs(bean);
	}

	@Test
	void postProcessAfterShouldCallInterfaceCustomizers() {
		Map<String, Object> beans = addInterfaceBeans();
		addMockBeans(beans);
		postProcessBeforeInitialization(WebServerFactory.class);
		assertThat(wasCalled(beans, "one")).isFalse();
		assertThat(wasCalled(beans, "two")).isFalse();
		assertThat(wasCalled(beans, "all")).isTrue();
	}

	@Test
	void postProcessAfterWhenWebServerFactoryOneShouldCallInterfaceCustomizers() {
		Map<String, Object> beans = addInterfaceBeans();
		addMockBeans(beans);
		postProcessBeforeInitialization(WebServerFactoryOne.class);
		assertThat(wasCalled(beans, "one")).isTrue();
		assertThat(wasCalled(beans, "two")).isFalse();
		assertThat(wasCalled(beans, "all")).isTrue();
	}

	@Test
	void postProcessAfterWhenWebServerFactoryTwoShouldCallInterfaceCustomizers() {
		Map<String, Object> beans = addInterfaceBeans();
		addMockBeans(beans);
		postProcessBeforeInitialization(WebServerFactoryTwo.class);
		assertThat(wasCalled(beans, "one")).isFalse();
		assertThat(wasCalled(beans, "two")).isTrue();
		assertThat(wasCalled(beans, "all")).isTrue();
	}

	private Map<String, Object> addInterfaceBeans() {
		WebServerFactoryOneCustomizer oneCustomizer = new WebServerFactoryOneCustomizer();
		WebServerFactoryTwoCustomizer twoCustomizer = new WebServerFactoryTwoCustomizer();
		WebServerFactoryAllCustomizer allCustomizer = new WebServerFactoryAllCustomizer();
		Map<String, Object> beans = new LinkedHashMap<>();
		beans.put("one", oneCustomizer);
		beans.put("two", twoCustomizer);
		beans.put("all", allCustomizer);
		return beans;
	}

	@Test
	void postProcessAfterShouldCallLambdaCustomizers() {
		List<String> called = new ArrayList<>();
		addLambdaBeans(called);
		postProcessBeforeInitialization(WebServerFactory.class);
		assertThat(called).containsExactly("all");
	}

	@Test
	void postProcessAfterWhenWebServerFactoryOneShouldCallLambdaCustomizers() {
		List<String> called = new ArrayList<>();
		addLambdaBeans(called);
		postProcessBeforeInitialization(WebServerFactoryOne.class);
		assertThat(called).containsExactly("one", "all");
	}

	@Test
	void postProcessAfterWhenWebServerFactoryTwoShouldCallLambdaCustomizers() {
		List<String> called = new ArrayList<>();
		addLambdaBeans(called);
		postProcessBeforeInitialization(WebServerFactoryTwo.class);
		assertThat(called).containsExactly("two", "all");
	}

	private void addLambdaBeans(List<String> called) {
		WebServerFactoryCustomizer<WebServerFactoryOne> one = (f) -> called.add("one");
		WebServerFactoryCustomizer<WebServerFactoryTwo> two = (f) -> called.add("two");
		WebServerFactoryCustomizer<WebServerFactory> all = (f) -> called.add("all");
		Map<String, Object> beans = new LinkedHashMap<>();
		beans.put("one", one);
		beans.put("two", two);
		beans.put("all", all);
		addMockBeans(beans);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addMockBeans(Map<String, ?> beans) {
		given(this.beanFactory.getBeansOfType(WebServerFactoryCustomizer.class, false, false))
				.willReturn((Map<String, WebServerFactoryCustomizer>) beans);
	}

	private void postProcessBeforeInitialization(Class<?> type) {
		this.processor.postProcessBeforeInitialization(mock(type), "foo");
	}

	private boolean wasCalled(Map<String, ?> beans, String name) {
		return ((MockWebServerFactoryCustomizer<?>) beans.get(name)).wasCalled();
	}

	interface WebServerFactoryOne extends WebServerFactory {

	}

	interface WebServerFactoryTwo extends WebServerFactory {

	}

	static class MockWebServerFactoryCustomizer<T extends WebServerFactory> implements WebServerFactoryCustomizer<T> {

		private boolean called;

		@Override
		public void customize(T factory) {
			this.called = true;
		}

		boolean wasCalled() {
			return this.called;
		}

	}

	static class WebServerFactoryOneCustomizer extends MockWebServerFactoryCustomizer<WebServerFactoryOne> {

	}

	static class WebServerFactoryTwoCustomizer extends MockWebServerFactoryCustomizer<WebServerFactoryTwo> {

	}

	static class WebServerFactoryAllCustomizer extends MockWebServerFactoryCustomizer<WebServerFactory> {

	}

}
