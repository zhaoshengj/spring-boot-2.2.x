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

package org.springframework.boot.devtools.restart.server;

import java.net.URL;

/**
 * Filter URLs based on a source folder name. Used to match URLs from the running
 * classpath against source folders on a remote system.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see DefaultSourceFolderUrlFilter
 */
@FunctionalInterface
public interface SourceFolderUrlFilter {

	/**
	 * Determine if the specified URL matches a source folder.
	 * @param sourceFolder the source folder
	 * @param url the URL to check
	 * @return {@code true} if the URL matches
	 */
	boolean isMatch(String sourceFolder, URL url);

}
