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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;

/**
 * Redis backed session configuration.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RedisTemplate.class, RedisIndexedSessionRepository.class })
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@Conditional(ServletSessionCondition.class)
@EnableConfigurationProperties(RedisSessionProperties.class)
class RedisSessionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ConfigureRedisAction configureRedisAction(RedisSessionProperties redisSessionProperties) {
		switch (redisSessionProperties.getConfigureAction()) {
		case NOTIFY_KEYSPACE_EVENTS:
			return new ConfigureNotifyKeyspaceEventsAction();
		case NONE:
			return ConfigureRedisAction.NO_OP;
		}
		throw new IllegalStateException(
				"Unsupported redis configure action '" + redisSessionProperties.getConfigureAction() + "'.");
	}

	@Configuration
	public static class SpringBootRedisHttpSessionConfiguration extends RedisHttpSessionConfiguration {

		@Autowired
		public void customize(SessionProperties sessionProperties, RedisSessionProperties redisSessionProperties) {
			Duration timeout = sessionProperties.getTimeout();
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds((int) timeout.getSeconds());
			}
			setRedisNamespace(redisSessionProperties.getNamespace());
			setFlushMode(redisSessionProperties.getFlushMode());
			setSaveMode(redisSessionProperties.getSaveMode());
			setCleanupCron(redisSessionProperties.getCleanupCron());
		}

	}

}
