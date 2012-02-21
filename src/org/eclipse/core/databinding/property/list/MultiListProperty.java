/*******************************************************************************
 * Copyright (c) 2009, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 265727)
 ******************************************************************************/

package org.eclipse.core.databinding.property.list;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.ObservableArrayHelper;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.databinding.observable.list.ListDiffVisitor;
import org.eclipse.core.databinding.observable.list.MultiList;
import org.eclipse.core.internal.databinding.property.PropertyObservableUtil;

/**
 * A list property for observing multiple list properties in sequence in a
 * combined list.
 * 
 * @since 1.2
 */
public class MultiListProperty<S, E> extends ListProperty<S, E> {
	private IListProperty<S, E>[] properties;
	private Object elementType;

	/**
	 * Constructs a MultiListProperty for observing the specified list
	 * properties in sequence
	 * 
	 * @param properties
	 *            the list properties
	 */
	public MultiListProperty(IListProperty<S, E>[] properties) {
		this(properties, null);
	}

	/**
	 * Constructs a MultiListProperty for observing the specified list
	 * properties in sequence.
	 * 
	 * @param properties
	 *            the list properties
	 * @param elementType
	 *            the element type of the MultiListProperty
	 */
	public MultiListProperty(IListProperty<S, E>[] properties,
			Object elementType) {
		this.properties = properties;
		this.elementType = elementType;
	}

	public Object getElementType() {
		return elementType;
	}

	protected List<E> doGetList(S source) {
		List<E> list = new ArrayList<E>();
		for (int i = 0; i < properties.length; i++)
			list.addAll(properties[i].getList(source));
		return list;
	}

	protected void doUpdateList(final S source, ListDiff<E> diff) {
		diff.accept(new ListDiffVisitor<E>() {
			public void handleAdd(int index, E element) {
				throw new UnsupportedOperationException();
			}

			public void handleMove(int oldIndex, int newIndex, E element) {
				throw new UnsupportedOperationException();
			}

			public void handleReplace(int index, E oldElement,
					E newElement) {
				int offset = 0;
				for (int i = 0; i < properties.length; i++) {
					List<E> subList = properties[i].getList(source);
					if (index - offset < subList.size()) {
						int subListIndex = index - offset;
						ListDiffEntry<E>[] entries = ListDiffEntry.<E>newArray(2);
						entries[0] = Diffs.createListDiffEntry(subListIndex, false,
								oldElement);
						entries[1] = Diffs.createListDiffEntry(subListIndex, true,
								newElement);
						ListDiff<E> diff = Diffs.createListDiff(entries);
						properties[i].updateList(source, diff);
						return;
					}
					offset += subList.size();
				}
				throw new IndexOutOfBoundsException("index: " + index //$NON-NLS-1$
						+ ", size: " + offset); //$NON-NLS-1$
			}

			public void handleRemove(int index, E element) {
				int offset = 0;
				for (int i = 0; i < properties.length; i++) {
					List<E> subList = properties[i].getList(source);
					int subListIndex = index - offset;
					if (subListIndex < subList.size()) {
						ListDiff<E> diff = Diffs.createListDiff(Diffs
								.createListDiffEntry(subListIndex, false,
										element));
						properties[i].updateList(source, diff);
						return;
					}
					offset += subList.size();
				}
				throw new IndexOutOfBoundsException("index: " + index //$NON-NLS-1$
						+ ", size: " + offset); //$NON-NLS-1$
			}
		});
	}

	public IObservableList<E> observe(Realm realm, S source) {
		IObservableList<E>[] lists = ObservableArrayHelper
				.newIObservableListArray(properties.length);
		for (int i = 0; i < lists.length; i++)
			lists[i] = properties[i].observe(realm, source);
		IObservableList<E> multiList = new MultiList<E>(lists, elementType);

		for (int i = 0; i < lists.length; i++)
			PropertyObservableUtil.cascadeDispose(multiList, lists[i]);

		return multiList;
	}
}
