/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bug 278550
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property;

import java.util.Map;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.MapDiff;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.map.IMapProperty;
import org.eclipse.core.databinding.property.map.MapProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * @since 3.3
 * 
 */
public class ValuePropertyDetailMap<S, T, K, V> extends MapProperty<S, K, V> {
	private final IValueProperty<S, T> masterProperty;
	private final IMapProperty<? super T, K, V> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ValuePropertyDetailMap(IValueProperty<S, T> masterProperty,
			IMapProperty<? super T, K, V> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getKeyType() {
		return detailProperty.getKeyType();
	}

	public Object getValueType() {
		return detailProperty.getValueType();
	}

	protected Map<K, V> doGetMap(S source) {
		T masterValue = masterProperty.getValue(source);
		return detailProperty.getMap(masterValue);
	}

	protected void doSetMap(S source, Map<K, V> map) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.setMap(masterValue, map);
	}

	protected void doUpdateMap(S source, MapDiff<K, V> diff) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.updateMap(masterValue, diff);
	}

	public IObservableMap<K, V> observe(Realm realm, S source) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<K, V> detailMap = detailProperty
				.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailMap, masterValue);
		return detailMap;
	}

	public <U extends S> IObservableMap<K, V> observeDetail(
			IObservableValue<U> master) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<K, V> detailMap = detailProperty
				.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailMap, masterValue);
		return detailMap;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
