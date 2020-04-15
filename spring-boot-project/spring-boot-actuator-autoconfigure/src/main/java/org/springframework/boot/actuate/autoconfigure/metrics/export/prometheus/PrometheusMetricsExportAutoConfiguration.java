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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Prometheus.
 *
 * @since 2.0.0
 * @author Jon Schneider
 * @author David J. M. Karlsen
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnProperty(prefix = "management.metrics.export.prometheus", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(PrometheusProperties.class)
public class PrometheusMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusPropertiesConfigAdapter(prometheusProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig,
			CollectorRegistry collectorRegistry, Clock clock) {
		return new PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public CollectorRegistry collectorRegistry() {
		return new CollectorRegistry(true);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnAvailableEndpoint(endpoint = PrometheusScrapeEndpoint.class)
	public static class PrometheusScrapeEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public PrometheusScrapeEndpoint prometheusEndpoint(CollectorRegistry collectorRegistry) {
			return new PrometheusScrapeEndpoint(collectorRegistry);
		}

	}

	/**
	 * Configuration for <a href="https://github.com/prometheus/pushgateway">Prometheus
	 * Pushgateway</a>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(PushGateway.class)
	@ConditionalOnProperty(prefix = "management.metrics.export.prometheus.pushgateway", name = "enabled")
	public static class PrometheusPushGatewayConfiguration {

		private static final Log logger = LogFactory.getLog(PrometheusPushGatewayConfiguration.class);

		/**
		 * The fallback job name. We use 'spring' since there's a history of Prometheus
		 * spring integration defaulting to that name from when Prometheus integration
		 * didn't exist in Spring itself.
		 */
		private static final String FALLBACK_JOB = "spring";

		@Bean
		@ConditionalOnMissingBean
		public PrometheusPushGatewayManager prometheusPushGatewayManager(CollectorRegistry collectorRegistry,
				PrometheusProperties prometheusProperties, Environment environment) {
			PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
			Duration pushRate = properties.getPushRate();
			String job = getJob(properties, environment);
			Map<String, String> groupingKey = properties.getGroupingKey();
			ShutdownOperation shutdownOperation = properties.getShutdownOperation();
			return new PrometheusPushGatewayManager(getPushGateway(properties.getBaseUrl()), collectorRegistry,
					pushRate, job, groupingKey, shutdownOperation);
		}

		private PushGateway getPushGateway(String url) {
			try {
				return new PushGateway(new URL(url));
			}
			catch (MalformedURLException ex) {
				logger.warn(LogMessage
						.format("Invalid PushGateway base url '%s': update your configuration to a valid URL", url));
				return new PushGateway(url);
			}
		}

		private String getJob(PrometheusProperties.Pushgateway properties, Environment environment) {
			String job = properties.getJob();
			job = (job != null) ? job : environment.getProperty("spring.application.name");
			return (job != null) ? job : FALLBACK_JOB;
		}

	}

}
