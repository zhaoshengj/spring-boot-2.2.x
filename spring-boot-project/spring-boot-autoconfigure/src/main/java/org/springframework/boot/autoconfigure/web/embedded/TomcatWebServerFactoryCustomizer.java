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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeStacktrace;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Accesslog;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Tomcat-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chentao Qu
 * @author Andrew McGhie
 * @author Dirk Deyne
 * @author Rafiullah Hamedy
 * @since 2.0.0
 */
public class TomcatWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public TomcatWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties properties = this.serverProperties;
		ServerProperties.Tomcat tomcatProperties = properties.getTomcat();
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(tomcatProperties::getBasedir).whenNonNull().to(factory::setBaseDirectory);
		propertyMapper.from(tomcatProperties::getBackgroundProcessorDelay).whenNonNull().as(Duration::getSeconds)
				.as(Long::intValue).to(factory::setBackgroundProcessorDelay);
		customizeRemoteIpValve(factory);
		propertyMapper.from(tomcatProperties::getMaxThreads).when(this::isPositive)
				.to((maxThreads) -> customizeMaxThreads(factory, tomcatProperties.getMaxThreads()));
		propertyMapper.from(tomcatProperties::getMinSpareThreads).when(this::isPositive)
				.to((minSpareThreads) -> customizeMinThreads(factory, minSpareThreads));
		propertyMapper.from(this.serverProperties.getMaxHttpHeaderSize()).whenNonNull().asInt(DataSize::toBytes)
				.when(this::isPositive)
				.to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory, maxHttpHeaderSize));
		propertyMapper.from(tomcatProperties::getMaxSwallowSize).whenNonNull().asInt(DataSize::toBytes)
				.to((maxSwallowSize) -> customizeMaxSwallowSize(factory, maxSwallowSize));
		propertyMapper.from(tomcatProperties::getMaxHttpFormPostSize).asInt(DataSize::toBytes)
				.when((maxHttpFormPostSize) -> maxHttpFormPostSize != 0)
				.to((maxHttpFormPostSize) -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));
		propertyMapper.from(tomcatProperties::getAccesslog).when(ServerProperties.Tomcat.Accesslog::isEnabled)
				.to((enabled) -> customizeAccessLog(factory));
		propertyMapper.from(tomcatProperties::getUriEncoding).whenNonNull().to(factory::setUriEncoding);
		propertyMapper.from(properties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
		propertyMapper.from(tomcatProperties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
		propertyMapper.from(tomcatProperties::getMaxConnections).when(this::isPositive)
				.to((maxConnections) -> customizeMaxConnections(factory, maxConnections));
		propertyMapper.from(tomcatProperties::getAcceptCount).when(this::isPositive)
				.to((acceptCount) -> customizeAcceptCount(factory, acceptCount));
		propertyMapper.from(tomcatProperties::getProcessorCache)
				.to((processorCache) -> customizeProcessorCache(factory, processorCache));
		propertyMapper.from(tomcatProperties::getRelaxedPathChars).as(this::joinCharacters).whenHasText()
				.to((relaxedChars) -> customizeRelaxedPathChars(factory, relaxedChars));
		propertyMapper.from(tomcatProperties::getRelaxedQueryChars).as(this::joinCharacters).whenHasText()
				.to((relaxedChars) -> customizeRelaxedQueryChars(factory, relaxedChars));
		customizeStaticResources(factory);
		customizeErrorReportValve(properties.getError(), factory);
	}

	private boolean isPositive(int value) {
		return value > 0;
	}

	private void customizeAcceptCount(ConfigurableTomcatWebServerFactory factory, int acceptCount) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setAcceptCount(acceptCount);
			}
		});
	}

	private void customizeProcessorCache(ConfigurableTomcatWebServerFactory factory, int processorCache) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				((AbstractProtocol<?>) handler).setProcessorCache(processorCache);
			}
		});
	}

	private void customizeMaxConnections(ConfigurableTomcatWebServerFactory factory, int maxConnections) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setMaxConnections(maxConnections);
			}
		});
	}

	private void customizeConnectionTimeout(ConfigurableTomcatWebServerFactory factory, Duration connectionTimeout) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setConnectionTimeout((int) connectionTimeout.toMillis());
			}
		});
	}

	private void customizeRelaxedPathChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
		factory.addConnectorCustomizers((connector) -> connector.setAttribute("relaxedPathChars", relaxedChars));
	}

	private void customizeRelaxedQueryChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
		factory.addConnectorCustomizers((connector) -> connector.setAttribute("relaxedQueryChars", relaxedChars));
	}

	private String joinCharacters(List<Character> content) {
		return content.stream().map(String::valueOf).collect(Collectors.joining());
	}

	private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
		Tomcat tomcatProperties = this.serverProperties.getTomcat();
		String protocolHeader = tomcatProperties.getProtocolHeader();
		String remoteIpHeader = tomcatProperties.getRemoteIpHeader();
		// For back compatibility the valve is also enabled if protocol-header is set
		if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
				|| getOrDeduceUseForwardHeaders()) {
			RemoteIpValve valve = new RemoteIpValve();
			valve.setProtocolHeader(StringUtils.hasLength(protocolHeader) ? protocolHeader : "X-Forwarded-Proto");
			if (StringUtils.hasLength(remoteIpHeader)) {
				valve.setRemoteIpHeader(remoteIpHeader);
			}
			// The internal proxies default to a white list of "safe" internal IP
			// addresses
			valve.setInternalProxies(tomcatProperties.getInternalProxies());
			try {
				valve.setHostHeader(tomcatProperties.getHostHeader());
			}
			catch (NoSuchMethodError ex) {
				// Avoid failure with war deployments to Tomcat 8.5 before 8.5.44 and
				// Tomcat 9 before 9.0.23
			}
			valve.setPortHeader(tomcatProperties.getPortHeader());
			valve.setProtocolHeaderHttpsValue(tomcatProperties.getProtocolHeaderHttpsValue());
			// ... so it's safe to add this valve by default.
			factory.addEngineValves(valve);
		}
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxThreads(ConfigurableTomcatWebServerFactory factory, int maxThreads) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol protocol = (AbstractProtocol) handler;
				protocol.setMaxThreads(maxThreads);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void customizeMinThreads(ConfigurableTomcatWebServerFactory factory, int minSpareThreads) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol protocol = (AbstractProtocol) handler;
				protocol.setMinSpareThreads(minSpareThreads);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxHttpHeaderSize(ConfigurableTomcatWebServerFactory factory, int maxHttpHeaderSize) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
				protocol.setMaxHttpHeaderSize(maxHttpHeaderSize);
			}
		});
	}

	private void customizeMaxSwallowSize(ConfigurableTomcatWebServerFactory factory, int maxSwallowSize) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) handler;
				protocol.setMaxSwallowSize(maxSwallowSize);
			}
		});
	}

	private void customizeMaxHttpFormPostSize(ConfigurableTomcatWebServerFactory factory, int maxHttpFormPostSize) {
		factory.addConnectorCustomizers((connector) -> connector.setMaxPostSize(maxHttpFormPostSize));
	}

	private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
		AccessLogValve valve = new AccessLogValve();
		PropertyMapper map = PropertyMapper.get();
		Accesslog accessLogConfig = tomcatProperties.getAccesslog();
		map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
		map.from(accessLogConfig.getConditionUnless()).to(valve::setConditionUnless);
		map.from(accessLogConfig.getPattern()).to(valve::setPattern);
		map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
		map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
		map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
		map.from(accessLogConfig.getEncoding()).whenHasText().to(valve::setEncoding);
		map.from(accessLogConfig.getLocale()).whenHasText().to(valve::setLocale);
		map.from(accessLogConfig.isCheckExists()).to(valve::setCheckExists);
		map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
		map.from(accessLogConfig.isRenameOnRotate()).to(valve::setRenameOnRotate);
		map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
		map.from(accessLogConfig.getFileDateFormat()).to(valve::setFileDateFormat);
		map.from(accessLogConfig.isIpv6Canonical()).to(valve::setIpv6Canonical);
		map.from(accessLogConfig.isRequestAttributesEnabled()).to(valve::setRequestAttributesEnabled);
		map.from(accessLogConfig.isBuffered()).to(valve::setBuffered);
		factory.addEngineValves(valve);
	}

	private void customizeStaticResources(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat.Resource resource = this.serverProperties.getTomcat().getResource();
		factory.addContextCustomizers((context) -> {
			context.addLifecycleListener((event) -> {
				if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
					context.getResources().setCachingAllowed(resource.isAllowCaching());
					if (resource.getCacheTtl() != null) {
						long ttl = resource.getCacheTtl().toMillis();
						context.getResources().setCacheTtl(ttl);
					}
				}
			});
		});
	}

	private void customizeErrorReportValve(ErrorProperties error, ConfigurableTomcatWebServerFactory factory) {
		if (error.getIncludeStacktrace() == IncludeStacktrace.NEVER) {
			factory.addContextCustomizers((context) -> {
				ErrorReportValve valve = new ErrorReportValve();
				valve.setShowServerInfo(false);
				valve.setShowReport(false);
				context.getParent().getPipeline().addValve(valve);
			});
		}
	}

}
