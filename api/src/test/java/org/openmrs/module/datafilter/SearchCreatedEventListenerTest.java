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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.search.SearchAndEntityCollection;
import org.openmrs.api.db.hibernate.search.SearchCreatedEvent;
import org.openmrs.api.db.hibernate.search.SearchQueryContributor;
import org.openmrs.module.datafilter.registration.FilterParameter;
import org.openmrs.module.datafilter.registration.FullTextFilterRegistration;

public class SearchCreatedEventListenerTest {
	
	@Rule
	public StaticMocks statics = new StaticMocks();
	
	private static final String FILTER_1 = "filter1";
	
	private static final String FILTER_2 = "filter2";
	
	private DataFilterListener dataFilterListener = mock(DataFilterListener.class);
	
	private FullTextFilterRegistration registration(String name) {
		FullTextFilterRegistration reg = new FullTextFilterRegistration();
		reg.setName(name);
		reg.setTargetClasses(Collections.singletonList(PersonName.class));
		FilterParameter field = new FilterParameter();
		field.setName(SearchCreatedEventListener.PARAM_FIELD);
		FilterParameter ids = new FilterParameter();
		ids.setName(SearchCreatedEventListener.PARAM_IDS);
		reg.setParameters(Stream.of(field, ids).collect(Collectors.toList()));
		return reg;
	}
	
	@Before
	public void setup() {
		statics.mock(Util.class);
		statics.mock(Context.class);
		List<FullTextFilterRegistration> regs = Stream.of(registration(FILTER_1), registration(FILTER_2))
		        .collect(Collectors.toList());
		when(Util.getFullTextFilterRegistrations()).thenReturn(regs);
		when(Util.skipFilter(anyString())).thenCallRealMethod();
		when(Context.isAuthenticated()).thenReturn(true);
		when(Context.getRegisteredComponents(DataFilterListener.class))
		        .thenReturn(Collections.singletonList(dataFilterListener));
		when(dataFilterListener.supports(anyString())).thenReturn(true);
		when(dataFilterListener.onEnableFilter(any(DataFilterContext.class))).thenAnswer(i -> {
			DataFilterContext ctx = i.getArgument(0);
			ctx.setParameter(SearchCreatedEventListener.PARAM_FIELD, "person.personId");
			ctx.setParameter(SearchCreatedEventListener.PARAM_IDS, Collections.singleton("1"));
			return true;
		});
	}
	
	private SearchAndEntityCollection<?> search(Class<?> clazz) {
		SearchQueryContributor contributor = new SearchQueryContributor() {
			
			private Function<SearchPredicateFactory, SearchPredicate> predicate;
			
			@Override
			public void setFinalPredicate(Function<SearchPredicateFactory, SearchPredicate> finalPredicate) {
				predicate = finalPredicate;
			}
			
			@Override
			public Function<SearchPredicateFactory, SearchPredicate> getFinalPredicate() {
				return predicate;
			}
		};
		return new SearchAndEntityCollection<>(contributor, Collections.singletonList(clazz));
	}
	
	@Test
	public void onApplicationEvent_shouldSetAPredicateForATargetedEntityClass() {
		SearchAndEntityCollection<?> search = search(PersonName.class);
		
		new SearchCreatedEventListener().onApplicationEvent(new SearchCreatedEvent(search));
		
		assertNotNull(search.getFinalPredicate());
	}
	
	@Test
	public void onApplicationEvent_shouldNotSetAPredicateForAnEntityClassThatIsNotTargeted() {
		SearchAndEntityCollection<?> search = search(org.openmrs.Concept.class);
		
		new SearchCreatedEventListener().onApplicationEvent(new SearchCreatedEvent(search));
		
		assertNull(search.getFinalPredicate());
	}
	
	@Test
	public void onApplicationEvent_shouldNotSetAPredicateIfTheListenerDisablesTheFilters() {
		when(dataFilterListener.onEnableFilter(any(DataFilterContext.class))).thenReturn(false);
		SearchAndEntityCollection<?> search = search(PersonName.class);
		
		new SearchCreatedEventListener().onApplicationEvent(new SearchCreatedEvent(search));
		
		assertNull(search.getFinalPredicate());
	}
	
	@Test
	public void onApplicationEvent_shouldNotEnableFiltersForWhichTheUserHasASpecificByPassPrivilege() {
		when(Context.hasPrivilege(eq(FILTER_1 + DataFilterConstants.BYPASS_PRIV_SUFFIX))).thenReturn(true);
		when(Context.hasPrivilege(eq(FILTER_2 + DataFilterConstants.BYPASS_PRIV_SUFFIX))).thenReturn(true);
		SearchAndEntityCollection<?> search = search(PersonName.class);
		
		new SearchCreatedEventListener().onApplicationEvent(new SearchCreatedEvent(search));
		
		assertNull(search.getFinalPredicate());
	}
}
