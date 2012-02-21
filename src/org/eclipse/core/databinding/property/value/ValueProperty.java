/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bug 195222
 *     Ovidio Mallo - bugs 331348, 305367
 ******************************************************************************/

package org.eclipse.core.databinding.property.value;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.masterdetail.MasterDetailObservables;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.map.IMapProperty;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.internal.databinding.property.ValuePropertyDetailList;
import org.eclipse.core.internal.databinding.property.ValuePropertyDetailMap;
import org.eclipse.core.internal.databinding.property.ValuePropertyDetailSet;
import org.eclipse.core.internal.databinding.property.ValuePropertyDetailValue;

/**
 * Abstract implementation of IValueProperty
 * 
 * @since 1.2
 */
public abstract class ValueProperty<S, T> implements IValueProperty<S, T> {

	/**
	 * By default, this method returns <code>null</code> in case the source
	 * object is itself <code>null</code>. Otherwise, this method delegates to
	 * {@link #doGetValue(Object)}.
	 * 
	 * <p>
	 * Clients may override this method if they e.g. want to return a specific
	 * default value in case the source object is <code>null</code>.
	 * </p>
	 * 
	 * @see #doGetValue(Object)
	 * 
	 * @since 1.3
	 */
	public final T getValue(S source) {
		if (source == null) {
			return null;
		}
		return doGetValue(source);
	}

	/**
	 * Returns the value of the property on the specified source object
	 * 
	 * @param source
	 *            the property source
	 * @return the current value of the source's value property
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 1.3
	 */
	protected T doGetValue(S source) {
		IObservableValue<T> observable = observe(source);
		try {
			return observable.getValue();
		} finally {
			observable.dispose();
		}
	}

	/**
	 * @since 1.3
	 */
	public final void setValue(S source, T value) {
		if (source != null) {
			doSetValue(source, value);
		}
	}

	/**
	 * Sets the source's value property to the specified vlaue
	 * 
	 * @param source
	 *            the property source
	 * @param value
	 *            the new value
	 * @since 1.3
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void doSetValue(S source, T value) {
		IObservableValue<T> observable = observe(source);
		try {
			observable.setValue(value);
		} finally {
			observable.dispose();
		}
	}

	public IObservableValue<T> observe(S source) {
		return observe(Realm.getDefault(), source);
	}

	public IObservableFactory<IObservableValue<T>, S> valueFactory() {
		return new IObservableFactory<IObservableValue<T>, S>() {
			public IObservableValue<T> createObservable(S target) {
				return observe(target);
			}
		};
	}

	public IObservableFactory<IObservableValue<T>, S> valueFactory(
			final Realm realm) {
		return new IObservableFactory<IObservableValue<T>, S>() {
			public IObservableValue<T> createObservable(S target) {
				return observe(realm, target);
			}
		};
	}

	public <U extends S> IObservableValue<T> observeDetail(
			IObservableValue<U> master) {
		return MasterDetailObservables.detailValue(master,
				valueFactory(master.getRealm()), getValueType());
	}

	/**
	 * @since 1.4
	 */
	public <V extends S> IObservableList<T> observeDetail(IObservableList<V> master) {
		return MasterDetailObservables.detailValues(master,
				valueFactory(master.getRealm()), getValueType());
	}

	/**
	 * @since 1.4
	 */
	public <V extends S> IObservableMap<V, T> observeDetail(IObservableSet<V> master) {
		return MasterDetailObservables.detailValues(master,
				valueFactory(master.getRealm()), getValueType());
	}

	/**
	 * @since 1.4
	 */
	public <K, V extends S> IObservableMap<K, T> observeDetail(IObservableMap<K, V> master) {
		return MasterDetailObservables.detailValues(master,
				valueFactory(master.getRealm()), getValueType());
	}

	public final <U> IValueProperty<S, U> value(
			IValueProperty<? super T, U> detailValue) {
		return new ValuePropertyDetailValue<S, T, U>(this, detailValue);
	}

	public final <E> IListProperty<S, E> list(
			IListProperty<? super T, E> detailList) {
		return new ValuePropertyDetailList<S, T, E>(this, detailList);
	}

	public final <E> ISetProperty<S, E> set(ISetProperty<? super T, E> detailSet) {
		return new ValuePropertyDetailSet<S, T, E>(this, detailSet);
	}

	public final <K, V> IMapProperty<S, K, V> map(
			IMapProperty<? super T, K, V> detailMap) {
		return new ValuePropertyDetailMap<S, T, K, V>(this, detailMap);
	}
}
