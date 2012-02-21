/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 195222, 247997, 265561
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
import org.eclipse.core.internal.databinding.property.value.ListSimpleValueObservableList;
import org.eclipse.core.internal.databinding.property.value.MapSimpleValueObservableMap;
import org.eclipse.core.internal.databinding.property.value.SetSimpleValueObservableMap;
import org.eclipse.core.internal.databinding.property.value.SimplePropertyObservableValue;

/**
 * Simplified abstract implementation of IValueProperty. This class takes care
 * of most of the functional requirements for an IValueProperty implementation,
 * leaving only the property-specific details to subclasses.
 * <p>
 * Subclasses must implement these methods:
 * <ul>
 * <li> {@link #getValueType()}
 * <li> {@link #doGetValue(Object)}
 * <li> {@link #doSetValue(Object, Object)}
 * <li> {@link #adaptListener(ISimplePropertyListener)}
 * </ul>
 * <p>
 * In addition, we recommended overriding {@link #toString()} to return a
 * description suitable for debugging purposes.
 * 
 * @since 1.2
 */
public abstract class SimpleValueProperty<S, T> extends ValueProperty<S, T> {
	protected abstract T doGetValue(S source);

	protected abstract void doSetValue(S source, T value);

	/**
	 * Returns a listener capable of adding or removing itself as a listener on
	 * a source object using the the source's "native" listener API. Events
	 * received from the source objects are parlayed to the specified listener
	 * argument.
	 * <p>
	 * This method returns null if the source object has no listener APIs for
	 * this property.
	 * 
	 * @param listener
	 *            the property listener to receive events
	 * @return a native listener which parlays property change events to the
	 *         specified listener, or null if the source object has no listener
	 *         APIs for this property.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract INativePropertyListener<S> adaptListener(
			ISimplePropertyListener<ValueDiff<T>> listener);

	public IObservableValue<T> observe(Realm realm, S source) {
		return new SimplePropertyObservableValue<S, T>(realm, source, this);
	}

	public <U extends S> IObservableList<T> observeDetail(
			IObservableList<U> master) {
		return new ListSimpleValueObservableList<S, U, T>(master, this);
	}

	public <U extends S> IObservableMap<U, T> observeDetail(
			IObservableSet<U> master) {
		return new SetSimpleValueObservableMap<S, U, T>(master, this);
	}

	public <K, V extends S> IObservableMap<K, T> observeDetail(
			IObservableMap<K, V> master) {
		return new MapSimpleValueObservableMap<S, K, V, T>(master, this);
	}
}