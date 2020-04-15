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

package org.springframework.boot.actuate.autoconfigure.metrics.export.elastic;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Elastic
 * metrics export.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.elastic")
public class ElasticProperties extends StepRegistryProperties {

	/**
	 * Host to export metrics to.
	 */
	private String host = "http://localhost:9200";

	/**
	 * Index to export metrics to.
	 */
	private String index = "metrics";

	/**
	 * Index date format used for rolling indices. Appended to the index name, preceded by
	 * a '-'.
	 */
	private String indexDateFormat = "yyyy-MM";

	/**
	 * Name of the timestamp field.
	 */
	private String timestampFieldName = "@timestamp";

	/**
	 * Whether to create the index automatically if it does not exist.
	 */
	private boolean autoCreateIndex = true;

	/**
	 * Login user of the Elastic server.
	 */
	private String userName = "";

	/**
	 * Login password of the Elastic server.
	 */
	private String password = "";

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getIndex() {
		return this.index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getIndexDateFormat() {
		return this.indexDateFormat;
	}

	public void setIndexDateFormat(String indexDateFormat) {
		this.indexDateFormat = indexDateFormat;
	}

	public String getTimestampFieldName() {
		return this.timestampFieldName;
	}

	public void setTimestampFieldName(String timestampFieldName) {
		this.timestampFieldName = timestampFieldName;
	}

	public boolean isAutoCreateIndex() {
		return this.autoCreateIndex;
	}

	public void setAutoCreateIndex(boolean autoCreateIndex) {
		this.autoCreateIndex = autoCreateIndex;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
