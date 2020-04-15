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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Artsiom Yudovin
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringApplicationTests {

	private String headlessProperty;

	private ConfigurableApplicationContext context;

	private Environment getEnvironment() {
		if (this.context != null) {
			return this.context.getEnvironment();
		}
		throw new IllegalStateException("Could not obtain Environment");
	}

	@BeforeEach
	void storeAndClearHeadlessProperty() {
		this.headlessProperty = System.getProperty("java.awt.headless");
		System.clearProperty("java.awt.headless");
	}

	@AfterEach
	void reinstateHeadlessProperty() {
		if (this.headlessProperty == null) {
			System.clearProperty("java.awt.headless");
		}
		else {
			System.setProperty("java.awt.headless", this.headlessProperty);
		}
	}

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("spring.main.banner-mode");
		System.clearProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
	}

	@Test
	void sourcesMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SpringApplication((Class<?>[]) null).run())
				.withMessageContaining("PrimarySources must not be null");
	}

	@Test
	void sourcesMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SpringApplication().run())
				.withMessageContaining("Sources must not be empty");
	}

	@Test
	void sourcesMustBeAccessible() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SpringApplication(InaccessibleConfiguration.class).run())
				.withMessageContaining("No visible constructors");
	}

	@Test
	void customBanner(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.banner.location=classpath:test-banner.txt");
		assertThat(output).startsWith("Running a Test!");
	}

	@Test
	void customBannerWithProperties(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.banner.location=classpath:test-banner-with-placeholder.txt",
				"--test.property=123456");
		assertThat(output).containsPattern("Running a Test!\\s+123456");
	}

	@Test
	void imageBannerAndTextBanner(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		MockResourceLoader resourceLoader = new MockResourceLoader();
		resourceLoader.addResource("banner.gif", "black-and-white.gif");
		resourceLoader.addResource("banner.txt", "foobar.txt");
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setResourceLoader(resourceLoader);
		application.run();
		assertThat(output).contains("@@@@").contains("Foo Bar");
	}

	@Test
	void imageBannerLoads(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		MockResourceLoader resourceLoader = new MockResourceLoader();
		resourceLoader.addResource("banner.gif", "black-and-white.gif");
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setResourceLoader(resourceLoader);
		application.run();
		assertThat(output).contains("@@@@@@");
	}

	@Test
	void logsNoActiveProfiles(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(output).contains("No active profile set, falling back to default profiles: default");
	}

	@Test
	void logsActiveProfiles(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=myprofiles");
		assertThat(output).contains("The following profiles are active: myprofile");
	}

	@Test
	void enableBannerInLogViaProperty(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=log");
		verify(application, atLeastOnce()).setBannerMode(Banner.Mode.LOG);
		assertThat(output).contains("o.s.b.SpringApplication");
	}

	@Test
	void setIgnoreBeanInfoPropertyByDefault(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		String property = System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
		assertThat(property).isEqualTo("true");
	}

	@Test
	void disableIgnoreBeanInfoProperty() {
		System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, "false");
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		String property = System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME);
		assertThat(property).isEqualTo("false");
	}

	@Test
	void triggersConfigFileApplicationListenerBeforeBinding() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=bindtoapplication");
		assertThat(application).hasFieldOrPropertyWithValue("bannerMode", Banner.Mode.OFF);
	}

	@Test
	void bindsSystemPropertyToSpringApplication() {
		System.setProperty("spring.main.banner-mode", "off");
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application).hasFieldOrPropertyWithValue("bannerMode", Banner.Mode.OFF);
	}

	@Test
	void bindsYamlStyleBannerModeToSpringApplication() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(Collections.singletonMap("spring.main.banner-mode", false));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application).hasFieldOrPropertyWithValue("bannerMode", Banner.Mode.OFF);
	}

	@Test
	void bindsBooleanAsStringBannerModeToSpringApplication() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=false");
		assertThat(application).hasFieldOrPropertyWithValue("bannerMode", Banner.Mode.OFF);
	}

	@Test
	void customId() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.application.name=foo");
		assertThat(this.context.getId()).startsWith("foo");
	}

	@Test
	void specificApplicationContextClass() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(StaticApplicationContext.class);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
	}

	@Test
	void specificWebApplicationContextClassDetectWebApplicationType() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(AnnotationConfigServletWebApplicationContext.class);
		assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.SERVLET);
	}

	@Test
	void specificReactiveApplicationContextClassDetectReactiveApplicationType() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(AnnotationConfigReactiveWebApplicationContext.class);
		assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.REACTIVE);
	}

	@Test
	void nonWebNorReactiveApplicationContextClassDetectNoneApplicationType() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(StaticApplicationContext.class);
		assertThat(application.getWebApplicationType()).isEqualTo(WebApplicationType.NONE);
	}

	@Test
	void specificApplicationContextInitializer() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<>();
		application.setInitializers(Collections
				.singletonList((ApplicationContextInitializer<ConfigurableApplicationContext>) reference::set));
		this.context = application.run("--foo=bar");
		assertThat(this.context).isSameAs(reference.get());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void applicationRunningEventListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		final AtomicReference<SpringApplication> reference = new AtomicReference<>();
		class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

			@Override
			public void onApplicationEvent(ApplicationReadyEvent event) {
				reference.set(event.getSpringApplication());
			}

		}
		application.addListeners(new ApplicationReadyEventListener());
		this.context = application.run("--foo=bar");
		assertThat(application).isSameAs(reference.get());
	}

	@Test
	void contextRefreshedEventListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<>();
		class InitializerListener implements ApplicationListener<ContextRefreshedEvent> {

			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				reference.set(event.getApplicationContext());
			}

		}
		application.setListeners(Collections.singletonList(new InitializerListener()));
		this.context = application.run("--foo=bar");
		assertThat(this.context).isSameAs(reference.get());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings("unchecked")
	void eventsArePublishedInExpectedOrder() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		this.context = application.run();
		InOrder inOrder = Mockito.inOrder(listener);
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationStartingEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationEnvironmentPreparedEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationContextInitializedEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationPreparedEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ContextRefreshedEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationStartedEvent.class));
		inOrder.verify(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void defaultApplicationContext() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test
	void defaultApplicationContextForWeb() {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigServletWebServerApplicationContext.class);
	}

	@Test
	void defaultApplicationContextForReactiveWeb() {
		SpringApplication application = new SpringApplication(ExampleReactiveWebConfig.class);
		application.setWebApplicationType(WebApplicationType.REACTIVE);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigReactiveWebServerApplicationContext.class);
	}

	@Test
	void environmentForWeb() {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isInstanceOf(StandardServletEnvironment.class);
	}

	@Test
	void environmentForReactiveWeb() {
		SpringApplication application = new SpringApplication(ExampleReactiveWebConfig.class);
		application.setWebApplicationType(WebApplicationType.REACTIVE);
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
	}

	@Test
	void customEnvironment() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		verify(application.getLoader()).setEnvironment(environment);
	}

	@Test
	void customResourceLoader() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		application.setResourceLoader(resourceLoader);
		this.context = application.run();
		verify(application.getLoader()).setResourceLoader(resourceLoader);
	}

	@Test
	void customResourceLoaderFromConstructor() {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		TestSpringApplication application = new TestSpringApplication(resourceLoader, ExampleWebConfig.class);
		this.context = application.run();
		verify(application.getLoader()).setResourceLoader(resourceLoader);
	}

	@Test
	void customBeanNameGenerator() {
		TestSpringApplication application = new TestSpringApplication(ExampleWebConfig.class);
		BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
		application.setBeanNameGenerator(beanNameGenerator);
		this.context = application.run();
		verify(application.getLoader()).setBeanNameGenerator(beanNameGenerator);
		Object actualGenerator = this.context.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
		assertThat(actualGenerator).isSameAs(beanNameGenerator);
	}

	@Test
	void customBeanNameGeneratorWithNonWebApplication() {
		TestSpringApplication application = new TestSpringApplication(ExampleWebConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
		application.setBeanNameGenerator(beanNameGenerator);
		this.context = application.run();
		verify(application.getLoader()).setBeanNameGenerator(beanNameGenerator);
		Object actualGenerator = this.context.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
		assertThat(actualGenerator).isSameAs(beanNameGenerator);
	}

	@Test
	void commandLinePropertySource() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).has(matchingPropertySource(CommandLinePropertySource.class, "commandLineArgs"));
	}

	@Test
	void commandLinePropertySourceEnhancesEnvironment() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
				.addFirst(new MapPropertySource("commandLineArgs", Collections.singletonMap("foo", "original")));
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar", "--bar=foo");
		assertThat(environment).has(matchingPropertySource(CompositePropertySource.class, "commandLineArgs"));
		assertThat(environment.getProperty("bar")).isEqualTo("foo");
		// New command line properties take precedence
		assertThat(environment.getProperty("foo")).isEqualTo("bar");
		CompositePropertySource composite = (CompositePropertySource) environment.getPropertySources()
				.get("commandLineArgs");
		assertThat(composite.getPropertySources()).hasSize(2);
		assertThat(composite.getPropertySources()).first().matches(
				(source) -> source.getName().equals("springApplicationCommandLineArgs"),
				"is named springApplicationCommandLineArgs");
		assertThat(composite.getPropertySources()).element(1)
				.matches((source) -> source.getName().equals("commandLineArgs"), "is named commandLineArgs");
	}

	@Test
	void propertiesFileEnhancesEnvironment() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	void addProfiles() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.acceptsProfiles(Profiles.of("foo"))).isTrue();
	}

	@Test
	void addProfilesOrder() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--spring.profiles.active=bar,spam");
		// Command line should always come last
		assertThat(environment.getActiveProfiles()).containsExactly("foo", "bar", "spam");
	}

	@Test
	void addProfilesOrderWithProperties() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("other");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		// Active profile should win over default
		assertThat(environment.getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	void emptyCommandLinePropertySourceNotAdded() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	void disableCommandLinePropertySource() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAddCommandLineProperties(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).doesNotHave(matchingPropertySource(PropertySource.class, "commandLineArgs"));
	}

	@Test
	void contextUsesApplicationConversionService() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getBeanFactory().getConversionService())
				.isInstanceOf(ApplicationConversionService.class);
		assertThat(this.context.getEnvironment().getConversionService())
				.isInstanceOf(ApplicationConversionService.class);
	}

	@Test
	void contextWhenHasAddConversionServiceFalseUsesRegularConversionService() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAddConversionService(false);
		this.context = application.run();
		assertThat(this.context.getBeanFactory().getConversionService()).isNull();
		assertThat(this.context.getEnvironment().getConversionService())
				.isNotInstanceOf(ApplicationConversionService.class);
	}

	@Test
	void runCommandLineRunnersAndApplicationRunners() {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("arg");
		assertThat(this.context).has(runTestRunnerBean("runnerA"));
		assertThat(this.context).has(runTestRunnerBean("runnerB"));
		assertThat(this.context).has(runTestRunnerBean("runnerC"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void runnersAreCalledAfterStartedIsLoggedAndBeforeApplicationReadyEventIsPublished(CapturedOutput output)
			throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ApplicationRunner applicationRunner = mock(ApplicationRunner.class);
		CommandLineRunner commandLineRunner = mock(CommandLineRunner.class);
		application.addInitializers((context) -> {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			beanFactory.registerSingleton("commandLineRunner", (CommandLineRunner) (args) -> {
				assertThat(output).contains("Started");
				commandLineRunner.run(args);
			});
			beanFactory.registerSingleton("applicationRunner", (ApplicationRunner) (args) -> {
				assertThat(output).contains("Started");
				applicationRunner.run(args);
			});
		});
		application.setWebApplicationType(WebApplicationType.NONE);
		ApplicationListener<ApplicationReadyEvent> eventListener = mock(ApplicationListener.class);
		application.addListeners(eventListener);
		this.context = application.run();
		InOrder applicationRunnerOrder = Mockito.inOrder(eventListener, applicationRunner);
		applicationRunnerOrder.verify(applicationRunner).run(ArgumentMatchers.any(ApplicationArguments.class));
		applicationRunnerOrder.verify(eventListener)
				.onApplicationEvent(ArgumentMatchers.any(ApplicationReadyEvent.class));
		InOrder commandLineRunnerOrder = Mockito.inOrder(eventListener, commandLineRunner);
		commandLineRunnerOrder.verify(commandLineRunner).run();
		commandLineRunnerOrder.verify(eventListener)
				.onApplicationEvent(ArgumentMatchers.any(ApplicationReadyEvent.class));
	}

	@Test
	void applicationRunnerFailureCausesApplicationFailedEventToBePublished() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		ApplicationRunner runner = mock(ApplicationRunner.class);
		Exception failure = new Exception();
		willThrow(failure).given(runner).run(isA(ApplicationArguments.class));
		application.addInitializers((context) -> context.getBeanFactory().registerSingleton("runner", runner));
		assertThatIllegalStateException().isThrownBy(application::run).withCause(failure);
		verify(listener).onApplicationEvent(isA(ApplicationStartedEvent.class));
		verify(listener).onApplicationEvent(isA(ApplicationFailedEvent.class));
		verify(listener, never()).onApplicationEvent(isA(ApplicationReadyEvent.class));
	}

	@Test
	void commandLineRunnerFailureCausesApplicationFailedEventToBePublished() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		CommandLineRunner runner = mock(CommandLineRunner.class);
		Exception failure = new Exception();
		willThrow(failure).given(runner).run();
		application.addInitializers((context) -> context.getBeanFactory().registerSingleton("runner", runner));
		assertThatIllegalStateException().isThrownBy(application::run).withCause(failure);
		verify(listener).onApplicationEvent(isA(ApplicationStartedEvent.class));
		verify(listener).onApplicationEvent(isA(ApplicationFailedEvent.class));
		verify(listener, never()).onApplicationEvent(isA(ApplicationReadyEvent.class));
	}

	@Test
	void failureInReadyEventListenerDoesNotCausePublicationOfFailedEvent() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		RuntimeException failure = new RuntimeException();
		willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run).isEqualTo(failure);
		verify(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		verify(listener, never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
	}

	@Test
	void failureInReadyEventListenerCloseApplicationContext(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ExitCodeListener exitCodeListener = new ExitCodeListener();
		application.addListeners(exitCodeListener);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		ExitStatusException failure = new ExitStatusException();
		willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
		verify(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		verify(listener, never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
		assertThat(exitCodeListener.getExitCode()).isEqualTo(11);
		assertThat(output).contains("Application run failed");
	}

	@Test
	void loadSources() {
		Class<?>[] sources = { ExampleConfig.class, TestCommandLineRunner.class };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.getSources().add("a");
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setUseMockLoader(true);
		this.context = application.run();
		Set<Object> allSources = application.getAllSources();
		assertThat(allSources).contains(ExampleConfig.class, TestCommandLineRunner.class, "a");
	}

	@Test
	void wildcardSources() {
		TestSpringApplication application = new TestSpringApplication();
		application.getSources().add("classpath:org/springframework/boot/sample-${sample.app.test.prop}.xml");
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
	}

	@Test
	void run() {
		this.context = SpringApplication.run(ExampleWebConfig.class);
		assertThat(this.context).isNotNull();
	}

	@Test
	void runComponents() {
		this.context = SpringApplication.run(new Class<?>[] { ExampleWebConfig.class, Object.class }, new String[0]);
		assertThat(this.context).isNotNull();
	}

	@Test
	void exit() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context)).isEqualTo(0);
	}

	@Test
	void exitWithExplicitCode() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context, (ExitCodeGenerator) () -> 2)).isEqualTo(2);
		assertThat(listener.getExitCode()).isEqualTo(2);
	}

	@Test
	void exitWithExplicitCodeFromException() {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(ExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatIllegalStateException().isThrownBy(application::run);
		verify(handler).registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	void exitWithExplicitCodeFromMappedException() {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(MappedExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatIllegalStateException().isThrownBy(application::run);
		verify(handler).registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	void exceptionFromRefreshIsHandledGracefully(CapturedOutput output) {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(RefreshFailureConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
		ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
		verify(handler).registerLoggedException(exceptionCaptor.capture());
		assertThat(exceptionCaptor.getValue()).hasCauseInstanceOf(RefreshFailureException.class);
		assertThat(output).doesNotContain("NullPointerException");
	}

	@Test
	void defaultCommandLineArgs() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(
				StringUtils.splitArrayElementsIntoProperties(new String[] { "baz=", "bar=spam" }, "="));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--bar=foo", "bucket", "crap");
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
		assertThat(getEnvironment().getProperty("bar")).isEqualTo("foo");
		assertThat(getEnvironment().getProperty("baz")).isEqualTo("");
	}

	@Test
	void commandLineArgsApplyToSpringApplication() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=OFF");
		assertThat(application.getBannerMode()).isEqualTo(Banner.Mode.OFF);
	}

	@Test
	void registerShutdownHook() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		this.context = application.run();
		SpyApplicationContext applicationContext = (SpyApplicationContext) this.context;
		verify(applicationContext.getApplicationContext()).registerShutdownHook();
	}

	@Test
	void registerListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class, ListenerConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		Set<ApplicationEvent> events = new LinkedHashSet<>();
		application.addListeners((ApplicationListener<ApplicationEvent>) events::add);
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyTestListenerEvents();
	}

	@Test
	void registerListenerWithCustomMulticaster() {
		SpringApplication application = new SpringApplication(ExampleConfig.class, ListenerConfig.class,
				Multicaster.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		Set<ApplicationEvent> events = new LinkedHashSet<>();
		application.addListeners((ApplicationListener<ApplicationEvent>) events::add);
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyTestListenerEvents();
	}

	@SuppressWarnings("unchecked")
	private void verifyTestListenerEvents() {
		ApplicationListener<ApplicationEvent> listener = this.context.getBean("testApplicationListener",
				ApplicationListener.class);
		verifyListenerEvents(listener, ContextRefreshedEvent.class, ApplicationStartedEvent.class,
				ApplicationReadyEvent.class);
	}

	@SuppressWarnings("unchecked")
	private void verifyListenerEvents(ApplicationListener<ApplicationEvent> listener,
			Class<? extends ApplicationEvent>... eventTypes) {
		for (Class<? extends ApplicationEvent> eventType : eventTypes) {
			verify(listener).onApplicationEvent(isA(eventType));
		}
		verifyNoMoreInteractions(listener);
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.addListeners(listener);
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(application::run);
		verifyListenerEvents(listener, ApplicationStartingEvent.class, ApplicationEnvironmentPreparedEvent.class,
				ApplicationContextInitializedEvent.class, ApplicationPreparedEvent.class, ApplicationFailedEvent.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addListeners(listener);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		verifyListenerEvents(listener, ApplicationStartingEvent.class, ApplicationEnvironmentPreparedEvent.class,
				ApplicationContextInitializedEvent.class, ApplicationPreparedEvent.class, ApplicationFailedEvent.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromContextIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		final ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.addInitializers((applicationContext) -> applicationContext.addApplicationListener(listener));
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(application::run);
		verifyListenerEvents(listener, ApplicationFailedEvent.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromContextIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addInitializers((applicationContext) -> applicationContext.addApplicationListener(listener));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		verifyListenerEvents(listener, ApplicationFailedEvent.class);
	}

	@Test
	void registerShutdownHookOff() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		application.setRegisterShutdownHook(false);
		this.context = application.run();
		SpyApplicationContext applicationContext = (SpyApplicationContext) this.context;
		verify(applicationContext.getApplicationContext(), never()).registerShutdownHook();
	}

	@Test
	void headless() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("true");
	}

	@Test
	void headlessFalse() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setHeadless(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	void headlessSystemPropertyTakesPrecedence() {
		System.setProperty("java.awt.headless", "false");
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	void getApplicationArgumentsBean() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--debug", "spring", "boot");
		ApplicationArguments args = this.context.getBean(ApplicationArguments.class);
		assertThat(args.getNonOptionArgs()).containsExactly("spring", "boot");
		assertThat(args.containsOption("debug")).isTrue();
	}

	@Test
	void webApplicationSwitchedOffInListener() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) (event) -> {
			assertThat(event.getEnvironment()).isInstanceOf(StandardServletEnvironment.class);
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(event.getEnvironment(), "foo=bar");
			event.getSpringApplication().setWebApplicationType(WebApplicationType.NONE);
		});
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isNotInstanceOf(StandardServletEnvironment.class);
		assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		Iterator<PropertySource<?>> iterator = this.context.getEnvironment().getPropertySources().iterator();
		assertThat(iterator.next().getName()).isEqualTo("configurationProperties");
		assertThat(iterator.next().getName())
				.isEqualTo(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
	}

	@Test
	void nonWebApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleConfig.class)
				.run("--spring.main.web-application-type=none");
		assertThat(context).isNotInstanceOfAny(WebApplicationContext.class, ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isNotInstanceOfAny(ConfigurableWebEnvironment.class);
	}

	@Test
	void webApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleWebConfig.class)
				.run("--spring.main.web-application-type=servlet");
		assertThat(context).isInstanceOf(WebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardServletEnvironment.class);
	}

	@Test
	void reactiveApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleReactiveWebConfig.class)
				.run("--spring.main.web-application-type=reactive");
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
	}

	@Test
	void environmentIsConvertedIfTypeDoesNotMatch() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleReactiveWebConfig.class)
				.run("--spring.profiles.active=withwebapplicationtype");
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
	}

	@Test
	void failureResultsInSingleStackTrace(CapturedOutput output) throws Exception {
		ThreadGroup group = new ThreadGroup("main");
		Thread thread = new Thread(group, "main") {

			@Override
			public void run() {
				SpringApplication application = new SpringApplication(FailingConfig.class);
				application.setWebApplicationType(WebApplicationType.NONE);
				application.run();
			}

		};
		thread.start();
		thread.join(6000);
		assertThat(output).containsOnlyOnce("Caused by: java.lang.RuntimeException: ExpectedError");
	}

	@Test
	void beanDefinitionOverridingIsDisabledByDefault() {
		assertThatExceptionOfType(BeanDefinitionOverrideException.class)
				.isThrownBy(() -> new SpringApplication(ExampleConfig.class, OverrideConfig.class).run());
	}

	@Test
	void beanDefinitionOverridingCanBeEnabled() {
		assertThat(new SpringApplication(ExampleConfig.class, OverrideConfig.class)
				.run("--spring.main.allow-bean-definition-overriding=true", "--spring.main.web-application-type=none")
				.getBean("someBean")).isEqualTo("override");
	}

	@Test
	void relaxedBindingShouldWorkBeforeEnvironmentIsPrepared() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.additionalLocation=classpath:custom-config/");
		assertThat(this.context.getEnvironment().getProperty("hello")).isEqualTo("world");
	}

	@Test
	void lazyInitializationIsDisabledByDefault() {
		assertThat(new SpringApplication(LazyInitializationConfig.class).run("--spring.main.web-application-type=none")
				.getBean(AtomicInteger.class)).hasValue(1);
	}

	@Test
	void lazyInitializationCanBeEnabled() {
		assertThat(new SpringApplication(LazyInitializationConfig.class)
				.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
				.getBean(AtomicInteger.class)).hasValue(0);
	}

	@Test
	void lazyInitializationIgnoresBeansThatAreExplicitlyNotLazy() {
		assertThat(new SpringApplication(NotLazyInitializationConfig.class)
				.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
				.getBean(AtomicInteger.class)).hasValue(1);
	}

	@Test
	void lazyInitializationIgnoresLazyInitializationExcludeFilteredBeans() {
		assertThat(new SpringApplication(LazyInitializationExcludeFilterConfig.class)
				.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
				.getBean(AtomicInteger.class)).hasValue(1);
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(final Class<?> propertySourceClass,
			final String name) {

		return new Condition<ConfigurableEnvironment>("has property source") {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				for (PropertySource<?> source : value.getPropertySources()) {
					if (propertySourceClass.isInstance(source) && (name == null || name.equals(source.getName()))) {
						return true;
					}
				}
				return false;
			}

		};
	}

	private Condition<ConfigurableApplicationContext> runTestRunnerBean(final String name) {
		return new Condition<ConfigurableApplicationContext>("run testrunner bean") {

			@Override
			public boolean matches(ConfigurableApplicationContext value) {
				return value.getBean(name, AbstractTestRunner.class).hasRun();
			}

		};
	}

	@Configuration
	static class InaccessibleConfiguration {

		private InaccessibleConfiguration() {
		}

	}

	static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());

		@Override
		public void registerShutdownHook() {
			this.applicationContext.registerShutdownHook();
		}

		ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void close() {
			this.applicationContext.close();
		}

	}

	static class TestSpringApplication extends SpringApplication {

		private BeanDefinitionLoader loader;

		private boolean useMockLoader;

		private Banner.Mode bannerMode;

		TestSpringApplication(Class<?>... primarySources) {
			super(primarySources);
		}

		TestSpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
			super(resourceLoader, primarySources);
		}

		void setUseMockLoader(boolean useMockLoader) {
			this.useMockLoader = useMockLoader;
		}

		@Override
		protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
			if (this.useMockLoader) {
				this.loader = mock(BeanDefinitionLoader.class);
			}
			else {
				this.loader = spy(super.createBeanDefinitionLoader(registry, sources));
			}
			return this.loader;
		}

		BeanDefinitionLoader getLoader() {
			return this.loader;
		}

		@Override
		public void setBannerMode(Banner.Mode bannerMode) {
			super.setBannerMode(bannerMode);
			this.bannerMode = bannerMode;
		}

		Banner.Mode getBannerMode() {
			return this.bannerMode;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConfig {

		@Bean
		String someBean() {
			return "test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OverrideConfig {

		@Bean
		String someBean() {
			return "override";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BrokenPostConstructConfig {

		@Bean
		Thing thing() {
			return new Thing();
		}

		static class Thing {

			@PostConstruct
			void boom() {
				throw new IllegalStateException();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ListenerConfig {

		@Bean
		ApplicationListener<?> testApplicationListener() {
			return mock(ApplicationListener.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Multicaster {

		@Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
		ApplicationEventMulticaster applicationEventMulticaster() {
			return spy(new SimpleApplicationEventMulticaster());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleWebConfig {

		@Bean
		TomcatServletWebServerFactory webServer() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleReactiveWebConfig {

		@Bean
		NettyReactiveWebServerFactory webServerFactory() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> Mono.empty();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FailingConfig {

		@Bean
		Object fail() {
			throw new RuntimeException("ExpectedError");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CommandLineRunConfig {

		@Bean
		TestCommandLineRunner runnerC() {
			return new TestCommandLineRunner(Ordered.LOWEST_PRECEDENCE, "runnerB", "runnerA");
		}

		@Bean
		TestApplicationRunner runnerB() {
			return new TestApplicationRunner(Ordered.LOWEST_PRECEDENCE - 1, "runnerA");
		}

		@Bean
		TestCommandLineRunner runnerA() {
			return new TestCommandLineRunner(Ordered.HIGHEST_PRECEDENCE);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExitCodeCommandLineRunConfig {

		@Bean
		CommandLineRunner runner() {
			return (args) -> {
				throw new IllegalStateException(new ExitStatusException());
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MappedExitCodeCommandLineRunConfig {

		@Bean
		CommandLineRunner runner() {
			return (args) -> {
				throw new IllegalStateException();
			};
		}

		@Bean
		ExitCodeExceptionMapper exceptionMapper() {
			return (exception) -> {
				if (exception instanceof IllegalStateException) {
					return 11;
				}
				return 0;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RefreshFailureConfig {

		@PostConstruct
		void fail() {
			throw new RefreshFailureException();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LazyInitializationConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger(0);
		}

		@Bean
		LazyBean lazyBean(AtomicInteger counter) {
			return new LazyBean(counter);
		}

		static class LazyBean {

			LazyBean(AtomicInteger counter) {
				counter.incrementAndGet();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotLazyInitializationConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger(0);
		}

		@Bean
		@Lazy(false)
		NotLazyBean NotLazyBean(AtomicInteger counter) {
			return new NotLazyBean(counter);
		}

		static class NotLazyBean {

			NotLazyBean(AtomicInteger counter) {
				counter.getAndIncrement();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LazyInitializationExcludeFilterConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger(0);
		}

		@Bean
		NotLazyBean notLazyBean(AtomicInteger counter) {
			return new NotLazyBean(counter);
		}

		@Bean
		static LazyInitializationExcludeFilter lazyInitializationExcludeFilter() {
			return LazyInitializationExcludeFilter.forBeanTypes(NotLazyBean.class);
		}

	}

	static class NotLazyBean {

		NotLazyBean(AtomicInteger counter) {
			counter.getAndIncrement();
		}

	}

	static class ExitStatusException extends RuntimeException implements ExitCodeGenerator {

		@Override
		public int getExitCode() {
			return 11;
		}

	}

	static class RefreshFailureException extends RuntimeException {

	}

	abstract static class AbstractTestRunner implements ApplicationContextAware, Ordered {

		private final String[] expectedBefore;

		private ApplicationContext applicationContext;

		private final int order;

		private boolean run;

		AbstractTestRunner(int order, String... expectedBefore) {
			this.expectedBefore = expectedBefore;
			this.order = order;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		void markAsRan() {
			this.run = true;
			for (String name : this.expectedBefore) {
				AbstractTestRunner bean = this.applicationContext.getBean(name, AbstractTestRunner.class);
				assertThat(bean.hasRun()).isTrue();
			}
		}

		boolean hasRun() {
			return this.run;
		}

	}

	static class TestCommandLineRunner extends AbstractTestRunner implements CommandLineRunner {

		TestCommandLineRunner(int order, String... expectedBefore) {
			super(order, expectedBefore);
		}

		@Override
		public void run(String... args) {
			markAsRan();
		}

	}

	static class TestApplicationRunner extends AbstractTestRunner implements ApplicationRunner {

		TestApplicationRunner(int order, String... expectedBefore) {
			super(order, expectedBefore);
		}

		@Override
		public void run(ApplicationArguments args) {
			markAsRan();
		}

	}

	static class ExitCodeListener implements ApplicationListener<ExitCodeEvent> {

		private Integer exitCode;

		@Override
		public void onApplicationEvent(ExitCodeEvent event) {
			this.exitCode = event.getExitCode();
		}

		Integer getExitCode() {
			return this.exitCode;
		}

	}

	static class MockResourceLoader implements ResourceLoader {

		private final Map<String, Resource> resources = new HashMap<>();

		void addResource(String source, String path) {
			this.resources.put(source, new ClassPathResource(path, getClass()));
		}

		@Override
		public Resource getResource(String path) {
			Resource resource = this.resources.get(path);
			return (resource != null) ? resource : new ClassPathResource("doesnotexist");
		}

		@Override
		public ClassLoader getClassLoader() {
			return getClass().getClassLoader();
		}

	}

}
