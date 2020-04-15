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

package org.springframework.boot.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Origin}.
 *
 * @author Phillip Webb
 */
class OriginTests {

	@Test
	void fromWhenSourceIsNullShouldReturnNull() {
		assertThat(Origin.from(null)).isNull();
	}

	@Test
	void fromWhenSourceIsRegularObjectShouldReturnNull() {
		Object source = new Object();
		assertThat(Origin.from(source)).isNull();
	}

	@Test
	void fromWhenSourceIsOriginShouldReturnSource() {
		Origin origin = mock(Origin.class);
		assertThat(Origin.from(origin)).isEqualTo(origin);
	}

	@Test
	void fromWhenSourceIsOriginProviderShouldReturnProvidedOrigin() {
		Origin origin = mock(Origin.class);
		OriginProvider originProvider = mock(OriginProvider.class);
		given(originProvider.getOrigin()).willReturn(origin);
		assertThat(Origin.from(origin)).isEqualTo(origin);
	}

	@Test
	void fromWhenSourceIsThrowableShouldUseCause() {
		Origin origin = mock(Origin.class);
		Exception exception = new RuntimeException(new TestException(origin, null));
		assertThat(Origin.from(exception)).isEqualTo(origin);
	}

	@Test
	void fromWhenSourceIsThrowableAndOriginProviderThatReturnsNullShouldUseCause() {
		Origin origin = mock(Origin.class);
		Exception exception = new TestException(null, new TestException(origin, null));
		assertThat(Origin.from(exception)).isEqualTo(origin);
	}

	static class TestException extends RuntimeException implements OriginProvider {

		private final Origin origin;

		TestException(Origin origin, Throwable cause) {
			super(cause);
			this.origin = origin;
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

	}

}
