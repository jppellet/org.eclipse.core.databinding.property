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

import java.util.List;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.ListProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * @since 3.3
 * 
 */
public class ValuePropertyDetailList<S, T, E> extends ListProperty<S, E> {
	private final IValueProperty<S, T> masterProperty;
	private final IListProperty<? super T, E> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ValuePropertyDetailList(IValueProperty<S, T> masterProperty,
			IListProperty<? super T, E> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getElementType() {
		return detailProperty.getElementType();
	}

	protected List<E> doGetList(S source) {
		T masterValue = masterProperty.getValue(source);
		return detailProperty.getList(masterValue);
	}

	protected void doSetList(S source, List<E> list) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.setList(masterValue, list);
	}

	protected void doUpdateList(S source, ListDiff<E> diff) {
		T masterValue = masterProperty.getValue(source);
		detailProperty.updateList(masterValue, diff);
	}

	public IObservableList<E> observe(Realm realm, S source) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableList<E> detailList = detailProperty
				.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailList, masterValue);
		return detailList;
	}

	public <U extends S> IObservableList<E> observeDetail(
			IObservableValue<U> master) {
		IObservableValue<T> masterValue;

		ObservableTracker.setIgnore(true);
		try {
			masterValue = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableList<E> detailList = detailProperty
				.observeDetail(masterValue);
		PropertyObservableUtil.cascadeDispose(detailList, masterValue);
		return detailList;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
