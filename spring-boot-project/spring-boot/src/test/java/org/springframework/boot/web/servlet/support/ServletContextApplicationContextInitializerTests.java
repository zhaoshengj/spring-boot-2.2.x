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

package org.springframework.boot.web.servlet.support;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServletContextApplicationContextInitializer}.
 *
 * @author Andy Wilkinson
 */
class ServletContextApplicationContextInitializerTests {

	private final ServletContext servletContext = mock(ServletContext.class);

	private final ConfigurableWebApplicationContext applicationContext = mock(ConfigurableWebApplicationContext.class);

	@Test
	void servletContextIsSetOnTheApplicationContext() {
		new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
		verify(this.applicationContext).setServletContext(this.servletContext);
	}

	@Test
	void applicationContextIsNotStoredInServletContextByDefault() {
		new ServletContextApplicationContextInitializer(this.servletContext).initialize(this.applicationContext);
		verify(this.servletContext, never()).setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.applicationContext);
	}

	@Test
	void applicationContextCanBeStoredInServletContext() {
		new ServletContextApplicationContextInitializer(this.servletContext, true).initialize(this.applicationContext);
		verify(this.servletContext).setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.applicationContext);
	}

}
