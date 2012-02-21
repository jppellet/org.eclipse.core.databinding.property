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

import java.util.Set;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.SetDiff;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.set.ISetProperty;
import org.eclipse.core.databinding.property.set.SetProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * @since 3.3
 * 
 */
public class ValuePropertyDetailSet<S, T, E> extends SetProperty<S, E> {
	private IValueProperty<S, T> masterProperty;
	private ISetProperty<? super T, E> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ValuePropertyDetailSet(IValueProperty<S, T> masterProperty,
			ISetProperty<? super T, E> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getElementType() {
		return detailProperty.getElementType();
	}

	protected Set<E> doGetSet(S source) {
		T masterValue = masterProperty.getValue(source);
		return detailProperty.getSet(masterValue);
	}

	protected void doSetSet(S source, Set<E> set) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.setSet(masterValue, set);
	}

	protected void doUpdateSet(S source, SetDiff<E> diff) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.updateSet(masterValue, diff);
	}

	public IObservableSet<E> observe(Realm realm, S source) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableSet<E> detailSet = detailProperty.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailSet, masterValue);
		return detailSet;
	}

	public <U extends S> IObservableSet<E> observeDetail(
			IObservableValue<U> master) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableSet<E> detailSet = detailProperty.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailSet, masterValue);
		return detailSet;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
