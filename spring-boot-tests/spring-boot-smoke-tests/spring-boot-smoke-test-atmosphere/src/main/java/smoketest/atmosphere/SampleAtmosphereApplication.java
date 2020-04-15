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

package smoketest.atmosphere;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.ContainerInitializer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootConfiguration
@EnableAutoConfiguration
public class SampleAtmosphereApplication {

	@Bean
	public EmbeddedAtmosphereInitializer atmosphereInitializer() {
		return new EmbeddedAtmosphereInitializer();
	}

	@Bean
	public ServletRegistrationBean<AtmosphereServlet> atmosphereServlet() {
		// Dispatcher servlet is mapped to '/home' to allow the AtmosphereServlet
		// to be mapped to '/chat'
		AtmosphereServlet atmosphereServlet = new AtmosphereServlet();
		atmosphereServlet.framework().setHandlersPath("/");
		ServletRegistrationBean<AtmosphereServlet> registration = new ServletRegistrationBean<>(atmosphereServlet,
				"/chat/*");
		registration.addInitParameter("org.atmosphere.cpr.packages", "smoketest");
		registration.addInitParameter(
				"org.atmosphere.interceptor.HeartbeatInterceptor.clientHeartbeatFrequencyInSeconds", "10");
		registration.setLoadOnStartup(0);
		// Need to occur before the EmbeddedAtmosphereInitializer
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleAtmosphereApplication.class, args);
	}

	@Configuration(proxyBeanMethods = false)
	static class MvcConfiguration implements WebMvcConfigurer {

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/").setViewName("forward:/home/home.html");
		}

	}

	private static class EmbeddedAtmosphereInitializer extends ContainerInitializer
			implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			onStartup(Collections.emptySet(), servletContext);
		}

	}

}
