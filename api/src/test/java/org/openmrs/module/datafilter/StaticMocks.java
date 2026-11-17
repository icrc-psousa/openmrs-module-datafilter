/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * 
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.datafilter;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * JUnit rule that creates static mocks for the duration of a single test and closes them
 * afterwards, replaces PowerMock's mockStatic.
 */
public class StaticMocks extends ExternalResource {
	
	private final List<MockedStatic<?>> mocks = new ArrayList<>();
	
	public <T> MockedStatic<T> mock(Class<T> clazz) {
		MockedStatic<T> mock = Mockito.mockStatic(clazz);
		mocks.add(mock);
		return mock;
	}
	
	@Override
	protected void after() {
		mocks.forEach(MockedStatic::close);
		mocks.clear();
	}
}
