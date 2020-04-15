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
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoClientFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
class MongoClientFactoryTests {

	private MockEnvironment environment = new MockEnvironment();

	@Test
	void portCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setPort(12345);
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	void hostCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("mongo.example.com");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	void credentialsCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getCredentials(client).get(0), "user", "secret", "test");
	}

	@Test
	void databaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getCredentials(client).get(0), "user", "secret", "foo");
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setAuthenticationDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getCredentials(client).get(0), "user", "secret", "foo");
	}

	@Test
	void uriCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		List<MongoCredential> credentialsList = getCredentials(client);
		assertThat(credentialsList).hasSize(1);
		assertMongoCredential(credentialsList.get(0), "user", "secret", "test");
	}

	@Test
	void uriIsIgnoredInEmbeddedMode() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://mongo.example.com:1234/mydb");
		this.environment.setProperty("local.mongo.port", "4000");
		MongoClient client = createMongoClient(properties, this.environment);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 4000);
	}

	private MongoClient createMongoClient(MongoProperties properties) {
		return createMongoClient(properties, null);
	}

	private MongoClient createMongoClient(MongoProperties properties, Environment environment) {
		return new MongoClientFactory(properties, environment).createMongoClient(null);
	}

	@SuppressWarnings("deprecation")
	private List<ServerAddress> getAllAddresses(MongoClient client) {
		// At some point we'll probably need to use reflection to find the address but for
		// now, we can use the deprecated getAllAddress method.
		return client.getAllAddress();
	}

	@SuppressWarnings("deprecation")
	private List<MongoCredential> getCredentials(MongoClient client) {
		// At some point we'll probably need to use reflection to find the credentials but
		// for now, we can use the deprecated getCredentialsList method.
		return client.getCredentialsList();
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	private void assertMongoCredential(MongoCredential credentials, String expectedUsername, String expectedPassword,
			String expectedSource) {
		assertThat(credentials.getUserName()).isEqualTo(expectedUsername);
		assertThat(credentials.getPassword()).isEqualTo(expectedPassword.toCharArray());
		assertThat(credentials.getSource()).isEqualTo(expectedSource);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
