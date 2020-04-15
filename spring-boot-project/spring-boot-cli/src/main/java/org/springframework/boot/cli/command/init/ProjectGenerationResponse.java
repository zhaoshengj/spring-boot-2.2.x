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

import org.apache.http.entity.ContentType;

/**
 * Represent the response of a {@link ProjectGenerationRequest}.
 *
 * @author Stephane Nicoll
 */
class ProjectGenerationResponse {

	private final ContentType contentType;

	private byte[] content;

	private String fileName;

	ProjectGenerationResponse(ContentType contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the {@link ContentType} of this instance.
	 * @return the content type
	 */
	ContentType getContentType() {
		return this.contentType;
	}

	/**
	 * The generated project archive or file.
	 * @return the content
	 */
	byte[] getContent() {
		return this.content;
	}

	void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * The preferred file name to use to store the entity on disk or {@code null} if no
	 * preferred value has been set.
	 * @return the file name, or {@code null}
	 */
	String getFileName() {
		return this.fileName;
	}

	void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
