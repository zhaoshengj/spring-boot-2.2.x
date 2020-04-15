/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Henri Kerola
 */
class JettyServletWebServerFactoryTests extends AbstractJettyServletWebServerFactoryTests {

	@Test
	void correctVersionOfJettyUsed() {
		String jettyVersion = ErrorHandler.class.getPackage().getImplementationVersion();
		Matcher matcher = Pattern.compile("[0-9]+.[0-9]+.([0-9]+)[\\.-].*").matcher(jettyVersion);
		assertThat(matcher.find()).isTrue();
		assertThat(Integer.valueOf(matcher.group(1))).isGreaterThan(19);
	}

	@Test
	void jettyConfigurations() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		Configuration[] configurations = new Configuration[4];
		Arrays.setAll(configurations, (i) -> mock(Configuration.class));
		factory.setConfigurations(Arrays.asList(configurations[0], configurations[1]));
		factory.addConfigurations(configurations[2], configurations[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) configurations);
		for (Configuration configuration : configurations) {
			ordered.verify(configuration).configure(any(WebAppContext.class));
		}
	}

	@Test
	void jettyCustomizations() {
		JettyServletWebServerFactory factory = getFactory();
		JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
		Arrays.setAll(configurations, (i) -> mock(JettyServerCustomizer.class));
		factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
		factory.addServerCustomizers(configurations[2], configurations[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) configurations);
		for (JettyServerCustomizer configuration : configurations) {
			ordered.verify(configuration).customize(any(Server.class));
		}
	}

	@Test
	void sessionTimeout() {
		JettyServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofSeconds(10));
		assertTimeout(factory, 10);
	}

	@Test
	void sessionTimeoutInMins() {
		JettyServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofMinutes(1));
		assertTimeout(factory, 60);
	}

	@Test
	void sslCiphersConfiguration() {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });

		JettyServletWebServerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.webServer = factory.getWebServer();
		this.webServer.start();

		JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
		ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
		SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
		assertThat(connectionFactory.getSslContextFactory().getIncludeCipherSuites()).containsExactly("ALPHA", "BRAVO",
				"CHARLIE");
		assertThat(connectionFactory.getSslContextFactory().getExcludeCipherSuites()).isEmpty();
	}

	@Test
	void stopCalledWithoutStart() {
		JettyServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.stop();
		Server server = ((JettyWebServer) this.webServer).getServer();
		assertThat(server.isStopped()).isTrue();
	}

	@Test
	void sslEnabledMultiProtocolsConfiguration() {
		JettyServletWebServerFactory factory = getFactory();
		factory.setSsl(getSslSettings("TLSv1.1", "TLSv1.2"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
		ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
		SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
		assertThat(connectionFactory.getSslContextFactory().getIncludeProtocols()).containsExactly("TLSv1.1",
				"TLSv1.2");
	}

	@Test
	void sslEnabledProtocolsConfiguration() {
		JettyServletWebServerFactory factory = getFactory();
		factory.setSsl(getSslSettings("TLSv1.1"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
		ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
		SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
		assertThat(connectionFactory.getSslContextFactory().getIncludeProtocols()).containsExactly("TLSv1.1");
	}

	private Ssl getSslSettings(String... enabledProtocols) {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		ssl.setEnabledProtocols(enabledProtocols);
		return ssl;
	}

	private void assertTimeout(JettyServletWebServerFactory factory, int expected) {
		this.webServer = factory.getWebServer();
		JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
		Handler[] handlers = jettyWebServer.getServer().getChildHandlersByClass(WebAppContext.class);
		WebAppContext webAppContext = (WebAppContext) handlers[0];
		int actual = webAppContext.getSessionHandler().getMaxInactiveInterval();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void wrappedHandlers() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		factory.setServerCustomizers(Collections.singletonList((server) -> {
			Handler handler = server.getHandler();
			HandlerWrapper wrapper = new HandlerWrapper();
			wrapper.setHandler(handler);
			HandlerCollection collection = new HandlerCollection();
			collection.addHandler(wrapper);
			server.setHandler(collection);
		}));
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
	}

	@Test
	void basicSslClasspathKeyStore() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	void useForwardHeaders() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void defaultThreadPool() {
		JettyServletWebServerFactory factory = getFactory();
		factory.setThreadPool(null);
		assertThat(factory.getThreadPool()).isNull();
		this.webServer = factory.getWebServer();
		assertThat(((JettyWebServer) this.webServer).getServer().getThreadPool()).isNotNull();
	}

	@Test
	void customThreadPool() {
		JettyServletWebServerFactory factory = getFactory();
		ThreadPool threadPool = mock(ThreadPool.class);
		factory.setThreadPool(threadPool);
		this.webServer = factory.getWebServer();
		assertThat(((JettyWebServer) this.webServer).getServer().getThreadPool()).isSameAs(threadPool);
	}

	@Test
	void startFailsWhenThreadPoolIsTooSmall() {
		JettyServletWebServerFactory factory = getFactory();
		factory.addServerCustomizers((server) -> {
			QueuedThreadPool threadPool = server.getBean(QueuedThreadPool.class);
			threadPool.setMaxThreads(2);
			threadPool.setMinThreads(2);
		});
		assertThatExceptionOfType(WebServerException.class).isThrownBy(factory.getWebServer()::start)
				.withCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void specificIPAddressNotReverseResolved() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		InetAddress localhost = InetAddress.getLocalHost();
		factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
		assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
	}

	@Test
	void specificIPAddressWithSslIsNotReverseResolved() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		InetAddress localhost = InetAddress.getLocalHost();
		factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
		assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
	}

	@Test
	void faultyListenerCausesStartFailure() throws Exception {
		JettyServletWebServerFactory factory = getFactory();
		factory.addServerCustomizers((JettyServerCustomizer) (server) -> {
			Collection<WebAppContext> contexts = server.getBeans(WebAppContext.class);
			contexts.iterator().next().addEventListener(new ServletContextListener() {

				@Override
				public void contextInitialized(ServletContextEvent event) {
					throw new RuntimeException();
				}

				@Override
				public void contextDestroyed(ServletContextEvent event) {
				}

			});
		});
		assertThatExceptionOfType(WebServerException.class).isThrownBy(() -> {
			JettyWebServer jettyWebServer = (JettyWebServer) factory.getWebServer();
			try {
				jettyWebServer.start();
			}
			finally {
				QueuedThreadPool threadPool = (QueuedThreadPool) jettyWebServer.getServer().getThreadPool();
				assertThat(threadPool.isRunning()).isFalse();
			}
		});
	}

	@Test
	void errorHandlerCanBeOverridden() {
		JettyServletWebServerFactory factory = getFactory();
		factory.addConfigurations(new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				context.setErrorHandler(new CustomErrorHandler());
			}

		});
		JettyWebServer jettyWebServer = (JettyWebServer) factory.getWebServer();
		WebAppContext context = (WebAppContext) jettyWebServer.getServer().getHandler();
		assertThat(context.getErrorHandler()).isInstanceOf(CustomErrorHandler.class);
	}

	private static class CustomErrorHandler extends ErrorPageErrorHandler {

	}

}
