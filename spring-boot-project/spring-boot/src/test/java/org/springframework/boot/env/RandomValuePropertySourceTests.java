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

package org.springframework.boot.env;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link RandomValuePropertySource}.
 *
 * @author Dave Syer
 * @author Matt Benson
 */
class RandomValuePropertySourceTests {

	private RandomValuePropertySource source = new RandomValuePropertySource();

	@Test
	void notRandom() {
		assertThat(this.source.getProperty("foo")).isNull();
	}

	@Test
	void string() {
		assertThat(this.source.getProperty("random.string")).isNotNull();
	}

	@Test
	void intValue() {
		Integer value = (Integer) this.source.getProperty("random.int");
		assertThat(value).isNotNull();
	}

	@Test
	void uuidValue() {
		String value = (String) this.source.getProperty("random.uuid");
		assertThat(value).isNotNull();
		assertThat(UUID.fromString(value)).isNotNull();
	}

	@Test
	void intRange() {
		Integer value = (Integer) this.source.getProperty("random.int[4,10]");
		assertThat(value).isNotNull();
		assertThat(value >= 4).isTrue();
		assertThat(value < 10).isTrue();
	}

	@Test
	void intMax() {
		Integer value = (Integer) this.source.getProperty("random.int(10)");
		assertThat(value).isNotNull().isLessThan(10);
	}

	@Test
	void longValue() {
		Long value = (Long) this.source.getProperty("random.long");
		assertThat(value).isNotNull();
	}

	@Test
	void longRange() {
		Long value = (Long) this.source.getProperty("random.long[4,10]");
		assertThat(value).isNotNull().isBetween(4L, 10L);
	}

	@Test
	void longMax() {
		Long value = (Long) this.source.getProperty("random.long(10)");
		assertThat(value).isNotNull().isLessThan(10L);
	}

	@Test
	void longOverflow() {
		RandomValuePropertySource source = spy(this.source);
		given(source.getSource()).willReturn(new Random() {

			@Override
			public long nextLong() {
				// constant that used to become -8, now becomes 8
				return Long.MIN_VALUE;
			}

		});
		Long value = (Long) source.getProperty("random.long(10)");
		assertThat(value).isNotNull().isGreaterThanOrEqualTo(0L).isLessThan(10L);
		value = (Long) source.getProperty("random.long[4,10]");
		assertThat(value).isNotNull().isGreaterThanOrEqualTo(4L).isLessThan(10L);
	}

}
