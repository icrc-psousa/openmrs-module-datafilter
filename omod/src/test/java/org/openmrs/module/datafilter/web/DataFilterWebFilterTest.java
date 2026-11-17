/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.datafilter.web;

import javax.servlet.FilterChain;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.datafilter.DataFilterSessionContext;

public class DataFilterWebFilterTest {
	
	@Mock
	private FilterChain filterChain;
	
	@Test
	public void doFilter_shouldAlwaysResetTheFiltersOnTheCurrentThread() throws Exception {
		try (AutoCloseable ignored = MockitoAnnotations.openMocks(this);
		        MockedStatic<DataFilterSessionContext> mock = Mockito.mockStatic(DataFilterSessionContext.class)) {
			
			new DataFilterWebFilter().doFilter(null, null, filterChain);
			
			mock.verify(DataFilterSessionContext::reset);
		}
	}
	
}
