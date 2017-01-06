/*
 * Adapted from GraphWalker of JOL
 * 
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package de.fmannhardt.objectsize;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

/**
 * Calculates the object size (deep/retained) by walking through the object
 * graph with JOL.
 *
 * @author F. Mannhardt
 * @author Aleksey Shipilev
 */
public class ObjectSize {

	private final VirtualMachine vm;

	private final Set<Object> visited;
	private final Object[] roots;

	public ObjectSize(Object... roots) {
		this.roots = roots;
		this.visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
		this.vm = VM.current();
	}

	public long calculateSize() {
		long size = 0;
		List<Object> curLayer = new ArrayList<Object>();
		List<Object> newLayer = new ArrayList<Object>();

		for (Object root : roots) {
			visited.add(root);
			size += size(root);
			curLayer.add(root);
		}

		while (!curLayer.isEmpty()) {
			newLayer.clear();
			for (Object next : curLayer) {
				for (Object ref : peelReferences(next)) {
					if (visited.add(ref)) {
						size += size(ref);
						newLayer.add(ref);
					}
				}
			}
			curLayer.clear();
			curLayer.addAll(newLayer);
		}

		return size;
	}

	private long size(Object obj) {
		return obj != null ? vm.sizeOf(obj) : 0;
	}

	private static List<Object> peelReferences(Object o) {

		if (o == null) {
			// Nothing to do here
			return Collections.emptyList();
		}

		if (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()) {
			// Nothing to do here
			return Collections.emptyList();
		}

		List<Object> result = new ArrayList<Object>();

		if (o.getClass().isArray()) {
			for (Object e : (Object[]) o) {
				if (e != null) {
					result.add(e);
				}
			}
		} else {
			for (Field f : getAllReferences(o.getClass())) {
				try {
					f.setAccessible(true);
				} catch (Exception e) {
					// Access denied
					result.add(null);
					continue;
				}

				try {
					Object e = f.get(o);
					if (e != null) {
						result.add(e);
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		return result;
	}

	private static Collection<Field> getAllReferences(Class<?> klass) {
		List<Field> results = new ArrayList<Field>();

		for (Field f : klass.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			if (f.getType().isPrimitive())
				continue;
			results.add(f);
		}

		Class<?> superKlass = klass;
		while ((superKlass = superKlass.getSuperclass()) != null) {
			for (Field f : superKlass.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				if (f.getType().isPrimitive())
					continue;
				results.add(f);
			}
		}

		return results;
	}

	public static long sizeOf(Object obj) {
		return new ObjectSize(obj).calculateSize();
	}

}
