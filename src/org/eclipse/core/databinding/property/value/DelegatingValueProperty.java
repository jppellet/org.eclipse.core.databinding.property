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

package org.eclipse.core.databinding.property.value;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.internal.databinding.property.value.ListDelegatingValueObservableList;
import org.eclipse.core.internal.databinding.property.value.MapDelegatingValueObservableMap;
import org.eclipse.core.internal.databinding.property.value.SetDelegatingValueObservableMap;

/**
 * @since 1.2
 * 
 */
public abstract class DelegatingValueProperty<S, T> extends ValueProperty<S, T> {
	private final Object valueType;
	private final IValueProperty<S, T> nullProperty = new NullValueProperty();

	protected DelegatingValueProperty() {
		this(null);
	}

	protected DelegatingValueProperty(Object valueType) {
		this.valueType = valueType;
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
	public final IValueProperty<S, T> getDelegate(S source) {
		if (source == null)
			return nullProperty;
		IValueProperty<S, T> delegate = doGetDelegate(source);
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
	protected abstract IValueProperty<S, T> doGetDelegate(S source);

	protected T doGetValue(S source) {
		return getDelegate(source).getValue(source);
	}

	protected void doSetValue(S source, T value) {
		getDelegate(source).setValue(source, value);
	}

	public Object getValueType() {
		return valueType;
	}

	public IObservableValue<T> observe(S source) {
		return getDelegate(source).observe(source);
	}

	public IObservableValue<T> observe(Realm realm, S source) {
		return getDelegate(source).observe(realm, source);
	}

	public <U extends S> IObservableList<T> observeDetail(
			IObservableList<U> master) {
		return new ListDelegatingValueObservableList<S, U, T>(master, this);
	}

	public <U extends S> IObservableMap<U, T> observeDetail(
			IObservableSet<U> master) {
		return new SetDelegatingValueObservableMap<S, U, T>(master, this);
	}

	public <K, V extends S> IObservableMap<K, T> observeDetail(
			IObservableMap<K, V> master) {
		return new MapDelegatingValueObservableMap<S, K, V, T>(master, this);
	}

	private class NullValueProperty extends SimpleValueProperty<S, T> {
		public Object getValueType() {
			return valueType;
		}

		protected T doGetValue(S source) {
			return null;
		}

		protected void doSetValue(S source, T value) {
		}

		public INativePropertyListener<S> adaptListener(
				ISimplePropertyListener<ValueDiff<T>> listener) {
			return null;
		}
	}
}
