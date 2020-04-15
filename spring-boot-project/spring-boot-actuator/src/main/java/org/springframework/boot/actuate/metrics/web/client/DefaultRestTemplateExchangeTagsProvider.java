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

import java.util.Arrays;

import io.micrometer.core.instrument.Tag;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RestTemplateExchangeTagsProvider}.
 *
 * @author Jon Schneider
 * @author Nishant Raut
 * @since 2.0.0
 */
public class DefaultRestTemplateExchangeTagsProvider implements RestTemplateExchangeTagsProvider {

	@Override
	public Iterable<Tag> getTags(String urlTemplate, HttpRequest request, ClientHttpResponse response) {
		Tag uriTag = (StringUtils.hasText(urlTemplate) ? RestTemplateExchangeTags.uri(urlTemplate)
				: RestTemplateExchangeTags.uri(request));
		return Arrays.asList(RestTemplateExchangeTags.method(request), uriTag,
				RestTemplateExchangeTags.status(response), RestTemplateExchangeTags.clientName(request),
				RestTemplateExchangeTags.outcome(response));
	}

}
