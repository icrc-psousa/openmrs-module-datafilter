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

import static org.openmrs.module.datafilter.DataFilterConstants.MODULE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.db.hibernate.search.SearchAndEntityCollection;
import org.openmrs.api.db.hibernate.search.SearchCreatedEvent;
import org.openmrs.module.datafilter.registration.FilterParameter;
import org.openmrs.module.datafilter.registration.FullTextFilterRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listens for Hibernate Search (6.x) queries created by the OpenMRS core and restricts them with a
 * predicate for every enabled full text filter.
 */
@Component(MODULE_ID + "SearchCreatedEventListener")
public class SearchCreatedEventListener implements ApplicationListener<SearchCreatedEvent> {
	
	private static final Logger log = LoggerFactory.getLogger(SearchCreatedEventListener.class);
	
	/**
	 * Names of the parameters a full text filter must be given: the indexed field to match and the
	 * values to match against it.
	 */
	public static final String PARAM_FIELD = "field";
	
	public static final String PARAM_IDS = "patientIds";
	
	@Override
	public void onApplicationEvent(SearchCreatedEvent event) {
		if (Daemon.isDaemonThread()) {
			if (log.isTraceEnabled()) {
				log.trace("Skipping enabling of full text filters on daemon thread");
			}
			
			return;
		}
		
		SearchAndEntityCollection<?> search = (SearchAndEntityCollection<?>) event.getSource();
		Collection<? extends Class<?>> entityClasses = search.getEntityCollection();
		
		for (FullTextFilterRegistration registration : Util.getFullTextFilterRegistrations()) {
			if (Util.skipFilter(registration.getName())) {
				continue;
			}
			
			//Search is across all the entity classes in the scope, we only filter when all of them are targets
			Class<?> entityClass = null;
			boolean allTargeted = !entityClasses.isEmpty();
			for (Class<?> clazz : entityClasses) {
				if (registration.getTargetClasses().contains(clazz)) {
					entityClass = clazz;
				} else {
					allTargeted = false;
				}
			}
			
			if (!allTargeted) {
				if (log.isDebugEnabled()) {
					log.debug("Skipping enabling of filter " + registration.getName() + " on the full text query for "
					        + entityClasses);
				}
				
				continue;
			}
			
			Map<String, Map<String, Object>> filterParamsMap = new HashMap<>();
			if (CollectionUtils.isNotEmpty(registration.getParameters())) {
				filterParamsMap.put(registration.getName(), new HashMap<String, Object>());
			}
			
			DataFilterContext filterContext = new FullTextDataFilterContext(registration.getName(), filterParamsMap,
			        entityClass);
			
			boolean enable = true;
			for (DataFilterListener listener : Context.getRegisteredComponents(DataFilterListener.class)) {
				if (listener.supports(registration.getName())) {
					enable = listener.onEnableFilter(filterContext);
					break;
				}
			}
			
			if (!enable) {
				continue;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Enabling filter " + registration.getName() + " on the full text query for " + entityClass);
			}
			
			Map<String, Object> params = filterParamsMap.get(registration.getName());
			addFilter(search, (String) params.get(PARAM_FIELD), (Collection<?>) params.get(PARAM_IDS));
		}
	}
	
	/**
	 * Restricts the search to documents with one of the specified ids in the specified field, any
	 * predicate already set is preserved and both must match.
	 */
	private void addFilter(SearchAndEntityCollection<?> search, String field, Collection<?> ids) {
		final Function<SearchPredicateFactory, SearchPredicate> existing = search.getFinalPredicate();
		final List<Integer> idValues = new ArrayList<>(ids.size());
		for (Object id : ids) {
			idValues.add(Integer.valueOf(id.toString()));
		}
		
		search.setFinalPredicate(f -> {
			SearchPredicate filter = f.terms().field(field).matchingAny(idValues).toPredicate();
			if (existing == null) {
				return filter;
			}
			
			return f.bool().must(existing.apply(f)).must(filter).toPredicate();
		});
	}
	
}
