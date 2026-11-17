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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Minimal replacement for PowerMock's Whitebox.
 */
public final class TestReflection {
	
	private TestReflection() {
	}
	
	private static Field find(Class<?> clazz, String name, Class<?> type) {
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if ((name != null && f.getName().equals(name)) || (name == null && f.getType().equals(type))) {
					f.setAccessible(true);
					return f;
				}
			}
		}
		throw new IllegalArgumentException("No field " + (name != null ? name : "of type " + type) + " in " + clazz);
	}
	
	private static void set(Object target, Field f, Object value) throws ReflectiveOperationException {
		if (Modifier.isFinal(f.getModifiers())) {
			Field mods = Field.class.getDeclaredField("modifiers");
			mods.setAccessible(true);
			mods.setInt(f, f.getModifiers() & ~Modifier.FINAL);
		}
		f.set(target, value);
	}
	
	/** Sets a static field by name, instance field when target is an object. */
	public static void setInternalState(Object target, String field, Object value) {
		try {
			boolean isClass = target instanceof Class;
			set(isClass ? null : target, find(isClass ? (Class<?>) target : target.getClass(), field, null), value);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Sets the static field of the specified type. */
	public static void setInternalState(Class<?> target, Class<?> fieldType, Object value) {
		try {
			set(null, find(target, null, fieldType), value);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object target, String field) {
		try {
			return (T) find(target.getClass(), field, null).get(target);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object target, Class<?> fieldType) {
		try {
			return (T) find(target.getClass(), null, fieldType).get(target);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
