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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Jetty-specific features common for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @since 2.0.0
 */
public class JettyWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public JettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		ServerProperties properties = this.serverProperties;
		ServerProperties.Jetty jettyProperties = properties.getJetty();
		factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(jettyProperties::getAcceptors).whenNonNull().to(factory::setAcceptors);
		propertyMapper.from(jettyProperties::getSelectors).whenNonNull().to(factory::setSelectors);
		propertyMapper.from(properties::getMaxHttpHeaderSize).whenNonNull().asInt(DataSize::toBytes)
				.when(this::isPositive).to((maxHttpHeaderSize) -> factory
						.addServerCustomizers(new MaxHttpHeaderSizeCustomizer(maxHttpHeaderSize)));
		propertyMapper.from(jettyProperties::getMaxHttpFormPostSize).asInt(DataSize::toBytes).when(this::isPositive)
				.to((maxHttpFormPostSize) -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));
		propertyMapper.from(jettyProperties::getMaxThreads).when(this::isPositive)
				.to((maxThreads) -> customizeThreadPool(factory, (threadPool) -> threadPool.setMaxThreads(maxThreads)));
		propertyMapper.from(jettyProperties::getMinThreads).when(this::isPositive)
				.to((minThreads) -> customizeThreadPool(factory, (threadPool) -> threadPool.setMinThreads(minThreads)));
		propertyMapper.from(jettyProperties::getThreadIdleTimeout).whenNonNull().asInt(Duration::toMillis).to(
				(idleTimeout) -> customizeThreadPool(factory, (threadPool) -> threadPool.setIdleTimeout(idleTimeout)));
		propertyMapper.from(properties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeIdleTimeout(factory, connectionTimeout));
		propertyMapper.from(jettyProperties::getConnectionIdleTimeout).whenNonNull()
				.to((idleTimeout) -> customizeIdleTimeout(factory, idleTimeout));
		propertyMapper.from(jettyProperties::getAccesslog).when(ServerProperties.Jetty.Accesslog::isEnabled)
				.to((accesslog) -> customizeAccessLog(factory, accesslog));
	}

	private boolean isPositive(Integer value) {
		return value > 0;
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	private void customizeIdleTimeout(ConfigurableJettyWebServerFactory factory, Duration connectionTimeout) {
		factory.addServerCustomizers((server) -> {
			for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
				if (connector instanceof AbstractConnector) {
					((AbstractConnector) connector).setIdleTimeout(connectionTimeout.toMillis());
				}
			}
		});
	}

	private void customizeMaxHttpFormPostSize(ConfigurableJettyWebServerFactory factory, int maxHttpFormPostSize) {
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				setHandlerMaxHttpFormPostSize(server.getHandlers());
			}

			private void setHandlerMaxHttpFormPostSize(Handler... handlers) {
				for (Handler handler : handlers) {
					if (handler instanceof ContextHandler) {
						((ContextHandler) handler).setMaxFormContentSize(maxHttpFormPostSize);
					}
					else if (handler instanceof HandlerWrapper) {
						setHandlerMaxHttpFormPostSize(((HandlerWrapper) handler).getHandler());
					}
					else if (handler instanceof HandlerCollection) {
						setHandlerMaxHttpFormPostSize(((HandlerCollection) handler).getHandlers());
					}
				}
			}

		});
	}

	private void customizeThreadPool(ConfigurableJettyWebServerFactory factory, Consumer<QueuedThreadPool> customizer) {
		factory.addServerCustomizers((connector) -> {
			ThreadPool threadPool = connector.getThreadPool();
			if (threadPool instanceof QueuedThreadPool) {
				customizer.accept((QueuedThreadPool) threadPool);
			}
		});
	}

	private void customizeAccessLog(ConfigurableJettyWebServerFactory factory,
			ServerProperties.Jetty.Accesslog properties) {
		factory.addServerCustomizers((server) -> {
			RequestLogWriter logWriter = new RequestLogWriter();
			String format = getLogFormat(properties);
			CustomRequestLog log = new CustomRequestLog(logWriter, format);
			if (!CollectionUtils.isEmpty(properties.getIgnorePaths())) {
				log.setIgnorePaths(properties.getIgnorePaths().toArray(new String[0]));
			}
			if (properties.getFilename() != null) {
				logWriter.setFilename(properties.getFilename());
			}
			if (properties.getFileDateFormat() != null) {
				logWriter.setFilenameDateFormat(properties.getFileDateFormat());
			}
			logWriter.setRetainDays(properties.getRetentionPeriod());
			logWriter.setAppend(properties.isAppend());
			server.setRequestLog(log);
		});
	}

	private String getLogFormat(ServerProperties.Jetty.Accesslog properties) {
		if (properties.getCustomFormat() != null) {
			return properties.getCustomFormat();
		}
		else if (ServerProperties.Jetty.Accesslog.FORMAT.EXTENDED_NCSA.equals(properties.getFormat())) {
			return CustomRequestLog.EXTENDED_NCSA_FORMAT;
		}
		return CustomRequestLog.NCSA_FORMAT;
	}

	private static class MaxHttpHeaderSizeCustomizer implements JettyServerCustomizer {

		private final int maxHttpHeaderSize;

		MaxHttpHeaderSizeCustomizer(int maxHttpHeaderSize) {
			this.maxHttpHeaderSize = maxHttpHeaderSize;
		}

		@Override
		public void customize(Server server) {
			Arrays.stream(server.getConnectors()).forEach(this::customize);
		}

		private void customize(org.eclipse.jetty.server.Connector connector) {
			connector.getConnectionFactories().forEach(this::customize);
		}

		private void customize(ConnectionFactory factory) {
			if (factory instanceof HttpConfiguration.ConnectionFactory) {
				((HttpConfiguration.ConnectionFactory) factory).getHttpConfiguration()
						.setRequestHeaderSize(this.maxHttpHeaderSize);
			}
		}

	}

}
