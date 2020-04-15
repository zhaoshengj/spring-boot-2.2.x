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

package org.springframework.boot.cli.command.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent a project type that is supported by a service.
 *
 * @author Stephane Nicoll
 */
class ProjectType {

	private final String id;

	private final String name;

	private final String action;

	private final boolean defaultType;

	private final Map<String, String> tags = new HashMap<>();

	ProjectType(String id, String name, String action, boolean defaultType, Map<String, String> tags) {
		this.id = id;
		this.name = name;
		this.action = action;
		this.defaultType = defaultType;
		if (tags != null) {
			this.tags.putAll(tags);
		}
	}

	String getId() {
		return this.id;
	}

	String getName() {
		return this.name;
	}

	String getAction() {
		return this.action;
	}

	boolean isDefaultType() {
		return this.defaultType;
	}

	Map<String, String> getTags() {
		return Collections.unmodifiableMap(this.tags);
	}

}
