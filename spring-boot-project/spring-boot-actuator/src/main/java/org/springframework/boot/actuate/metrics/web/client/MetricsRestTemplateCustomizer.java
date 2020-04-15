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

package org.springframework.boot.actuate.metrics.web.client;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link RestTemplateCustomizer} that configures the {@link RestTemplate} to record
 * request metrics.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class MetricsRestTemplateCustomizer implements RestTemplateCustomizer {

	private final MetricsClientHttpRequestInterceptor interceptor;

	/**
	 * Creates a new {@code MetricsRestTemplateInterceptor} that will record metrics using
	 * the given {@code meterRegistry} with tags provided by the given
	 * {@code tagProvider}.
	 * @param meterRegistry the meter registry
	 * @param tagProvider the tag provider
	 * @param metricName the name of the recorded metric
	 * @deprecated since 2.2.0 in favor of
	 * {@link #MetricsRestTemplateCustomizer(MeterRegistry, RestTemplateExchangeTagsProvider, String, AutoTimer)}
	 */
	@Deprecated
	public MetricsRestTemplateCustomizer(MeterRegistry meterRegistry, RestTemplateExchangeTagsProvider tagProvider,
			String metricName) {
		this(meterRegistry, tagProvider, metricName, AutoTimer.ENABLED);
	}

	/**
	 * Creates a new {@code MetricsRestTemplateInterceptor}. When {@code autoTimeRequests}
	 * is set to {@code true}, the interceptor records metrics using the given
	 * {@code meterRegistry} with tags provided by the given {@code tagProvider} and with
	 * {@link AutoTimer auto-timed configuration}.
	 * @param meterRegistry the meter registry
	 * @param tagProvider the tag provider
	 * @param metricName the name of the recorded metric
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 2.2.0
	 */
	public MetricsRestTemplateCustomizer(MeterRegistry meterRegistry, RestTemplateExchangeTagsProvider tagProvider,
			String metricName, AutoTimer autoTimer) {
		this.interceptor = new MetricsClientHttpRequestInterceptor(meterRegistry, tagProvider, metricName, autoTimer);
	}

	@Override
	public void customize(RestTemplate restTemplate) {
		UriTemplateHandler templateHandler = restTemplate.getUriTemplateHandler();
		templateHandler = this.interceptor.createUriTemplateHandler(templateHandler);
		restTemplate.setUriTemplateHandler(templateHandler);
		List<ClientHttpRequestInterceptor> existingInterceptors = restTemplate.getInterceptors();
		if (!existingInterceptors.contains(this.interceptor)) {
			List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
			interceptors.add(this.interceptor);
			interceptors.addAll(existingInterceptors);
			restTemplate.setInterceptors(interceptors);
		}
	}

}
