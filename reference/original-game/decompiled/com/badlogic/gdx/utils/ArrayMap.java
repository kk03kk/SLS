/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayMap<K, V>
implements Iterable<ObjectMap.Entry<K, V>> {
    public K[] keys;
    public V[] values;
    public int size;
    public boolean ordered;
    private Entries entries1;
    private Entries entries2;
    private Values valuesIter1;
    private Values valuesIter2;
    private Keys keysIter1;
    private Keys keysIter2;

    public ArrayMap() {
        this(true, 16);
    }

    public ArrayMap(int capacity) {
        this(true, capacity);
    }

    public ArrayMap(boolean ordered, int capacity) {
        this.ordered = ordered;
        this.keys = new Object[capacity];
        this.values = new Object[capacity];
    }

    public ArrayMap(boolean ordered, int capacity, Class keyArrayType, Class valueArrayType) {
        this.ordered = ordered;
        this.keys = (Object[])ArrayReflection.newInstance(keyArrayType, capacity);
        this.values = (Object[])ArrayReflection.newInstance(valueArrayType, capacity);
    }

    public ArrayMap(Class keyArrayType, Class valueArrayType) {
        this(false, 16, keyArrayType, valueArrayType);
    }

    public ArrayMap(ArrayMap array) {
        this(array.ordered, array.size, array.keys.getClass().getComponentType(), array.values.getClass().getComponentType());
        this.size = array.size;
        System.arraycopy(array.keys, 0, this.keys, 0, this.size);
        System.arraycopy(array.values, 0, this.values, 0, this.size);
    }

    public int put(K key, V value) {
        int index = this.indexOfKey(key);
        if (index == -1) {
            if (this.size == this.keys.length) {
                this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
            }
            index = this.size++;
        }
        this.keys[index] = key;
        this.values[index] = value;
        return index;
    }

    public int put(K key, V value, int index) {
        int existingIndex = this.indexOfKey(key);
        if (existingIndex != -1) {
            this.removeIndex(existingIndex);
        } else if (this.size == this.keys.length) {
            this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
        }
        System.arraycopy(this.keys, index, this.keys, index + 1, this.size - index);
        System.arraycopy(this.values, index, this.values, index + 1, this.size - index);
        this.keys[index] = key;
        this.values[index] = value;
        ++this.size;
        return index;
    }

    public void putAll(ArrayMap map) {
        this.putAll(map, 0, map.size);
    }

    public void putAll(ArrayMap map, int offset, int length) {
        if (offset + length > map.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + map.size);
        }
        int sizeNeeded = this.size + length - offset;
        if (sizeNeeded >= this.keys.length) {
            this.resize(Math.max(8, (int)((float)sizeNeeded * 1.75f)));
        }
        System.arraycopy(map.keys, offset, this.keys, this.size, length);
        System.arraycopy(map.values, offset, this.values, this.size, length);
        this.size += length;
    }

    public V get(K key) {
        int i;
        K[] keys = this.keys;
        if (key == null) {
            for (i = this.size - 1; i >= 0; --i) {
                if (keys[i] != key) continue;
                return this.values[i];
            }
        } else {
            while (i >= 0) {
                if (key.equals(keys[i])) {
                    return this.values[i];
                }
                --i;
            }
        }
        return null;
    }

    public K getKey(V value, boolean identity) {
        int i;
        V[] values = this.values;
        if (identity || value == null) {
            for (i = this.size - 1; i >= 0; --i) {
                if (values[i] != value) continue;
                return this.keys[i];
            }
        } else {
            while (i >= 0) {
                if (value.equals(values[i])) {
                    return this.keys[i];
                }
                --i;
            }
        }
        return null;
    }

    public K getKeyAt(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        return this.keys[index];
    }

    public V getValueAt(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        return this.values[index];
    }

    public K firstKey() {
        if (this.size == 0) {
            throw new IllegalStateException("Map is empty.");
        }
        return this.keys[0];
    }

    public V firstValue() {
        if (this.size == 0) {
            throw new IllegalStateException("Map is empty.");
        }
        return this.values[0];
    }

    public void setKey(int index, K key) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        this.keys[index] = key;
    }

    public void setValue(int index, V value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        this.values[index] = value;
    }

    public void insert(int index, K key, V value) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        if (this.size == this.keys.length) {
            this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
        }
        if (this.ordered) {
            System.arraycopy(this.keys, index, this.keys, index + 1, this.size - index);
            System.arraycopy(this.values, index, this.values, index + 1, this.size - index);
        } else {
            this.keys[this.size] = this.keys[index];
            this.values[this.size] = this.values[index];
        }
        ++this.size;
        this.keys[index] = key;
        this.values[index] = value;
    }

    public boolean containsKey(K key) {
        K[] keys = this.keys;
        int i = this.size - 1;
        if (key == null) {
            while (i >= 0) {
                if (keys[i--] != key) continue;
                return true;
            }
        } else {
            while (i >= 0) {
                if (!key.equals(keys[i--])) continue;
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(V value, boolean identity) {
        V[] values = this.values;
        int i = this.size - 1;
        if (identity || value == null) {
            while (i >= 0) {
                if (values[i--] != value) continue;
                return true;
            }
        } else {
            while (i >= 0) {
                if (!value.equals(values[i--])) continue;
                return true;
            }
        }
        return false;
    }

    public int indexOfKey(K key) {
        K[] keys = this.keys;
        if (key == null) {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (keys[i] != key) continue;
                return i;
            }
        } else {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (!key.equals(keys[i])) continue;
                return i;
            }
        }
        return -1;
    }

    public int indexOfValue(V value, boolean identity) {
        V[] values = this.values;
        if (identity || value == null) {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (values[i] != value) continue;
                return i;
            }
        } else {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (!value.equals(values[i])) continue;
                return i;
            }
        }
        return -1;
    }

    public V removeKey(K key) {
        K[] keys = this.keys;
        if (key == null) {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (keys[i] != key) continue;
                V value = this.values[i];
                this.removeIndex(i);
                return value;
            }
        } else {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (!key.equals(keys[i])) continue;
                V value = this.values[i];
                this.removeIndex(i);
                return value;
            }
        }
        return null;
    }

    public boolean removeValue(V value, boolean identity) {
        V[] values = this.values;
        if (identity || value == null) {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (values[i] != value) continue;
                this.removeIndex(i);
                return true;
            }
        } else {
            int n = this.size;
            for (int i = 0; i < n; ++i) {
                if (!value.equals(values[i])) continue;
                this.removeIndex(i);
                return true;
            }
        }
        return false;
    }

    public void removeIndex(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        K[] keys = this.keys;
        --this.size;
        if (this.ordered) {
            System.arraycopy(keys, index + 1, keys, index, this.size - index);
            System.arraycopy(this.values, index + 1, this.values, index, this.size - index);
        } else {
            keys[index] = keys[this.size];
            this.values[index] = this.values[this.size];
        }
        keys[this.size] = null;
        this.values[this.size] = null;
    }

    public K peekKey() {
        return this.keys[this.size - 1];
    }

    public V peekValue() {
        return this.values[this.size - 1];
    }

    public void clear(int maximumCapacity) {
        if (this.keys.length <= maximumCapacity) {
            this.clear();
            return;
        }
        this.size = 0;
        this.resize(maximumCapacity);
    }

    public void clear() {
        K[] keys = this.keys;
        V[] values = this.values;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            keys[i] = null;
            values[i] = null;
        }
        this.size = 0;
    }

    public void shrink() {
        if (this.keys.length == this.size) {
            return;
        }
        this.resize(this.size);
    }

    public void ensureCapacity(int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded >= this.keys.length) {
            this.resize(Math.max(8, sizeNeeded));
        }
    }

    protected void resize(int newSize) {
        Object[] newKeys = (Object[])ArrayReflection.newInstance(this.keys.getClass().getComponentType(), newSize);
        System.arraycopy(this.keys, 0, newKeys, 0, Math.min(this.size, newKeys.length));
        this.keys = newKeys;
        Object[] newValues = (Object[])ArrayReflection.newInstance(this.values.getClass().getComponentType(), newSize);
        System.arraycopy(this.values, 0, newValues, 0, Math.min(this.size, newValues.length));
        this.values = newValues;
    }

    public void reverse() {
        int lastIndex = this.size - 1;
        int n = this.size / 2;
        for (int i = 0; i < n; ++i) {
            int ii = lastIndex - i;
            K tempKey = this.keys[i];
            this.keys[i] = this.keys[ii];
            this.keys[ii] = tempKey;
            V tempValue = this.values[i];
            this.values[i] = this.values[ii];
            this.values[ii] = tempValue;
        }
    }

    public void shuffle() {
        for (int i = this.size - 1; i >= 0; --i) {
            int ii = MathUtils.random(i);
            K tempKey = this.keys[i];
            this.keys[i] = this.keys[ii];
            this.keys[ii] = tempKey;
            V tempValue = this.values[i];
            this.values[i] = this.values[ii];
            this.values[ii] = tempValue;
        }
    }

    public void truncate(int newSize) {
        if (this.size <= newSize) {
            return;
        }
        for (int i = newSize; i < this.size; ++i) {
            this.keys[i] = null;
            this.values[i] = null;
        }
        this.size = newSize;
    }

    public int hashCode() {
        K[] keys = this.keys;
        V[] values = this.values;
        int h = 0;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            K key = keys[i];
            V value = values[i];
            if (key != null) {
                h += key.hashCode() * 31;
            }
            if (value == null) continue;
            h += value.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ArrayMap)) {
            return false;
        }
        ArrayMap other = (ArrayMap)obj;
        if (other.size != this.size) {
            return false;
        }
        K[] keys = this.keys;
        V[] values = this.values;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            K key = keys[i];
            V value = values[i];
            if (!(value == null ? !other.containsKey(key) || other.get(key) != null : !value.equals(other.get(key)))) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        if (this.size == 0) {
            return "{}";
        }
        K[] keys = this.keys;
        V[] values = this.values;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        buffer.append(keys[0]);
        buffer.append('=');
        buffer.append(values[0]);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(", ");
            buffer.append(keys[i]);
            buffer.append('=');
            buffer.append(values[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public Iterator<ObjectMap.Entry<K, V>> iterator() {
        return this.entries();
    }

    public Entries<K, V> entries() {
        if (this.entries1 == null) {
            this.entries1 = new Entries(this);
            this.entries2 = new Entries(this);
        }
        if (!this.entries1.valid) {
            this.entries1.index = 0;
            this.entries1.valid = true;
            this.entries2.valid = false;
            return this.entries1;
        }
        this.entries2.index = 0;
        this.entries2.valid = true;
        this.entries1.valid = false;
        return this.entries2;
    }

    public Values<V> values() {
        if (this.valuesIter1 == null) {
            this.valuesIter1 = new Values(this);
            this.valuesIter2 = new Values(this);
        }
        if (!this.valuesIter1.valid) {
            this.valuesIter1.index = 0;
            this.valuesIter1.valid = true;
            this.valuesIter2.valid = false;
            return this.valuesIter1;
        }
        this.valuesIter2.index = 0;
        this.valuesIter2.valid = true;
        this.valuesIter1.valid = false;
        return this.valuesIter2;
    }

    public Keys<K> keys() {
        if (this.keysIter1 == null) {
            this.keysIter1 = new Keys(this);
            this.keysIter2 = new Keys(this);
        }
        if (!this.keysIter1.valid) {
            this.keysIter1.index = 0;
            this.keysIter1.valid = true;
            this.keysIter2.valid = false;
            return this.keysIter1;
        }
        this.keysIter2.index = 0;
        this.keysIter2.valid = true;
        this.keysIter1.valid = false;
        return this.keysIter2;
    }

    public static class Keys<K>
    implements Iterable<K>,
    Iterator<K> {
        private final ArrayMap<K, Object> map;
        int index;
        boolean valid = true;

        public Keys(ArrayMap<K, Object> map) {
            this.map = map;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.index < this.map.size;
        }

        @Override
        public Iterator<K> iterator() {
            return this;
        }

        @Override
        public K next() {
            if (this.index >= this.map.size) {
                throw new NoSuchElementException(String.valueOf(this.index));
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.map.keys[this.index++];
        }

        @Override
        public void remove() {
            --this.index;
            this.map.removeIndex(this.index);
        }

        public void reset() {
            this.index = 0;
        }

        public Array<K> toArray() {
            return new Array(true, this.map.keys, this.index, this.map.size - this.index);
        }

        public Array<K> toArray(Array array) {
            array.addAll(this.map.keys, this.index, this.map.size - this.index);
            return array;
        }
    }

    public static class Values<V>
    implements Iterable<V>,
    Iterator<V> {
        private final ArrayMap<Object, V> map;
        int index;
        boolean valid = true;

        public Values(ArrayMap<Object, V> map) {
            this.map = map;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.index < this.map.size;
        }

        @Override
        public Iterator<V> iterator() {
            return this;
        }

        @Override
        public V next() {
            if (this.index >= this.map.size) {
                throw new NoSuchElementException(String.valueOf(this.index));
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.map.values[this.index++];
        }

        @Override
        public void remove() {
            --this.index;
            this.map.removeIndex(this.index);
        }

        public void reset() {
            this.index = 0;
        }

        public Array<V> toArray() {
            return new Array(true, this.map.values, this.index, this.map.size - this.index);
        }

        public Array<V> toArray(Array array) {
            array.addAll(this.map.values, this.index, this.map.size - this.index);
            return array;
        }
    }

    public static class Entries<K, V>
    implements Iterable<ObjectMap.Entry<K, V>>,
    Iterator<ObjectMap.Entry<K, V>> {
        private final ArrayMap<K, V> map;
        ObjectMap.Entry<K, V> entry = new ObjectMap.Entry();
        int index;
        boolean valid = true;

        public Entries(ArrayMap<K, V> map) {
            this.map = map;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.index < this.map.size;
        }

        @Override
        public Iterator<ObjectMap.Entry<K, V>> iterator() {
            return this;
        }

        @Override
        public ObjectMap.Entry<K, V> next() {
            if (this.index >= this.map.size) {
                throw new NoSuchElementException(String.valueOf(this.index));
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            this.entry.key = this.map.keys[this.index];
            this.entry.value = this.map.values[this.index++];
            return this.entry;
        }

        @Override
        public void remove() {
            --this.index;
            this.map.removeIndex(this.index);
        }

        public void reset() {
            this.index = 0;
        }
    }
}

