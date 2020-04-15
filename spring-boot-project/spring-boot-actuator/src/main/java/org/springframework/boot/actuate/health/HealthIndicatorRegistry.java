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

package org.springframework.boot.actuate.health;

import java.util.Map;

/**
 * A mutable registry of {@link HealthIndicator HealthIndicators}.
 * <p>
 * Implementations <strong>must</strong> be thread-safe.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.1.0
 * @see HealthEndpoint
 * @deprecated since 2.2.0 in favor of a {@link HealthContributorRegistry}
 */
@Deprecated
public interface HealthIndicatorRegistry {

	/**
	 * Registers the given {@link HealthIndicator}, associating it with the given
	 * {@code name}.
	 * @param name the name of the indicator
	 * @param healthIndicator the indicator
	 * @throws IllegalStateException if the indicator cannot be registered with the given
	 * {@code name}.
	 */
	void register(String name, HealthIndicator healthIndicator);

	/**
	 * Unregisters the {@link HealthIndicator} previously registered with the given
	 * {@code name}.
	 * @param name the name of the indicator
	 * @return the unregistered indicator, or {@code null} if no indicator was found in
	 * the registry for the given {@code name}.
	 */
	HealthIndicator unregister(String name);

	/**
	 * Returns the {@link HealthIndicator} registered with the given {@code name}.
	 * @param name the name of the indicator
	 * @return the health indicator, or {@code null} if no indicator was registered with
	 * the given {@code name}.
	 */
	HealthIndicator get(String name);

	/**
	 * Returns a snapshot of the registered health indicators and their names. The
	 * contents of the map do not reflect subsequent changes to the registry.
	 * @return the snapshot of registered health indicators
	 */
	Map<String, HealthIndicator> getAll();

}
