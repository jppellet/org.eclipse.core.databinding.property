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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.MapDiff;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.map.MapProperty;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.internal.databinding.identity.IdentityMap;

/**
 * @since 3.3
 * 
 */
public class SetPropertyDetailValuesMap<S, E, T> extends MapProperty<S, E, T> {
	private final ISetProperty<S, E> masterProperty;
	private final IValueProperty<? super E, T> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public SetPropertyDetailValuesMap(ISetProperty<S, E> masterProperty,
			IValueProperty<? super E, T> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getKeyType() {
		return masterProperty.getElementType();
	}

	public Object getValueType() {
		return detailProperty.getValueType();
	}

	protected Map<E, T> doGetMap(S source) {
		Set<E> set = masterProperty.getSet(source);
		Map<E, T> map = new IdentityMap<E, T>();
		for (Iterator<E> it = set.iterator(); it.hasNext();) {
			E key = it.next();
			map.put(key, detailProperty.getValue(key));
		}
		return map;
	}

	protected void doUpdateMap(S source, MapDiff<E, T> diff) {
		if (!diff.getAddedKeys().isEmpty())
			throw new UnsupportedOperationException(toString()
					+ " does not support entry additions"); //$NON-NLS-1$
		if (!diff.getRemovedKeys().isEmpty())
			throw new UnsupportedOperationException(toString()
					+ " does not support entry removals"); //$NON-NLS-1$
		for (Iterator<E> it = diff.getChangedKeys().iterator(); it.hasNext();) {
			E key = it.next();
			T newValue = diff.getNewValue(key);
			detailProperty.setValue(key, newValue);
		}
	}

	public IObservableMap<E, T> observe(Realm realm, S source) {
		IObservableSet<E> masterSet;

		ObservableTracker.setIgnore(true);
		try {
			masterSet = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<E, T> detailMap = detailProperty
				.observeDetail(masterSet);
		PropertyObservableUtil.cascadeDispose(detailMap, masterSet);
		return detailMap;
	}

	public <U extends S> IObservableMap<E, T> observeDetail(
			IObservableValue<U> master) {
		IObservableSet<E> masterSet;

		ObservableTracker.setIgnore(true);
		try {
			masterSet = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableMap<E, T> detailMap = detailProperty
				.observeDetail(masterSet);
		PropertyObservableUtil.cascadeDispose(detailMap, masterSet);
		return detailMap;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
