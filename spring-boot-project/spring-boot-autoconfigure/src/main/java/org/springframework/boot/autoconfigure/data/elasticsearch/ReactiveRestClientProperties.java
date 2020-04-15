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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Elasticsearch Reactive REST clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@ConfigurationProperties(prefix = "spring.data.elasticsearch.client.reactive")
public class ReactiveRestClientProperties {

	/**
	 * Comma-separated list of the Elasticsearch endpoints to connect to.
	 */
	private List<String> endpoints = new ArrayList<>(Collections.singletonList("localhost:9200"));

	/**
	 * Whether the client should use SSL to connect to the endpoints.
	 */
	private boolean useSsl = false;

	/**
	 * Credentials username.
	 */
	private String username;

	/**
	 * Credentials password.
	 */
	private String password;

	/**
	 * Connection timeout.
	 */
	private Duration connectionTimeout;

	/**
	 * Read and Write Socket timeout.
	 */
	private Duration socketTimeout;

	public List<String> getEndpoints() {
		return this.endpoints;
	}

	public void setEndpoints(List<String> endpoints) {
		this.endpoints = endpoints;
	}

	public boolean isUseSsl() {
		return this.useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getSocketTimeout() {
		return this.socketTimeout;
	}

	public void setSocketTimeout(Duration socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

}
