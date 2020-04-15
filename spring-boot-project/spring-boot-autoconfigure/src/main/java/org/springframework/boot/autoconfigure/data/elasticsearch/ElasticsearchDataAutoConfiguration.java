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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Elasticsearch
 * support.
 * <p>
 * Registers an {@link ElasticsearchTemplate} if no other bean of the same type and the
 * same name {@code "elasticsearchTemplate"} is configured.
 *
 * @author Brian Clozel
 * @author Artur Konczak
 * @author Mohsin Husen
 * @see EnableElasticsearchRepositories
 * @see EnableReactiveElasticsearchRepositories
 * @since 1.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ElasticsearchTemplate.class })
@AutoConfigureAfter({ ElasticsearchAutoConfiguration.class, RestClientAutoConfiguration.class,
		ReactiveRestClientAutoConfiguration.class })
@Import({ ElasticsearchDataConfiguration.BaseConfiguration.class,
		ElasticsearchDataConfiguration.TransportClientConfiguration.class,
		ElasticsearchDataConfiguration.RestClientConfiguration.class,
		ElasticsearchDataConfiguration.ReactiveRestClientConfiguration.class })
public class ElasticsearchDataAutoConfiguration {

}
