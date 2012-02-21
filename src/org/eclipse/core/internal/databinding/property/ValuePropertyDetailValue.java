/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 195222, 278550
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.databinding.property.value.ValueProperty;

/**
 * @since 1.2
 * 
 */
public class ValuePropertyDetailValue<S, U, T> extends ValueProperty<S, T> implements IValueProperty<S, T> {
	private IValueProperty<S, U> masterProperty;
	private IValueProperty<? super U, T> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ValuePropertyDetailValue(IValueProperty<S, U> masterProperty, IValueProperty<? super U, T> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getValueType() {
		return detailProperty.getValueType();
	}

	protected T doGetValue(S source) {
		U masterValue = masterProperty.getValue(source);
		return detailProperty.getValue(masterValue);
	}

	protected void doSetValue(S source, T value) {
		U masterValue = masterProperty.getValue(source);
		detailProperty.setValue(masterValue, value);
	}

	public IObservableValue<T> observe(Realm realm, S source) {
		IObservableValue<U> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableValue<T> detailValue = detailProperty.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailValue, masterValue);
		return detailValue;
	}

	public <V extends S> IObservableValue<T> observeDetail(IObservableValue<V> master) {
		IObservableValue<U> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableValue<T> detailValue = detailProperty.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailValue, masterValue);
		return detailValue;
	}

	public <V extends S> IObservableList<T> observeDetail(IObservableList<V> master) {
		IObservableList<U> masterList;

		ObservableTracker.setIgnore(true);
		try {
			masterList = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableList<T> detailList = detailProperty.observeDetail(masterList);
		PropertyObservableUtil.cascadeDispose(detailList, masterList);
		return detailList;
	}

	public <V extends S> IObservableMap<V, T> observeDetail(IObservableSet<V> master) {
		IObservableMap<V, U> masterMap;

		ObservableTracker.setIgnore(true);
		try {
			masterMap = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<V, T> detailMap = detailProperty.observeDetail(masterMap);
		PropertyObservableUtil.cascadeDispose(detailMap, masterMap);
		return detailMap;
	}

	public <K, V extends S> IObservableMap<K, T> observeDetail(IObservableMap<K, V> master) {
		IObservableMap<K, U> masterMap;

		ObservableTracker.setIgnore(true);
		try {
			masterMap = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<K, T> detailMap = detailProperty.observeDetail(masterMap);
		PropertyObservableUtil.cascadeDispose(detailMap, masterMap);
		return detailMap;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
