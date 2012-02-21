/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 262269, 265561, 262287, 268688, 278550
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property.value;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.StaleEvent;
import org.eclipse.core.databinding.observable.list.AbstractObservableList;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.IPropertyObservable;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.databinding.property.SimplePropertyEvent;
import org.eclipse.core.databinding.property.value.SimpleValueProperty;
import org.eclipse.core.internal.databinding.identity.IdentityMap;
import org.eclipse.core.internal.databinding.identity.IdentityObservableSet;
import org.eclipse.core.internal.databinding.identity.IdentitySet;
import org.eclipse.core.internal.databinding.property.Util;

/**
 * @since 1.2
 */
public class ListSimpleValueObservableList<S, U extends S, E> extends
AbstractObservableList<E> implements
IPropertyObservable<SimpleValueProperty<S, E>> {
private IObservableList<U> masterList;
private SimpleValueProperty<S, E> detailProperty;

private IObservableSet<U> knownMasterElements;
private Map<U, E> cachedValues;
private Set<U> staleElements;

private boolean updating;

private IListChangeListener<U> masterListener = new IListChangeListener<U>() {
public void handleListChange(ListChangeEvent<U> event) {
			if (!isDisposed()) {
				updateKnownElements();
				fireListChange(convertDiff(event.diff));
			}
		}

		private void updateKnownElements() {
			Set<U> identityKnownElements = new IdentitySet<U>(masterList);
			knownMasterElements.retainAll(identityKnownElements);
			knownMasterElements.addAll(identityKnownElements);
		}

		private ListDiff<E> convertDiff(ListDiff<U> diff) {
			// Convert diff to detail value
			ListDiffEntry<U>[] masterEntries = diff.getDifferences();
			ListDiffEntry<E>[] detailEntries = ListDiffEntry
					.newArray(masterEntries.length);
			for (int i = 0; i < masterEntries.length; i++) {
				ListDiffEntry<U> masterDifference = masterEntries[i];
				int index = masterDifference.getPosition();
				boolean addition = masterDifference.isAddition();
				U masterElement = masterDifference.getElement();
				E elementDetailValue = detailProperty
						.getValue(masterElement);
				detailEntries[i] = Diffs.createListDiffEntry(index, addition,
						elementDetailValue);
			}
			return Diffs.createListDiff(detailEntries);
		}
	};

	private IStaleListener staleListener = new IStaleListener() {
		public void handleStale(StaleEvent staleEvent) {
			fireStale();
		}
	};

	private INativePropertyListener<S> detailListener;

	/**
	 * @param masterList
	 * @param valueProperty
	 */
	public ListSimpleValueObservableList(IObservableList<U> masterList,
			SimpleValueProperty<S, E> valueProperty) {
		super(masterList.getRealm());
		this.masterList = masterList;
		this.detailProperty = valueProperty;

		ISimplePropertyListener<ValueDiff<E>> listener = new ISimplePropertyListener<ValueDiff<E>>() {
			public void handleEvent(
					final SimplePropertyEvent<ValueDiff<E>> event) {
				if (!isDisposed() && !updating) {
					getRealm().exec(new Runnable() {
						public void run() {
							@SuppressWarnings("unchecked") U source = (U) event
									.getSource();
							if (event.type == SimplePropertyEvent.CHANGE) {
								notifyIfChanged(source);
							} else if (event.type == SimplePropertyEvent.STALE) {
								boolean wasStale = !staleElements.isEmpty();
								staleElements.add(source);
								if (!wasStale)
									fireStale();
							}
						}
					});
				}
			}
		};
		this.detailListener = detailProperty.adaptListener(listener);
	}

	protected void firstListenerAdded() {
		ObservableTracker.setIgnore(true);
		try {
			knownMasterElements = new IdentityObservableSet<U>(getRealm(), null);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		cachedValues = new IdentityMap<U, E>();
		staleElements = new IdentitySet<U>();
		knownMasterElements.addSetChangeListener(new ISetChangeListener<U>() {
			public void handleSetChange(SetChangeEvent<U> event) {
				for (Iterator<U> it = event.diff.getRemovals().iterator(); it
						.hasNext();) {
					U key = it.next();
					if (detailListener != null)
						detailListener.removeFrom(key);
					cachedValues.remove(key);
					staleElements.remove(key);
				}
				for (Iterator<U> it = event.diff.getAdditions().iterator(); it
						.hasNext();) {
					U key = it.next();
					cachedValues.put(key, detailProperty.getValue(key));
					if (detailListener != null)
						detailListener.addTo(key);
				}
			}
		});
		getRealm().exec(new Runnable() {
			public void run() {
				knownMasterElements.addAll(masterList);

				masterList.addListChangeListener(masterListener);
				masterList.addStaleListener(staleListener);
			}
		});
	}

	protected void lastListenerRemoved() {
		if (masterList != null) {
			masterList.removeListChangeListener(masterListener);
			masterList.removeStaleListener(staleListener);
		}
		if (knownMasterElements != null) {
			knownMasterElements.dispose();
			knownMasterElements = null;
		}
		if (cachedValues != null) {
			cachedValues.clear();
			cachedValues = null;
		}
		if (staleElements != null) {
			staleElements.clear();
			staleElements = null;
		}
	}

	protected int doGetSize() {
		getterCalled();
		return masterList.size();
	}

	private void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	public Object getElementType() {
		return detailProperty.getValueType();
	}

	public E get(int index) {
		getterCalled();
		U masterElement = masterList.get(index);
		return detailProperty.getValue(masterElement);
	}

	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		getterCalled();

		for (Iterator<U> it = masterList.iterator(); it.hasNext();) {
			if (Util.equals(detailProperty.getValue(it.next()), o))
				return true;
		}
		return false;
	}

	public boolean isEmpty() {
		getterCalled();
		return masterList.isEmpty();
	}

	public boolean isStale() {
		getterCalled();
		return masterList.isStale() || staleElements != null
				&& !staleElements.isEmpty();
	}

	public Iterator<E> iterator() {
		getterCalled();
		return new Iterator<E>() {
			Iterator<U> it = masterList.iterator();

			public boolean hasNext() {
				getterCalled();
				return it.hasNext();
			}

			public E next() {
				getterCalled();
				U masterElement = it.next();
				return detailProperty.getValue(masterElement);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public E move(int oldIndex, int newIndex) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray() {
		getterCalled();
		Object[] masterElements = masterList.toArray();
		Object[] result = new Object[masterElements.length];
		for (int i = 0; i < result.length; i++) {
			// cast is always safe as we get the array from a list of Us
			result[i] = detailProperty.getValue((U) masterElements[i]);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <V> V[] toArray(V[] a) {
		getterCalled();
		Object[] masterElements = masterList.toArray();
		if (a.length < masterElements.length)
			a = (V[]) Array.newInstance(a.getClass().getComponentType(),
					masterElements.length);
		for (int i = 0; i < masterElements.length; i++) {
			a[i] = (V) detailProperty.getValue((U) masterElements[i]);
		}
		return a;
	}

	public void add(int index, Object o) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	public ListIterator<E> listIterator(final int index) {
		getterCalled();
		return new ListIterator<E>() {
			ListIterator<U> it = masterList.listIterator(index);
			U lastMasterElement;
			E lastElement;
			boolean haveIterated = false;

			public void add(Object arg0) {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				getterCalled();
				return it.hasNext();
			}

			public boolean hasPrevious() {
				getterCalled();
				return it.hasPrevious();
			}

			public E next() {
				getterCalled();
				lastMasterElement = it.next();
				lastElement = detailProperty.getValue(lastMasterElement);
				haveIterated = true;
				return lastElement;
			}

			public int nextIndex() {
				getterCalled();
				return it.nextIndex();
			}

			public E previous() {
				getterCalled();
				lastMasterElement = it.previous();
				lastElement = detailProperty.getValue(lastMasterElement);
				haveIterated = true;
				return lastElement;
			}

			public int previousIndex() {
				getterCalled();
				return it.previousIndex();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public void set(E o) {
				checkRealm();
				if (!haveIterated)
					throw new IllegalStateException();

				boolean wasUpdating = updating;
				updating = true;
				try {
//					detailProperty.setValue(lastElement, o); // jpp: bug!
					detailProperty.setValue(lastMasterElement, o);
				} finally {
					updating = wasUpdating;
				}

				notifyIfChanged(lastMasterElement);

				lastElement = o;
			}
		};
	}

	private void notifyIfChanged(U masterElement) {
		if (cachedValues != null) {
			E oldValue = cachedValues.get(masterElement);
			E newValue = detailProperty.getValue(masterElement);
			if (!Util.equals(oldValue, newValue)
					|| staleElements.contains(masterElement)) {
				cachedValues.put(masterElement, newValue);
				staleElements.remove(masterElement);
				fireListChange(indicesOf(masterElement), oldValue, newValue);
			}
		}
	}

	private int[] indicesOf(Object masterElement) {
		List<Integer> indices = new ArrayList<Integer>();

		for (ListIterator<U> it = ListSimpleValueObservableList.this.masterList
				.listIterator(); it.hasNext();) {
			if (masterElement == it.next())
				indices.add(new Integer(it.previousIndex()));
		}

		int[] result = new int[indices.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = indices.get(i).intValue();
		}
		return result;
	}

	private void fireListChange(int[] indices, E oldValue, E newValue) {
		ListDiffEntry<E>[] differences = ListDiffEntry
				.newArray(indices.length * 2);
		for (int i = 0; i < indices.length; i++) {
			int index = indices[i];
			differences[i * 2] = Diffs.createListDiffEntry(index, false,
					oldValue);
			differences[i * 2 + 1] = Diffs.createListDiffEntry(index, true,
					newValue);
		}
		fireListChange(Diffs.createListDiff(differences));
	}

	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	public E set(int index, E o) {
		checkRealm();
		U masterElement = masterList.get(index);
		E oldValue = detailProperty.getValue(masterElement);

		boolean wasUpdating = updating;
		updating = true;
		try {
			detailProperty.setValue(masterElement, o);
		} finally {
			updating = wasUpdating;
		}

		notifyIfChanged(masterElement);

		return oldValue;
	}

	public Object getObserved() {
		return masterList;
	}

	public SimpleValueProperty<S, E> getProperty() {
		return detailProperty;
	}

	public synchronized void dispose() {
		if (knownMasterElements != null) {
			knownMasterElements.clear(); // detaches listeners
			knownMasterElements.dispose();
			knownMasterElements = null;
		}

		if (masterList != null) {
			masterList.removeListChangeListener(masterListener);
			masterList = null;
		}

		masterListener = null;
		detailListener = null;
		detailProperty = null;
		cachedValues = null;
		staleElements = null;

		super.dispose();
	}
}