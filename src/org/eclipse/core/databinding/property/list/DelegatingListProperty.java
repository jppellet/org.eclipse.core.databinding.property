/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 247997)
 *     Matthew Hall - bug 264306
 ******************************************************************************/

package org.eclipse.core.databinding.property.list;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.ISimplePropertyListener;

/**
 * @since 1.2
 * 
 */
public abstract class DelegatingListProperty<S, E> extends ListProperty<S, E> {
	private final IListProperty<S, E> nullProperty;
	private final Object elementType;

	protected DelegatingListProperty() {
		this(null);
	}

	protected DelegatingListProperty(Object elementType) {
		this.elementType = elementType;
		this.nullProperty = new NullListProperty();
	}

	/**
	 * Returns the property to delegate to for the specified source object.
	 * Repeated calls to this method with the same source object returns the
	 * same delegate instance.
	 * 
	 * @param source
	 *            the property source (may be null)
	 * @return the property to delegate to for the specified source object.
	 */
	public final IListProperty<S, E> getDelegate(S source) {
		if (source == null)
			return nullProperty;
		IListProperty<S, E> delegate = doGetDelegate(source);
		if (delegate == null)
			delegate = nullProperty;
		return delegate;
	}

	/**
	 * Returns the property to delegate to for the specified source object.
	 * Implementers must ensure that repeated calls to this method with the same
	 * source object returns the same delegate instance.
	 * 
	 * @param source
	 *            the property source
	 * @return the property to delegate to for the specified source object.
	 */
	protected abstract IListProperty<S, E> doGetDelegate(Object source);

	public Object getElementType() {
		return elementType;
	}

	protected List<E> doGetList(S source) {
		return getDelegate(source).getList(source);
	}

	protected void doSetList(S source, List<E> list) {
		getDelegate(source).setList(source, list);
	}

	protected void doUpdateList(S source, ListDiff<E> diff) {
		getDelegate(source).updateList(source, diff);
	}

	public IObservableList<E> observe(S source) {
		return getDelegate(source).observe(source);
	}

	public IObservableList<E> observe(Realm realm, S source) {
		return getDelegate(source).observe(realm, source);
	}

	private class NullListProperty extends SimpleListProperty<S, E> {
		public Object getElementType() {
			return elementType;
		}

		protected List<E> doGetList(S source) {
			return Collections.emptyList();
		}

		protected void doSetList(S source, List<E> list, ListDiff<E> diff) {
		}

		protected void doSetList(S source, List<E> list) {
		}

		protected void doUpdateList(S source, ListDiff<E> diff) {
		}

		public INativePropertyListener<S> adaptListener(
				ISimplePropertyListener<ListDiff<E>> listener) {
			return null;
		}
	}
}
