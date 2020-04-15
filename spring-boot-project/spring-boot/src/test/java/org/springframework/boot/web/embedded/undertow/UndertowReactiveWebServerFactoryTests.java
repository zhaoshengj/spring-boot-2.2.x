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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;

import io.undertow.Undertow;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowReactiveWebServerFactory} and {@link UndertowWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class UndertowReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

	@TempDir
	File tempDir;

	@Override
	protected UndertowReactiveWebServerFactory getFactory() {
		return new UndertowReactiveWebServerFactory(0);
	}

	@Test
	void setNullBuilderCustomizersShouldThrowException() {
		UndertowReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setBuilderCustomizers(null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void addNullBuilderCustomizersShouldThrowException() {
		UndertowReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void builderCustomizersShouldBeInvoked() {
		UndertowReactiveWebServerFactory factory = getFactory();
		HttpHandler handler = mock(HttpHandler.class);
		UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
		Arrays.setAll(customizers, (i) -> mock(UndertowBuilderCustomizer.class));
		factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addBuilderCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowBuilderCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(Undertow.Builder.class));
		}
	}

	@Test
	void useForwardedHeaders() {
		UndertowReactiveWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void accessLogCanBeEnabled() throws IOException, URISyntaxException, InterruptedException {
		testAccessLog(null, null, "access_log.log");
	}

	@Test
	void accessLogCanBeCustomized() throws IOException, URISyntaxException, InterruptedException {
		testAccessLog("my_access.", "logz", "my_access.logz");
	}

	private void testAccessLog(String prefix, String suffix, String expectedFile)
			throws IOException, URISyntaxException, InterruptedException {
		UndertowReactiveWebServerFactory factory = getFactory();
		factory.setAccessLogEnabled(true);
		factory.setAccessLogPrefix(prefix);
		factory.setAccessLogSuffix(suffix);
		File accessLogDirectory = this.tempDir;
		factory.setAccessLogDirectory(accessLogDirectory);
		assertThat(accessLogDirectory.listFiles()).isEmpty();
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		WebClient client = getWebClient().build();
		Mono<String> result = client.post().uri("/test").contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World")).exchange()
				.flatMap((response) -> response.bodyToMono(String.class));
		assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
		File accessLog = new File(accessLogDirectory, expectedFile);
		awaitFile(accessLog);
		assertThat(accessLogDirectory.listFiles()).contains(accessLog);
	}

	private void awaitFile(File file) {
		Awaitility.waitAtMost(Duration.ofSeconds(10)).until(file::exists, is(true));
	}

}
