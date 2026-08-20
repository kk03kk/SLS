/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedSet;
import com.badlogic.gdx.utils.Pools;
import java.util.Iterator;

public class Selection<T>
implements Disableable,
Iterable<T> {
    private Actor actor;
    final OrderedSet<T> selected = new OrderedSet();
    private final OrderedSet<T> old = new OrderedSet();
    boolean isDisabled;
    private boolean toggle;
    boolean multiple;
    boolean required;
    private boolean programmaticChangeEvents = true;
    T lastSelected;

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public void choose(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }
        if (this.isDisabled) {
            return;
        }
        this.snapshot();
        try {
            if ((this.toggle || !this.required && this.selected.size == 1 || UIUtils.ctrl()) && this.selected.contains(item)) {
                if (this.required && this.selected.size == 1) {
                    return;
                }
                this.selected.remove(item);
                this.lastSelected = null;
            } else {
                boolean modified = false;
                if (!this.multiple || !this.toggle && !UIUtils.ctrl()) {
                    if (this.selected.size == 1 && this.selected.contains(item)) {
                        return;
                    }
                    modified = this.selected.size > 0;
                    this.selected.clear();
                }
                if (!this.selected.add(item) && !modified) {
                    return;
                }
                this.lastSelected = item;
            }
            if (this.fireChangeEvent()) {
                this.revert();
            } else {
                this.changed();
            }
        }
        finally {
            this.cleanup();
        }
    }

    public boolean hasItems() {
        return this.selected.size > 0;
    }

    public boolean isEmpty() {
        return this.selected.size == 0;
    }

    public int size() {
        return this.selected.size;
    }

    public OrderedSet<T> items() {
        return this.selected;
    }

    public T first() {
        return this.selected.size == 0 ? null : (T)this.selected.first();
    }

    void snapshot() {
        this.old.clear();
        this.old.addAll(this.selected);
    }

    void revert() {
        this.selected.clear();
        this.selected.addAll(this.old);
    }

    void cleanup() {
        this.old.clear(32);
    }

    public void set(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }
        if (this.selected.size == 1 && this.selected.first() == item) {
            return;
        }
        this.snapshot();
        this.selected.clear();
        this.selected.add(item);
        if (this.programmaticChangeEvents && this.fireChangeEvent()) {
            this.revert();
        } else {
            this.lastSelected = item;
            this.changed();
        }
        this.cleanup();
    }

    public void setAll(Array<T> items) {
        boolean added = false;
        this.snapshot();
        this.lastSelected = null;
        this.selected.clear();
        int n = items.size;
        for (int i = 0; i < n; ++i) {
            T item = items.get(i);
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null.");
            }
            if (!this.selected.add(item)) continue;
            added = true;
        }
        if (added) {
            if (this.programmaticChangeEvents && this.fireChangeEvent()) {
                this.revert();
            } else if (items.size > 0) {
                this.lastSelected = items.peek();
                this.changed();
            }
        }
        this.cleanup();
    }

    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }
        if (!this.selected.add(item)) {
            return;
        }
        if (this.programmaticChangeEvents && this.fireChangeEvent()) {
            this.selected.remove(item);
        } else {
            this.lastSelected = item;
            this.changed();
        }
    }

    public void addAll(Array<T> items) {
        boolean added = false;
        this.snapshot();
        int n = items.size;
        for (int i = 0; i < n; ++i) {
            T item = items.get(i);
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null.");
            }
            if (!this.selected.add(item)) continue;
            added = true;
        }
        if (added) {
            if (this.programmaticChangeEvents && this.fireChangeEvent()) {
                this.revert();
            } else {
                this.lastSelected = items.peek();
                this.changed();
            }
        }
        this.cleanup();
    }

    public void remove(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }
        if (!this.selected.remove(item)) {
            return;
        }
        if (this.programmaticChangeEvents && this.fireChangeEvent()) {
            this.selected.add(item);
        } else {
            this.lastSelected = null;
            this.changed();
        }
    }

    public void removeAll(Array<T> items) {
        boolean removed = false;
        this.snapshot();
        int n = items.size;
        for (int i = 0; i < n; ++i) {
            T item = items.get(i);
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null.");
            }
            if (!this.selected.remove(item)) continue;
            removed = true;
        }
        if (removed) {
            if (this.programmaticChangeEvents && this.fireChangeEvent()) {
                this.revert();
            } else {
                this.lastSelected = null;
                this.changed();
            }
        }
        this.cleanup();
    }

    public void clear() {
        if (this.selected.size == 0) {
            return;
        }
        this.snapshot();
        this.selected.clear();
        if (this.programmaticChangeEvents && this.fireChangeEvent()) {
            this.revert();
        } else {
            this.lastSelected = null;
            this.changed();
        }
        this.cleanup();
    }

    protected void changed() {
    }

    public boolean fireChangeEvent() {
        if (this.actor == null) {
            return false;
        }
        ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
        try {
            boolean bl = this.actor.fire(changeEvent);
            return bl;
        }
        finally {
            Pools.free(changeEvent);
        }
    }

    public boolean contains(T item) {
        if (item == null) {
            return false;
        }
        return this.selected.contains(item);
    }

    public T getLastSelected() {
        if (this.lastSelected != null) {
            return this.lastSelected;
        }
        if (this.selected.size > 0) {
            return this.selected.first();
        }
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return this.selected.iterator();
    }

    public Array<T> toArray() {
        return this.selected.iterator().toArray();
    }

    public Array<T> toArray(Array<T> array) {
        return this.selected.iterator().toArray(array);
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    @Override
    public boolean isDisabled() {
        return this.isDisabled;
    }

    public boolean getToggle() {
        return this.toggle;
    }

    public void setToggle(boolean toggle) {
        this.toggle = toggle;
    }

    public boolean getMultiple() {
        return this.multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean getRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
        this.programmaticChangeEvents = programmaticChangeEvents;
    }

    public String toString() {
        return this.selected.toString();
    }
}

