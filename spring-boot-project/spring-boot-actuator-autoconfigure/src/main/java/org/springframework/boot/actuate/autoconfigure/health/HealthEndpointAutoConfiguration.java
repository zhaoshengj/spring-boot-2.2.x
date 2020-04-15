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

package org.springframework.boot.actuate.autoconfigure.health;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@EnableConfigurationProperties
@Import({ LegacyHealthEndpointAdaptersConfiguration.class, LegacyHealthEndpointCompatibilityConfiguration.class,
		HealthEndpointConfiguration.class, ReactiveHealthEndpointConfiguration.class,
		HealthEndpointWebExtensionConfiguration.class, HealthEndpointReactiveWebExtensionConfiguration.class })
public class HealthEndpointAutoConfiguration {

	@Bean
	@SuppressWarnings("deprecation")
	HealthEndpointProperties healthEndpointProperties(HealthIndicatorProperties healthIndicatorProperties) {
		HealthEndpointProperties healthEndpointProperties = new HealthEndpointProperties();
		healthEndpointProperties.getStatus().getOrder().addAll(healthIndicatorProperties.getOrder());
		healthEndpointProperties.getStatus().getHttpMapping().putAll(healthIndicatorProperties.getHttpMapping());
		return healthEndpointProperties;
	}

}
