/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.NoSuchElementException;

public class OrderedSet<T>
extends ObjectSet<T> {
    final Array<T> items;
    OrderedSetIterator iterator1;
    OrderedSetIterator iterator2;

    public OrderedSet() {
        this.items = new Array();
    }

    public OrderedSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.items = new Array(this.capacity);
    }

    public OrderedSet(int initialCapacity) {
        super(initialCapacity);
        this.items = new Array(this.capacity);
    }

    public OrderedSet(OrderedSet set) {
        super(set);
        this.items = new Array(this.capacity);
        this.items.addAll(set.items);
    }

    @Override
    public boolean add(T key) {
        if (!super.add(key)) {
            return false;
        }
        this.items.add(key);
        return true;
    }

    public boolean add(T key, int index) {
        if (!super.add(key)) {
            this.items.removeValue(key, true);
            this.items.insert(index, key);
            return false;
        }
        this.items.insert(index, key);
        return true;
    }

    @Override
    public boolean remove(T key) {
        this.items.removeValue(key, false);
        return super.remove(key);
    }

    @Override
    public void clear(int maximumCapacity) {
        this.items.clear();
        super.clear(maximumCapacity);
    }

    @Override
    public void clear() {
        this.items.clear();
        super.clear();
    }

    public Array<T> orderedItems() {
        return this.items;
    }

    @Override
    public OrderedSetIterator<T> iterator() {
        if (this.iterator1 == null) {
            this.iterator1 = new OrderedSetIterator(this);
            this.iterator2 = new OrderedSetIterator(this);
        }
        if (!this.iterator1.valid) {
            this.iterator1.reset();
            this.iterator1.valid = true;
            this.iterator2.valid = false;
            return this.iterator1;
        }
        this.iterator2.reset();
        this.iterator2.valid = true;
        this.iterator1.valid = false;
        return this.iterator2;
    }

    @Override
    public String toString() {
        if (this.size == 0) {
            return "{}";
        }
        T[] items = this.items.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        buffer.append(items[0]);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public String toString(String separator) {
        return this.items.toString(separator);
    }

    public static class OrderedSetIterator<T>
    extends ObjectSet.ObjectSetIterator<T> {
        private Array<T> items;

        public OrderedSetIterator(OrderedSet<T> set) {
            super(set);
            this.items = set.items;
        }

        @Override
        public void reset() {
            this.nextIndex = 0;
            this.hasNext = this.set.size > 0;
        }

        @Override
        public T next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            T key = this.items.get(this.nextIndex);
            ++this.nextIndex;
            this.hasNext = this.nextIndex < this.set.size;
            return key;
        }

        @Override
        public void remove() {
            if (this.nextIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            }
            --this.nextIndex;
            this.set.remove(this.items.get(this.nextIndex));
        }
    }
}

