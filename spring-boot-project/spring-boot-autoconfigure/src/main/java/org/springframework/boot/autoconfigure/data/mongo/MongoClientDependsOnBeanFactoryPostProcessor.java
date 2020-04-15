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

package org.springframework.boot.autoconfigure.data.mongo;

import com.mongodb.MongoClient;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;

/**
 * {@link BeanFactoryPostProcessor} to automatically set up the recommended
 * {@link BeanDefinition#setDependsOn(String[]) dependsOn} configuration for Mongo clients
 * when used embedded Mongo.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class MongoClientDependsOnBeanFactoryPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

	/**
	 * Creates a new {@code MongoClientDependsOnBeanFactoryPostProcessor} that will set up
	 * dependencies upon beans with the given names.
	 * @param dependsOn names of the beans to depend upon
	 * @deprecated since 2.1.7 in favor of
	 * {@link #MongoClientDependsOnBeanFactoryPostProcessor(Class...)}
	 */
	@Deprecated
	public MongoClientDependsOnBeanFactoryPostProcessor(String... dependsOn) {
		super(MongoClient.class, MongoClientFactoryBean.class, dependsOn);
	}

	/**
	 * Creates a new {@code MongoClientDependsOnBeanFactoryPostProcessor} that will set up
	 * dependencies upon beans with the given types.
	 * @param dependsOn types of the beans to depend upon
	 */
	public MongoClientDependsOnBeanFactoryPostProcessor(Class<?>... dependsOn) {
		super(MongoClient.class, MongoClientFactoryBean.class, dependsOn);
	}

}
