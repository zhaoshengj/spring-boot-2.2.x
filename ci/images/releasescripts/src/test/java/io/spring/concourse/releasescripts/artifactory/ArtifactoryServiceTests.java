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

package io.spring.concourse.releasescripts.artifactory;

import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.bintray.BintrayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link ArtifactoryService}.
 *
 * @author Madhura Bhave
 */
@RestClientTest(ArtifactoryService.class)
@EnableConfigurationProperties(ArtifactoryProperties.class)
class ArtifactoryServiceTests {

	@Autowired
	private ArtifactoryService service;

	@MockBean
	private BintrayService bintrayService;

	@Autowired
	private ArtifactoryProperties properties;

	@Autowired
	private MockRestServiceServer server;

	@AfterEach
	void tearDown() {
		this.server.reset();
	}

	@Test
	void promoteWhenSuccessful() {
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/promote/" + "example-build" + "/" + "example-build-1"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"{\"status\": \"staged\", \"sourceRepo\": \"libs-staging-local\", \"targetRepo\": \"libs-milestone-local\"}"))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(String
						.format("%s:%s", this.properties.getUsername(), this.properties.getPassword()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
		this.service.promote("libs-milestone-local", getReleaseInfo());
		this.server.verify();
	}

	@Test
	void promoteWhenArtifactsAlreadyPromoted() {
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/promote/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withStatus(HttpStatus.CONFLICT));
		this.server.expect(requestTo("https://repo.spring.io/api/build/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withJsonFrom("build-info-response.json"));
		this.service.promote("libs-release-local", getReleaseInfo());
		this.server.verify();
	}

	@Test
	void promoteWhenCheckForArtifactsAlreadyPromotedFails() {
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/promote/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withStatus(HttpStatus.CONFLICT));
		this.server.expect(requestTo("https://repo.spring.io/api/build/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> this.service.promote("libs-release-local", getReleaseInfo()));
		this.server.verify();
	}

	@Test
	void promoteWhenPromotionFails() {
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/promote/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withStatus(HttpStatus.CONFLICT));
		this.server.expect(requestTo("https://repo.spring.io/api/build/" + "example-build" + "/" + "example-build-1"))
				.andRespond(withJsonFrom("staged-build-info-response.json"));
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> this.service.promote("libs-release-local", getReleaseInfo()));
		this.server.verify();
	}

	@Test
	void distributeWhenSuccessful() throws Exception {
		ReleaseInfo releaseInfo = getReleaseInfo();
		given(this.bintrayService.isDistributionComplete(releaseInfo)).willReturn(true);
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/distribute/" + "example-build" + "/" + "example-build-1"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"{\"sourceRepos\": [\"libs-release-local\"], \"targetRepo\" : \"spring-distributions\", \"async\":\"true\"}"))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(String
						.format("%s:%s", this.properties.getUsername(), this.properties.getPassword()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
		this.service.distribute("libs-release-local", releaseInfo);
		this.server.verify();
		verify(this.bintrayService, times(1)).isDistributionComplete(releaseInfo);
	}

	@Test
	void distributeWhenFailure() throws Exception {
		ReleaseInfo releaseInfo = getReleaseInfo();
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/distribute/" + "example-build" + "/" + "example-build-1"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"{\"sourceRepos\": [\"libs-release-local\"], \"targetRepo\" : \"spring-distributions\", \"async\":\"true\"}"))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(String
						.format("%s:%s", this.properties.getUsername(), this.properties.getPassword()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString()))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> this.service.distribute("libs-release-local", releaseInfo));
		this.server.verify();
		verifyNoInteractions(this.bintrayService);
	}

	@Test
	void distributeWhenGettingPackagesTimesOut() throws Exception {
		ReleaseInfo releaseInfo = getReleaseInfo();
		given(this.bintrayService.isDistributionComplete(releaseInfo)).willReturn(false);
		this.server
				.expect(requestTo(
						"https://repo.spring.io/api/build/distribute/" + "example-build" + "/" + "example-build-1"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"{\"sourceRepos\": [\"libs-release-local\"], \"targetRepo\" : \"spring-distributions\", \"async\":\"true\"}"))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(String
						.format("%s:%s", this.properties.getUsername(), this.properties.getPassword()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
		assertThatExceptionOfType(DistributionTimeoutException.class)
				.isThrownBy(() -> this.service.distribute("libs-release-local", releaseInfo));
		this.server.verify();
		verify(this.bintrayService, times(1)).isDistributionComplete(releaseInfo);
	}

	private ReleaseInfo getReleaseInfo() {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		releaseInfo.setBuildName("example-build");
		releaseInfo.setBuildNumber("example-build-1");
		return releaseInfo;
	}

	private DefaultResponseCreator withJsonFrom(String path) {
		return withSuccess(getClassPathResource(path), MediaType.APPLICATION_JSON);
	}

	private ClassPathResource getClassPathResource(String path) {
		return new ClassPathResource(path, getClass());
	}

}