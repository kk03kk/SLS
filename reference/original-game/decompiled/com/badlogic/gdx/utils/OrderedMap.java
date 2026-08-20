/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.NoSuchElementException;

public class OrderedMap<K, V>
extends ObjectMap<K, V> {
    final Array<K> keys;
    private ObjectMap.Entries entries1;
    private ObjectMap.Entries entries2;
    private ObjectMap.Values values1;
    private ObjectMap.Values values2;
    private ObjectMap.Keys keys1;
    private ObjectMap.Keys keys2;

    public OrderedMap() {
        this.keys = new Array();
    }

    public OrderedMap(int initialCapacity) {
        super(initialCapacity);
        this.keys = new Array(this.capacity);
    }

    public OrderedMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.keys = new Array(this.capacity);
    }

    public OrderedMap(OrderedMap<? extends K, ? extends V> map) {
        super(map);
        this.keys = new Array<K>(map.keys);
    }

    @Override
    public V put(K key, V value) {
        if (!this.containsKey(key)) {
            this.keys.add(key);
        }
        return super.put(key, value);
    }

    @Override
    public V remove(K key) {
        this.keys.removeValue(key, false);
        return super.remove(key);
    }

    @Override
    public void clear(int maximumCapacity) {
        this.keys.clear();
        super.clear(maximumCapacity);
    }

    @Override
    public void clear() {
        this.keys.clear();
        super.clear();
    }

    public Array<K> orderedKeys() {
        return this.keys;
    }

    @Override
    public ObjectMap.Entries<K, V> iterator() {
        return this.entries();
    }

    @Override
    public ObjectMap.Entries<K, V> entries() {
        if (this.entries1 == null) {
            this.entries1 = new OrderedMapEntries(this);
            this.entries2 = new OrderedMapEntries(this);
        }
        if (!this.entries1.valid) {
            this.entries1.reset();
            this.entries1.valid = true;
            this.entries2.valid = false;
            return this.entries1;
        }
        this.entries2.reset();
        this.entries2.valid = true;
        this.entries1.valid = false;
        return this.entries2;
    }

    @Override
    public ObjectMap.Values<V> values() {
        if (this.values1 == null) {
            this.values1 = new OrderedMapValues(this);
            this.values2 = new OrderedMapValues(this);
        }
        if (!this.values1.valid) {
            this.values1.reset();
            this.values1.valid = true;
            this.values2.valid = false;
            return this.values1;
        }
        this.values2.reset();
        this.values2.valid = true;
        this.values1.valid = false;
        return this.values2;
    }

    @Override
    public ObjectMap.Keys<K> keys() {
        if (this.keys1 == null) {
            this.keys1 = new OrderedMapKeys(this);
            this.keys2 = new OrderedMapKeys(this);
        }
        if (!this.keys1.valid) {
            this.keys1.reset();
            this.keys1.valid = true;
            this.keys2.valid = false;
            return this.keys1;
        }
        this.keys2.reset();
        this.keys2.valid = true;
        this.keys1.valid = false;
        return this.keys2;
    }

    @Override
    public String toString() {
        if (this.size == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        Array<K> keys = this.keys;
        int n = keys.size;
        for (int i = 0; i < n; ++i) {
            K key = keys.get(i);
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(key);
            buffer.append('=');
            buffer.append(this.get(key));
        }
        buffer.append('}');
        return buffer.toString();
    }

    public static class OrderedMapValues<V>
    extends ObjectMap.Values<V> {
        private Array keys;

        public OrderedMapValues(OrderedMap<?, V> map) {
            super(map);
            this.keys = map.keys;
        }

        @Override
        public void reset() {
            this.nextIndex = 0;
            this.hasNext = this.map.size > 0;
        }

        @Override
        public V next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            Object value = this.map.get(this.keys.get(this.nextIndex));
            this.currentIndex = this.nextIndex++;
            this.hasNext = this.nextIndex < this.map.size;
            return value;
        }

        @Override
        public void remove() {
            if (this.currentIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            }
            this.map.remove(this.keys.get(this.currentIndex));
            this.nextIndex = this.currentIndex;
            this.currentIndex = -1;
        }
    }

    public static class OrderedMapKeys<K>
    extends ObjectMap.Keys<K> {
        private Array<K> keys;

        public OrderedMapKeys(OrderedMap<K, ?> map) {
            super(map);
            this.keys = map.keys;
        }

        @Override
        public void reset() {
            this.nextIndex = 0;
            this.hasNext = this.map.size > 0;
        }

        @Override
        public K next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            K key = this.keys.get(this.nextIndex);
            this.currentIndex = this.nextIndex++;
            this.hasNext = this.nextIndex < this.map.size;
            return key;
        }

        @Override
        public void remove() {
            if (this.currentIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            }
            this.map.remove(this.keys.get(this.nextIndex - 1));
            this.nextIndex = this.currentIndex;
            this.currentIndex = -1;
        }
    }

    public static class OrderedMapEntries<K, V>
    extends ObjectMap.Entries<K, V> {
        private Array<K> keys;

        public OrderedMapEntries(OrderedMap<K, V> map) {
            super(map);
            this.keys = map.keys;
        }

        @Override
        public void reset() {
            this.nextIndex = 0;
            this.hasNext = this.map.size > 0;
        }

        @Override
        public ObjectMap.Entry next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            this.entry.key = this.keys.get(this.nextIndex);
            this.entry.value = this.map.get(this.entry.key);
            ++this.nextIndex;
            this.hasNext = this.nextIndex < this.map.size;
            return this.entry;
        }

        @Override
        public void remove() {
            if (this.currentIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            }
            this.map.remove(this.entry.key);
            --this.nextIndex;
        }
    }
}

