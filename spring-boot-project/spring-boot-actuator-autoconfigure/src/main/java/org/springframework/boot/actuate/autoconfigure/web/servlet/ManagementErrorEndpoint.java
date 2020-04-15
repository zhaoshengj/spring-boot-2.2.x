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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Map;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@link Controller @Controller} for handling "/error" path when the management servlet
 * is in a child context. The regular {@link ErrorController} should be available there
 * but because of the way the handler mappings are set up it will not be detected.
 *
 * @author Dave Syer
 * @since 2.0.0
 */
@Controller
public class ManagementErrorEndpoint {

	private final ErrorAttributes errorAttributes;

	public ManagementErrorEndpoint(ErrorAttributes errorAttributes) {
		Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
		this.errorAttributes = errorAttributes;
	}

	@RequestMapping("${server.error.path:${error.path:/error}}")
	@ResponseBody
	public Map<String, Object> invoke(ServletWebRequest request) {
		return this.errorAttributes.getErrorAttributes(request, false);
	}

}
