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

package org.springframework.boot.docs.elasticsearch;

import javax.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Example configuration for configuring Hibernate to depend on Elasticsearch so that
 * Hibernate Search can use Elasticsearch as its index manager.
 *
 * @author Andy Wilkinson
 */
public class HibernateSearchElasticsearchExample {

	// tag::configuration[]
	/**
	 * {@link EntityManagerFactoryDependsOnPostProcessor} that ensures that
	 * {@link EntityManagerFactory} beans depend on the {@code elasticsearchClient} bean.
	 */
	@Component
	static class ElasticsearchEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		ElasticsearchEntityManagerFactoryDependsOnPostProcessor() {
			super("elasticsearchClient");
		}

	}
	// end::configuration[]

}
