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

import javax.sql.DataSource;

import org.springframework.boot.jdbc.AbstractDataSourceInitializer;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Initializer for Spring Session schema.
 *
 * @author Vedran Pavic
 * @since 1.4.0
 */
public class JdbcSessionDataSourceInitializer extends AbstractDataSourceInitializer {

	private final JdbcSessionProperties properties;

	public JdbcSessionDataSourceInitializer(DataSource dataSource, ResourceLoader resourceLoader,
			JdbcSessionProperties properties) {
		super(dataSource, resourceLoader);
		Assert.notNull(properties, "JdbcSessionProperties must not be null");
		this.properties = properties;
	}

	@Override
	protected DataSourceInitializationMode getMode() {
		return this.properties.getInitializeSchema();
	}

	@Override
	protected String getSchemaLocation() {
		return this.properties.getSchema();
	}

}
