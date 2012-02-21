/*******************************************************************************
 * Copyright (c) 2009 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 263868)
 *     Matthew Hall - bug 268203
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property.set;

import java.util.Set;

import org.eclipse.core.databinding.observable.set.SetDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.databinding.property.set.SimpleSetProperty;

/**
 * @since 3.3
 * 
 */
public final class SelfSetProperty<E> extends SimpleSetProperty<Set<E>, E> {
	private final Object elementType;

	/**
	 * @param elementType
	 */
	public SelfSetProperty(Object elementType) {
		this.elementType = elementType;
	}

	public Object getElementType() {
		return elementType;
	}

	protected Set<E> doGetSet(Set<E> source) {
		return source;
	}

	protected void doSetSet(Set<E> source, Set<E> set, SetDiff<E> diff) {
		diff.applyTo(source);
	}

	public INativePropertyListener<Set<E>> adaptListener(
			ISimplePropertyListener<SetDiff<E>> listener) {
		return null; // no listener API
	}

	protected void doAddListener(Object source,
			INativePropertyListener<Set<E>> listener) {
	}

	protected void doRemoveListener(Set<E> source,
			INativePropertyListener<Set<E>> listener) {
	}
}