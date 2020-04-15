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

package org.springframework.boot.autoconfigure.mongo;

import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 */
class MongoPropertiesTests {

	@Test
	void canBindCharArrayPassword() {
		// gh-1572
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.password:word").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.getPassword()).isEqualTo("word".toCharArray());
	}

	@Test
	@SuppressWarnings("deprecation")
	void allMongoClientOptionsCanBeSet() {
		MongoClientOptions.Builder builder = MongoClientOptions.builder();
		builder.alwaysUseMBeans(true);
		builder.connectionsPerHost(101);
		builder.connectTimeout(10001);
		builder.cursorFinalizerEnabled(false);
		builder.description("test");
		builder.maxWaitTime(120001);
		builder.socketKeepAlive(false);
		builder.socketTimeout(1000);
		builder.threadsAllowedToBlockForConnectionMultiplier(6);
		builder.minConnectionsPerHost(0);
		builder.maxConnectionIdleTime(60000);
		builder.maxConnectionLifeTime(60000);
		builder.heartbeatFrequency(10001);
		builder.minHeartbeatFrequency(501);
		builder.heartbeatConnectTimeout(20001);
		builder.heartbeatSocketTimeout(20001);
		builder.localThreshold(20);
		builder.requiredReplicaSetName("testReplicaSetName");
		MongoClientOptions options = builder.build();
		MongoProperties properties = new MongoProperties();
		MongoClient client = new MongoClientFactory(properties, null).createMongoClient(options);
		MongoClientOptions wrapped = client.getMongoClientOptions();
		assertThat(wrapped.isAlwaysUseMBeans()).isEqualTo(options.isAlwaysUseMBeans());
		assertThat(wrapped.getConnectionsPerHost()).isEqualTo(options.getConnectionsPerHost());
		assertThat(wrapped.getConnectTimeout()).isEqualTo(options.getConnectTimeout());
		assertThat(wrapped.isCursorFinalizerEnabled()).isEqualTo(options.isCursorFinalizerEnabled());
		assertThat(wrapped.getDescription()).isEqualTo(options.getDescription());
		assertThat(wrapped.getMaxWaitTime()).isEqualTo(options.getMaxWaitTime());
		assertThat(wrapped.getSocketTimeout()).isEqualTo(options.getSocketTimeout());
		assertThat(wrapped.isSocketKeepAlive()).isEqualTo(options.isSocketKeepAlive());
		assertThat(wrapped.getThreadsAllowedToBlockForConnectionMultiplier())
				.isEqualTo(options.getThreadsAllowedToBlockForConnectionMultiplier());
		assertThat(wrapped.getMinConnectionsPerHost()).isEqualTo(options.getMinConnectionsPerHost());
		assertThat(wrapped.getMaxConnectionIdleTime()).isEqualTo(options.getMaxConnectionIdleTime());
		assertThat(wrapped.getMaxConnectionLifeTime()).isEqualTo(options.getMaxConnectionLifeTime());
		assertThat(wrapped.getHeartbeatFrequency()).isEqualTo(options.getHeartbeatFrequency());
		assertThat(wrapped.getMinHeartbeatFrequency()).isEqualTo(options.getMinHeartbeatFrequency());
		assertThat(wrapped.getHeartbeatConnectTimeout()).isEqualTo(options.getHeartbeatConnectTimeout());
		assertThat(wrapped.getHeartbeatSocketTimeout()).isEqualTo(options.getHeartbeatSocketTimeout());
		assertThat(wrapped.getLocalThreshold()).isEqualTo(options.getLocalThreshold());
		assertThat(wrapped.getRequiredReplicaSetName()).isEqualTo(options.getRequiredReplicaSetName());
	}

	@Test
	void uriOverridesHostAndPort() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("localhost");
		properties.setPort(27017);
		properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClient client = new MongoClientFactory(properties, null).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void onlyHostAndPortSetShouldUseThat() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("localhost");
		properties.setPort(27017);
		MongoClient client = new MongoClientFactory(properties, null).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void onlyUriSetShouldUseThat() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClient client = new MongoClientFactory(properties, null).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void noCustomAddressAndNoUriUsesDefaultUri() {
		MongoProperties properties = new MongoProperties();
		MongoClient client = new MongoClientFactory(properties, null).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@SuppressWarnings("deprecation")
	private List<ServerAddress> getAllAddresses(MongoClient client) {
		// At some point we'll probably need to use reflection to find the address but for
		// now, we can use the deprecated getAllAddress method.
		return client.getAllAddress();
	}

	@Test
	void canBindAutoIndexCreation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.autoIndexCreation:true").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.isAutoIndexCreation()).isTrue();
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
