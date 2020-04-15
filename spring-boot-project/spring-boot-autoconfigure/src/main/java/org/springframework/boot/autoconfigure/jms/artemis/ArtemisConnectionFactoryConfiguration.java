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

package org.springframework.boot.autoconfigure.jms.artemis;

import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.pool2.PooledObject;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryFactory;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Configuration for Artemis {@link ConnectionFactory}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ArtemisConnectionFactoryConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CachingConnectionFactory.class)
	@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		private final ArtemisProperties properties;

		private final ListableBeanFactory beanFactory;

		SimpleConnectionFactoryConfiguration(ArtemisProperties properties, ListableBeanFactory beanFactory) {
			this.properties = properties;
			this.beanFactory = beanFactory;
		}

		@Bean(name = "jmsConnectionFactory")
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true",
				matchIfMissing = true)
		CachingConnectionFactory cachingJmsConnectionFactory(JmsProperties jmsProperties) {
			JmsProperties.Cache cacheProperties = jmsProperties.getCache();
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(createConnectionFactory());
			connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
			connectionFactory.setCacheProducers(cacheProperties.isProducers());
			connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
			return connectionFactory;
		}

		@Bean(name = "jmsConnectionFactory")
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		ActiveMQConnectionFactory jmsConnectionFactory() {
			return createConnectionFactory();
		}

		private ActiveMQConnectionFactory createConnectionFactory() {
			return new ArtemisConnectionFactoryFactory(this.beanFactory, this.properties)
					.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JmsPoolConnectionFactory.class, PooledObject.class })
	@ConditionalOnProperty(prefix = "spring.artemis.pool", name = "enabled", havingValue = "true")
	static class PooledConnectionFactoryConfiguration {

		@Bean(destroyMethod = "stop")
		JmsPoolConnectionFactory jmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties) {
			ActiveMQConnectionFactory connectionFactory = new ArtemisConnectionFactoryFactory(beanFactory, properties)
					.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new JmsPoolConnectionFactoryFactory(properties.getPool())
					.createPooledConnectionFactory(connectionFactory);
		}

	}

}
