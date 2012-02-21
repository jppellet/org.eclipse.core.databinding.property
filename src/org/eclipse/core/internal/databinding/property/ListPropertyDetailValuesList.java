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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.ListProperty;
import org.eclipse.core.databinding.property.value.IValueProperty;

/**
 * @since 3.3
 * 
 */
public class ListPropertyDetailValuesList<S, T, E> extends ListProperty<S, E> {
	private final IListProperty<S, T> masterProperty;
	private final IValueProperty<? super T, E> detailProperty;

	/**
	 * @param masterProperty
	 * @param detailProperty
	 */
	public ListPropertyDetailValuesList(IListProperty<S, T> masterProperty,
			IValueProperty<? super T, E> detailProperty) {
		this.masterProperty = masterProperty;
		this.detailProperty = detailProperty;
	}

	public Object getElementType() {
		return detailProperty.getValueType();
	}

	protected List<E> doGetList(S source) {
		List<T> masterList = masterProperty.getList(source);
		List<E> detailList = new ArrayList<E>(masterList.size());
		for (Iterator<T> it = masterList.iterator(); it.hasNext();)
			detailList.add(detailProperty.getValue(it.next()));
		return detailList;
	}

	protected void doUpdateList(S source, ListDiff<E> diff) {
		final List<T> masterList = masterProperty.getList(source);
		diff.accept(new ListDiffVisitor<E>() {
			public void handleAdd(int index, E element) {
				throw new UnsupportedOperationException();
			}

			public void handleRemove(int index, E element) {
				throw new UnsupportedOperationException();
			}

			public void handleMove(int oldIndex, int newIndex, E element) {
				throw new UnsupportedOperationException();
			}

			public void handleReplace(int index, E oldElement,
					E newElement) {
				detailProperty.setValue(masterList.get(index), newElement);
			}
		});
	}

	public IObservableList<E> observe(Realm realm, S source) {
		IObservableList<T> masterList;

		ObservableTracker.setIgnore(true);
		try {
			masterList = masterProperty.observe(realm, source);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableList<E> detailList = detailProperty.observeDetail(masterList);
		PropertyObservableUtil.cascadeDispose(detailList, masterList);
		return detailList;
	}

	public <U extends S> IObservableList<E> observeDetail(
			IObservableValue<U> master) {
		IObservableList<T> masterList;

		ObservableTracker.setIgnore(true);
		try {
			masterList = masterProperty.observeDetail(master);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		IObservableList<E> detailList = detailProperty.observeDetail(masterList);
		PropertyObservableUtil.cascadeDispose(detailList, masterList);
		return detailList;
	}

	public String toString() {
		return masterProperty + " => " + detailProperty; //$NON-NLS-1$
	}
}
