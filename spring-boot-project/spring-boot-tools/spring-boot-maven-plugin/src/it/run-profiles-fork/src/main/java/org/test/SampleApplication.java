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

package org.test;

import java.util.Arrays;

public class SampleApplication {

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException("Missing active profile argument " + Arrays.toString(args) + "");
		}
		String argument = args[0];
		if (!argument.startsWith("--spring.profiles.active=")) {
			throw new IllegalArgumentException("Invalid argument " + argument);
		}
		int index = args[0].indexOf('=');
		String profile = argument.substring(index + 1);
		System.out.println("I haz been run with profile(s) '" + profile + "'");
	}

}
