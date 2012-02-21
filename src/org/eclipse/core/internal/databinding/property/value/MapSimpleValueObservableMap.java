/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 262269, 265561, 262287, 268688, 278550, 303847
 *     Ovidio Mallo - bugs 299619, 301370
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property.value;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.StaleEvent;
import org.eclipse.core.databinding.observable.map.AbstractObservableMap;
import org.eclipse.core.databinding.observable.map.IMapChangeListener;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.MapChangeEvent;
import org.eclipse.core.databinding.observable.map.MapDiff;
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
 * 
 */
public class MapSimpleValueObservableMap<S, K, I extends S, V> extends AbstractObservableMap<K, V> implements
		IPropertyObservable<SimpleValueProperty<S, V>> {
	private IObservableMap<K, I> masterMap;
	private SimpleValueProperty<S, V> detailProperty;

	private IObservableSet<I> knownMasterValues;
	private Map<I, V> cachedValues;
	private Set<I> staleMasterValues;

	private boolean updating = false;

	private IMapChangeListener<K, I> masterListener = new IMapChangeListener<K, I>() {
		public void handleMapChange(final MapChangeEvent<K, I> event) {
			if (!isDisposed()) {
				updateKnownValues();
				if (!updating)
					fireMapChange(convertDiff(event.diff));
			}
		}

		private void updateKnownValues() {
			Set<I> knownValues = new IdentitySet<I>(masterMap.values());
			knownMasterValues.retainAll(knownValues);
			knownMasterValues.addAll(knownValues);
		}

		private MapDiff<K, V> convertDiff(MapDiff<K, I> diff) {
			Map<K, V> oldValues = new IdentityMap<K, V>();
			Map<K, V> newValues = new IdentityMap<K, V>();

			Set<K> addedKeys = diff.getAddedKeys();
			for (Iterator<K> it = addedKeys.iterator(); it.hasNext();) {
				K key = it.next();
				I newSource = diff.getNewValue(key);
				V newValue = detailProperty.getValue(newSource);
				newValues.put(key, newValue);
			}

			Set<K> removedKeys = diff.getRemovedKeys();
			for (Iterator<K> it = removedKeys.iterator(); it.hasNext();) {
				K key = it.next();
				I oldSource = diff.getOldValue(key);
				V oldValue = detailProperty.getValue(oldSource);
				oldValues.put(key, oldValue);
			}

			Set<K> changedKeys = new IdentitySet<K>(diff.getChangedKeys());
			for (Iterator<K> it = changedKeys.iterator(); it.hasNext();) {
				K key = it.next();

				I oldSource = diff.getOldValue(key);
				I newSource = diff.getNewValue(key);

				V oldValue = detailProperty.getValue(oldSource);
				V newValue = detailProperty.getValue(newSource);

				if (Util.equals(oldValue, newValue)) {
					it.remove();
				} else {
					oldValues.put(key, oldValue);
					newValues.put(key, newValue);
				}
			}

			return Diffs.createMapDiff(addedKeys, removedKeys, changedKeys,
					oldValues, newValues);
		}
	};

	private IStaleListener staleListener = new IStaleListener() {
		public void handleStale(StaleEvent staleEvent) {
			fireStale();
		}
	};

	private INativePropertyListener<S> detailListener;

	/**
	 * @param map
	 * @param valueProperty
	 */
	public MapSimpleValueObservableMap(IObservableMap<K, I> map,
			SimpleValueProperty<S, V> valueProperty) {
		super(map.getRealm());
		this.masterMap = map;
		this.detailProperty = valueProperty;

		ISimplePropertyListener<ValueDiff<V>> listener = new ISimplePropertyListener<ValueDiff<V>>() {
			public void handleEvent(
					final SimplePropertyEvent<ValueDiff<V>> event) {
				if (!isDisposed() && !updating) {
					getRealm().exec(new Runnable() {
						public void run() {
							// TODO should we type the source, too?
							@SuppressWarnings("unchecked") I source = (I) event
									.getSource();
							if (event.type == SimplePropertyEvent.CHANGE) {
								notifyIfChanged(source);
							} else if (event.type == SimplePropertyEvent.STALE) {
								boolean wasStale = !staleMasterValues.isEmpty();
								staleMasterValues.add(source);
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

	public Object getKeyType() {
		return masterMap.getKeyType();
	}

	public Object getValueType() {
		return detailProperty.getValueType();
	}

	protected void firstListenerAdded() {
		ObservableTracker.setIgnore(true);
		try {
			knownMasterValues = new IdentityObservableSet<I>(getRealm(), null);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		cachedValues = new IdentityMap<I, V>();
		staleMasterValues = new IdentitySet<I>();
		knownMasterValues.addSetChangeListener(new ISetChangeListener<I>() {
			public void handleSetChange(SetChangeEvent<I> event) {
				for (Iterator<I> it = event.diff.getRemovals().iterator(); it
						.hasNext();) {
					I key = it.next();
					if (detailListener != null)
						detailListener.removeFrom(key);
					cachedValues.remove(key);
					staleMasterValues.remove(key);
				}
				for (Iterator<I> it = event.diff.getAdditions().iterator(); it
						.hasNext();) {
					I key = it.next();
					cachedValues.put(key, detailProperty.getValue(key));
					if (detailListener != null)
						detailListener.addTo(key);
				}
			}
		});

		getRealm().exec(new Runnable() {
			public void run() {
				knownMasterValues.addAll(masterMap.values());

				masterMap.addMapChangeListener(masterListener);
				masterMap.addStaleListener(staleListener);
			}
		});
	}

	protected void lastListenerRemoved() {
		masterMap.removeMapChangeListener(masterListener);
		masterMap.removeStaleListener(staleListener);
		if (knownMasterValues != null) {
			knownMasterValues.dispose();
			knownMasterValues = null;
		}
		cachedValues.clear();
		cachedValues = null;
		staleMasterValues.clear();
		staleMasterValues = null;
	}

	private Set<Map.Entry<K, V>> entrySet;

	public Set<Map.Entry<K, V>> entrySet() {
		getterCalled();
		if (entrySet == null)
			entrySet = new EntrySet();
		return entrySet;
	}

	class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		public Iterator<Map.Entry<K, V>> iterator() {
			return new Iterator<Map.Entry<K, V>>() {
				Iterator<Map.Entry<K, I>> it = masterMap.entrySet().iterator();

				public boolean hasNext() {
					getterCalled();
					return it.hasNext();
				}

				public Map.Entry<K, V> next() {
					getterCalled();
					Map.Entry<K, I> next = it.next();
					return new MapEntry(next.getKey());
				}

				public void remove() {
					it.remove();
				}
			};
		}

		public int size() {
			return masterMap.size();
		}
	}

	class MapEntry implements Map.Entry<K, V> {
		private K key;

		MapEntry(K key) {
			this.key = key;
		}

		public K getKey() {
			getterCalled();
			return key;
		}

		public V getValue() {
			getterCalled();
			if (!masterMap.containsKey(key))
				return null;
			return detailProperty.getValue(masterMap.get(key));
		}

		public V setValue(V value) {
			if (!masterMap.containsKey(key))
				return null;
			I source = masterMap.get(key);

			V oldValue = detailProperty.getValue(source);

			updating = true;
			try {
				detailProperty.setValue(source, value);
			} finally {
				updating = false;
			}

			notifyIfChanged(source);

			return oldValue;
		}

		public boolean equals(Object o) {
			getterCalled();
			if (o == this)
				return true;
			if (o == null)
				return false;
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> that = (Map.Entry<?, ?>) o;
			return Util.equals(this.getKey(), that.getKey())
					&& Util.equals(this.getValue(), that.getValue());
		}

		public int hashCode() {
			getterCalled();
			Object value = getValue();
			return (key == null ? 0 : key.hashCode())
					^ (value == null ? 0 : value.hashCode());
		}
	}

	public boolean containsKey(Object key) {
		getterCalled();

		return masterMap.containsKey(key);
	}

	public V get(Object key) {
		getterCalled();

		return detailProperty.getValue(masterMap.get(key));
	}

	public V put(K key, V value) {
		if (!masterMap.containsKey(key))
			return null;
		I masterValue = masterMap.get(key);
		V oldValue = detailProperty.getValue(masterValue);
		detailProperty.setValue(masterValue, value);
		notifyIfChanged(masterValue);
		return oldValue;
	}

	public V remove(Object key) {
		checkRealm();

		I masterValue = masterMap.get(key);
		V oldValue = detailProperty.getValue(masterValue);

		masterMap.remove(key);

		return oldValue;
	}

	private void notifyIfChanged(I masterValue) {
		if (cachedValues != null) {
			final Set<K> keys = keysFor(masterValue);

			final V oldValue = cachedValues.get(masterValue);
			final V newValue = detailProperty.getValue(masterValue);

			if (!Util.equals(oldValue, newValue)
					|| staleMasterValues.contains(masterValue)) {
				cachedValues.put(masterValue, newValue);
				staleMasterValues.remove(masterValue);
				fireMapChange(new MapDiff<K, V>() {
					public Set<K> getAddedKeys() {
						return Collections.emptySet();
					}

					public Set<K> getChangedKeys() {
						return keys;
					}

					public Set<K> getRemovedKeys() {
						return Collections.emptySet();
					}

					public V getNewValue(Object key) {
						return newValue;
					}

					public V getOldValue(Object key) {
						return oldValue;
					}
				});
			}
		}
	}

	private Set<K> keysFor(I value) {
		Set<K> keys = new IdentitySet<K>();

		for (Map.Entry<K, I> entry : masterMap.entrySet()) {
			if (entry.getValue() == value) {
				keys.add(entry.getKey());
			}
		}

		return keys;
	}

	public boolean isStale() {
		getterCalled();
		return masterMap.isStale() || staleMasterValues != null
				&& !staleMasterValues.isEmpty();
	}

	private void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	public Object getObserved() {
		return masterMap;
	}

	public SimpleValueProperty<S, V> getProperty() {
		return detailProperty;
	}

	public synchronized void dispose() {
		if (masterMap != null) {
			masterMap.removeMapChangeListener(masterListener);
			masterMap = null;
		}
		if (knownMasterValues != null) {
			knownMasterValues.clear(); // detaches listeners
			knownMasterValues.dispose();
			knownMasterValues = null;
		}

		masterListener = null;
		detailListener = null;
		detailProperty = null;
		cachedValues = null;
		staleMasterValues = null;

		super.dispose();
	}
}
