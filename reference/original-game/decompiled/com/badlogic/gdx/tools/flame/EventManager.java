/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class EventManager {
    private static EventManager mInstance;
    private ObjectMap<Integer, Array<Listener>> mListeners = new ObjectMap();

    private EventManager() {
    }

    public static EventManager get() {
        if (mInstance == null) {
            mInstance = new EventManager();
        }
        return mInstance;
    }

    public void attach(int aEventType, Listener aListener) {
        boolean isNew = false;
        Array<Listener> listeners = this.mListeners.get(aEventType);
        if (listeners == null) {
            listeners = new Array();
            this.mListeners.put(aEventType, listeners);
            isNew = true;
        }
        if (isNew || !listeners.contains(aListener, true)) {
            listeners.add(aListener);
        }
    }

    public void detach(int aEventType, Listener aListener) {
        Array<Listener> listeners = this.mListeners.get(aEventType);
        if (listeners != null) {
            listeners.removeValue(aListener, true);
            if (listeners.size == 0) {
                this.mListeners.remove(aEventType);
            }
        }
    }

    public void fire(int aEventType, Object aEventData) {
        Array<Listener> listeners = this.mListeners.get(aEventType);
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.handle(aEventType, aEventData);
            }
        }
    }

    public void clear() {
        this.mListeners.clear();
    }

    public static interface Listener {
        public void handle(int var1, Object var2);
    }
}

