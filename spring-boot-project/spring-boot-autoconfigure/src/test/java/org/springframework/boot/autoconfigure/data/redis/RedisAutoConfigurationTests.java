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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Christoph Strobl
 * @author Eddú Meléndez
 * @author Marco Aust
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Alen Turkovic
 */
class RedisAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

	@Test
	void testDefaultRedisConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean("redisTemplate", RedisOperations.class)).isNotNull();
			assertThat(context.getBean(StringRedisTemplate.class)).isNotNull();
		});
	}

	@Test
	void testOverrideRedisConfiguration() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.database:1",
				"spring.redis.lettuce.shutdown-timeout:500").run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getDatabase()).isEqualTo(1);
					assertThat(cf.getPassword()).isNull();
					assertThat(cf.isUseSsl()).isFalse();
					assertThat(cf.getShutdownTimeout()).isEqualTo(500);
				});
	}

	@Test
	void testCustomizeRedisConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.isUseSsl()).isTrue();
		});
	}

	@Test
	void testRedisUrlConfiguration() {
		this.contextRunner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.url:redis://user:password@example:33")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isFalse();
				});
	}

	@Test
	void testOverrideUrlRedisConfiguration() {
		this.contextRunner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.password:xyz", "spring.redis.port:1000",
						"spring.redis.ssl:false", "spring.redis.url:rediss://user:password@example:33")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isTrue();
				});
	}

	@Test
	void testPasswordInUrlWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://:pass:word@example:33").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("example");
			assertThat(cf.getPort()).isEqualTo(33);
			assertThat(cf.getPassword()).isEqualTo("pass:word");
		});
	}

	@Test
	void testPasswordInUrlStartsWithColon() {
		this.contextRunner.withPropertyValues("spring.redis.url:redis://user::pass:word@example:33").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("example");
			assertThat(cf.getPort()).isEqualTo(33);
			assertThat(cf.getPassword()).isEqualTo(":pass:word");
		});
	}

	@Test
	void testRedisConfigurationWithPool() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.lettuce.pool.min-idle:1",
				"spring.redis.lettuce.pool.max-idle:4", "spring.redis.lettuce.pool.max-active:16",
				"spring.redis.lettuce.pool.max-wait:2000", "spring.redis.lettuce.pool.time-between-eviction-runs:30000",
				"spring.redis.lettuce.shutdown-timeout:1000").run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					GenericObjectPoolConfig<?> poolConfig = getPoolingClientConfiguration(cf).getPoolConfig();
					assertThat(poolConfig.getMinIdle()).isEqualTo(1);
					assertThat(poolConfig.getMaxIdle()).isEqualTo(4);
					assertThat(poolConfig.getMaxTotal()).isEqualTo(16);
					assertThat(poolConfig.getMaxWaitMillis()).isEqualTo(2000);
					assertThat(poolConfig.getTimeBetweenEvictionRunsMillis()).isEqualTo(30000);
					assertThat(cf.getShutdownTimeout()).isEqualTo(1000);
				});
	}

	@Test
	void testRedisConfigurationWithTimeout() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.timeout:100").run((context) -> {
			LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
			assertThat(cf.getHostName()).isEqualTo("foo");
			assertThat(cf.getTimeout()).isEqualTo(100);
		});
	}

	@Test
	void testRedisConfigurationWithClientName() {
		this.contextRunner.withPropertyValues("spring.redis.host:foo", "spring.redis.client-name:spring-boot")
				.run((context) -> {
					LettuceConnectionFactory cf = context.getBean(LettuceConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getClientName()).isEqualTo("spring-boot");
				});
	}

	@Test
	void testRedisConfigurationWithSentinel() {
		List<String> sentinels = Arrays.asList("127.0.0.1:26379", "127.0.0.1:26380");
		this.contextRunner
				.withPropertyValues("spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:" + StringUtils.collectionToCommaDelimitedString(sentinels))
				.run((context) -> assertThat(context.getBean(LettuceConnectionFactory.class).isRedisSentinelAware())
						.isTrue());
	}

	@Test
	void testRedisConfigurationWithSentinelAndDatabase() {
		this.contextRunner.withPropertyValues("spring.redis.database:1", "spring.redis.sentinel.master:mymaster",
				"spring.redis.sentinel.nodes:127.0.0.1:26379, 127.0.0.1:26380").run((context) -> {
					LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
					assertThat(connectionFactory.getDatabase()).isEqualTo(1);
					assertThat(connectionFactory.isRedisSentinelAware()).isTrue();
				});
	}

	@Test
	void testRedisConfigurationWithSentinelAndPassword() {
		this.contextRunner.withPropertyValues("spring.redis.password=password", "spring.redis.sentinel.master:mymaster",
				"spring.redis.sentinel.nodes:127.0.0.1:26379,  127.0.0.1:26380").run((context) -> {
					LettuceConnectionFactory connectionFactory = context.getBean(LettuceConnectionFactory.class);
					assertThat(connectionFactory.getPassword()).isEqualTo("password");
					Set<RedisNode> sentinels = connectionFactory.getSentinelConfiguration().getSentinels();
					assertThat(sentinels.stream().map(Object::toString).collect(Collectors.toSet()))
							.contains("127.0.0.1:26379", "127.0.0.1:26380");
				});
	}

	@Test
	void testRedisConfigurationWithCluster() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		this.contextRunner.withPropertyValues("spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
				"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1)).run((context) -> {
					RedisClusterConfiguration clusterConfiguration = context.getBean(LettuceConnectionFactory.class)
							.getClusterConfiguration();
					assertThat(clusterConfiguration.getClusterNodes()).hasSize(2);
					assertThat(clusterConfiguration.getClusterNodes())
							.extracting((node) -> node.getHost() + ":" + node.getPort())
							.containsExactlyInAnyOrder("127.0.0.1:27379", "127.0.0.1:27380");
				});

	}

	@Test
	void testRedisConfigurationWithClusterAndPassword() {
		List<String> clusterNodes = Arrays.asList("127.0.0.1:27379", "127.0.0.1:27380");
		this.contextRunner
				.withPropertyValues("spring.redis.password=password",
						"spring.redis.cluster.nodes[0]:" + clusterNodes.get(0),
						"spring.redis.cluster.nodes[1]:" + clusterNodes.get(1))
				.run((context) -> assertThat(context.getBean(LettuceConnectionFactory.class).getPassword())
						.isEqualTo("password")

				);
	}

	private LettucePoolingClientConfiguration getPoolingClientConfiguration(LettuceConnectionFactory factory) {
		return (LettucePoolingClientConfiguration) ReflectionTestUtils.getField(factory, "clientConfiguration");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		LettuceClientConfigurationBuilderCustomizer customizer() {
			return LettuceClientConfigurationBuilder::useSsl;
		}

	}

}
