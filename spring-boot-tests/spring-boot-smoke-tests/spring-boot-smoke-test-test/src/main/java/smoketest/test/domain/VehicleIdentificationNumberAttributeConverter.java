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

package smoketest.test.domain;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA {@link AttributeConverter} for {@link VehicleIdentificationNumber}.
 *
 * @author Phillip Webb
 */
@Converter(autoApply = true)
public class VehicleIdentificationNumberAttributeConverter
		implements AttributeConverter<VehicleIdentificationNumber, String> {

	@Override
	public String convertToDatabaseColumn(VehicleIdentificationNumber attribute) {
		return attribute.toString();
	}

	@Override
	public VehicleIdentificationNumber convertToEntityAttribute(String dbData) {
		return new VehicleIdentificationNumber(dbData);
	}

}
