/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.datafilter.annotations;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;

/**
 * An instance of this class represents a {@link Filter} annotation to be added to a persistent
 * class mapped with annotations.
 */
public class FilterAnnotation implements Filter {
	
	private String name;
	
	private String condition;
	
	/**
	 * Constructor
	 * 
	 * @param name the name of the filter
	 * @param condition the condition to set on the filter
	 */
	public FilterAnnotation(String name, String condition) {
		this.name = name;
		this.condition = condition;
	}
	
	/**
	 * @see Filter#name()
	 */
	@Override
	public String name() {
		return name;
	}
	
	/**
	 * @see Filter#condition()
	 */
	@Override
	public String condition() {
		return condition;
	}
	
	/**
	 * @see Filter#deduceAliasInjectionPoints()
	 */
	@Override
	public boolean deduceAliasInjectionPoints() {
		return true;
	}
	
	/**
	 * @see Filter#aliases()
	 */
	@Override
	public SqlFragmentAlias[] aliases() {
		return new SqlFragmentAlias[] {};
	}
	
	/**
	 * @see Filter#annotationType()
	 */
	@Override
	public Class<? extends Annotation> annotationType() {
		return Filter.class;
	}
	
}
