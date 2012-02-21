/*******************************************************************************
 * Copyright (c) 2009, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 263868)
 *     Matthew Hall - bug 268203
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property.list;

import java.util.List;

import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.databinding.property.list.SimpleListProperty;

/**
 * @since 3.3
 * 
 */
public class SelfListProperty<E> extends SimpleListProperty<List<E>, E> {
	private final Object elementType;

	/**
	 * @param elementType
	 */
	public SelfListProperty(Object elementType) {
		this.elementType = elementType;
	}

	public Object getElementType() {
		return elementType;
	}

	protected List<E> doGetList(List<E> source) {
		return source;
	}

	protected void doSetList(List<E> source, List<E> list, ListDiff<E> diff) {
		doUpdateList(source, diff);
	}

	protected void doUpdateList(List<E> source, ListDiff<E> diff) {
		diff.applyTo(source);
	}

	public INativePropertyListener<List<E>> adaptListener(
			ISimplePropertyListener<ListDiff<E>> listener) {
		return null; // no listener API
	}

	protected void doAddListener(Object source,
			INativePropertyListener<List<E>> listener) {
	}

	protected void doRemoveListener(Object source,
			INativePropertyListener<List<E>> listener) {
	}
}