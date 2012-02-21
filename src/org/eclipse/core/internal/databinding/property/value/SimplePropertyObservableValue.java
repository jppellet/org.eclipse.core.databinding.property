/*******************************************************************************
 * Copyright (c) 2008, 2009 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bugs 265561, 262287, 268688
 ******************************************************************************/

package org.eclipse.core.internal.databinding.property.value;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.property.INativePropertyListener;
import org.eclipse.core.databinding.property.IPropertyObservable;
import org.eclipse.core.databinding.property.ISimplePropertyListener;
import org.eclipse.core.databinding.property.SimplePropertyEvent;
import org.eclipse.core.databinding.property.value.SimpleValueProperty;
import org.eclipse.core.internal.databinding.property.Util;

/**
 * @since 1.2
 * 
 */
public class SimplePropertyObservableValue<S, T> extends AbstractObservableValue<T> implements
		IPropertyObservable<SimpleValueProperty<S, T>> {
	private S source;
	private SimpleValueProperty<S, T> property;

	private boolean updating = false;
	private T cachedValue;
	private boolean stale;

	private INativePropertyListener<S> listener;

	/**
	 * @param realm
	 * @param source
	 * @param property
	 */
	public SimplePropertyObservableValue(Realm realm, S source, SimpleValueProperty<S, T> property) {
		super(realm);
		this.source = source;
		this.property = property;
	}

	protected void firstListenerAdded() {
		if (!isDisposed()) {
			if (listener == null) {
				listener = property.adaptListener(new ISimplePropertyListener<ValueDiff<T>>() {
					public void handleEvent(final SimplePropertyEvent<ValueDiff<T>> event) {
						if (!isDisposed() && !updating) {
							getRealm().exec(new Runnable() {
								public void run() {
									if (event.type == SimplePropertyEvent.CHANGE) {
										notifyIfChanged(event.diff);
									} else if (event.type == SimplePropertyEvent.STALE && !stale) {
										stale = true;
										fireStale();
									}
								}
							});
						}
					}
				});
			}
			getRealm().exec(new Runnable() {
				public void run() {
					cachedValue = property.getValue(source);
					stale = false;
					if (listener != null)
						listener.addTo(source);
				}
			});
		}
	}

	protected void lastListenerRemoved() {
		if (listener != null)
			listener.removeFrom(source);
		cachedValue = null;
		stale = false;
	}

	protected T doGetValue() {
		notifyIfChanged(null);
		return property.getValue(source);
	}

	protected void doSetValue(T value) {
		updating = true;
		try {
			property.setValue(source, value);
		} finally {
			updating = false;
		}

		notifyIfChanged(null);
	}

	private void notifyIfChanged(ValueDiff<T> diff) {
		if (hasListeners()) {
			T oldValue = cachedValue;
			T newValue = cachedValue = property.getValue(source);
			if (diff == null)
				diff = Diffs.createValueDiff(oldValue, newValue);
			if (!Util.equals(oldValue, newValue) || stale) {
				stale = false;
				fireValueChange(diff);
			}
		}
	}

	public Object getValueType() {
		return property.getValueType();
	}

	public Object getObserved() {
		return source;
	}

	public SimpleValueProperty<S, T> getProperty() {
		return property;
	}

	public boolean isStale() {
		ObservableTracker.getterCalled(this);
		return stale;
	}

	public synchronized void dispose() {
		if (!isDisposed()) {
			if (listener != null)
				listener.removeFrom(source);
			source = null;
			property = null;
			listener = null;
			stale = false;
		}
		super.dispose();
	}
}
