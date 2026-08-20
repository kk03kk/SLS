/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LongMap<V>
implements Iterable<Entry<V>> {
    private static final int PRIME1 = -1105259343;
    private static final int PRIME2 = -1262997959;
    private static final int PRIME3 = -825114047;
    private static final int EMPTY = 0;
    public int size;
    long[] keyTable;
    V[] valueTable;
    int capacity;
    int stashSize;
    V zeroValue;
    boolean hasZeroValue;
    private float loadFactor;
    private int hashShift;
    private int mask;
    private int threshold;
    private int stashCapacity;
    private int pushIterations;
    private Entries entries1;
    private Entries entries2;
    private Values values1;
    private Values values2;
    private Keys keys1;
    private Keys keys2;

    public LongMap() {
        this(51, 0.8f);
    }

    public LongMap(int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    public LongMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
        }
        if ((initialCapacity = MathUtils.nextPowerOfTwo((int)Math.ceil((float)initialCapacity / loadFactor))) > 0x40000000) {
            throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
        }
        this.capacity = initialCapacity;
        if (loadFactor <= 0.0f) {
            throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
        }
        this.loadFactor = loadFactor;
        this.threshold = (int)((float)this.capacity * loadFactor);
        this.mask = this.capacity - 1;
        this.hashShift = 63 - Long.numberOfTrailingZeros(this.capacity);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(this.capacity)) * 2);
        this.pushIterations = Math.max(Math.min(this.capacity, 8), (int)Math.sqrt(this.capacity) / 8);
        this.keyTable = new long[this.capacity + this.stashCapacity];
        this.valueTable = new Object[this.keyTable.length];
    }

    public LongMap(LongMap<? extends V> map) {
        this((int)Math.floor((float)map.capacity * map.loadFactor), map.loadFactor);
        this.stashSize = map.stashSize;
        System.arraycopy(map.keyTable, 0, this.keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, this.valueTable, 0, map.valueTable.length);
        this.size = map.size;
        this.zeroValue = map.zeroValue;
        this.hasZeroValue = map.hasZeroValue;
    }

    public V put(long key, V value) {
        int i;
        if (key == 0L) {
            V oldValue = this.zeroValue;
            this.zeroValue = value;
            if (!this.hasZeroValue) {
                this.hasZeroValue = true;
                ++this.size;
            }
            return oldValue;
        }
        long[] keyTable = this.keyTable;
        int index1 = (int)(key & (long)this.mask);
        long key1 = keyTable[index1];
        if (key1 == key) {
            V oldValue = this.valueTable[index1];
            this.valueTable[index1] = value;
            return oldValue;
        }
        int index2 = this.hash2(key);
        long key2 = keyTable[index2];
        if (key2 == key) {
            V oldValue = this.valueTable[index2];
            this.valueTable[index2] = value;
            return oldValue;
        }
        int index3 = this.hash3(key);
        long key3 = keyTable[index3];
        if (key3 == key) {
            V oldValue = this.valueTable[index3];
            this.valueTable[index3] = value;
            return oldValue;
        }
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (keyTable[i] != key) continue;
            V oldValue = this.valueTable[i];
            this.valueTable[i] = value;
            return oldValue;
        }
        if (key1 == 0L) {
            keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return null;
        }
        if (key2 == 0L) {
            keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return null;
        }
        if (key3 == 0L) {
            keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return null;
        }
        this.push(key, value, index1, key1, index2, key2, index3, key3);
        return null;
    }

    public void putAll(LongMap<V> map) {
        for (Entry<V> entry : map.entries()) {
            this.put(entry.key, entry.value);
        }
    }

    private void putResize(long key, V value) {
        if (key == 0L) {
            this.zeroValue = value;
            this.hasZeroValue = true;
            return;
        }
        int index1 = (int)(key & (long)this.mask);
        long key1 = this.keyTable[index1];
        if (key1 == 0L) {
            this.keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index2 = this.hash2(key);
        long key2 = this.keyTable[index2];
        if (key2 == 0L) {
            this.keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index3 = this.hash3(key);
        long key3 = this.keyTable[index3];
        if (key3 == 0L) {
            this.keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        this.push(key, value, index1, key1, index2, key2, index3, key3);
    }

    private void push(long insertKey, V insertValue, int index1, long key1, int index2, long key2, int index3, long key3) {
        V evictedValue;
        long evictedKey;
        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int mask = this.mask;
        int i = 0;
        int pushIterations = this.pushIterations;
        while (true) {
            switch (MathUtils.random(2)) {
                case 0: {
                    evictedKey = key1;
                    evictedValue = valueTable[index1];
                    keyTable[index1] = insertKey;
                    valueTable[index1] = insertValue;
                    break;
                }
                case 1: {
                    evictedKey = key2;
                    evictedValue = valueTable[index2];
                    keyTable[index2] = insertKey;
                    valueTable[index2] = insertValue;
                    break;
                }
                default: {
                    evictedKey = key3;
                    evictedValue = valueTable[index3];
                    keyTable[index3] = insertKey;
                    valueTable[index3] = insertValue;
                }
            }
            index1 = (int)(evictedKey & (long)mask);
            key1 = keyTable[index1];
            if (key1 == 0L) {
                keyTable[index1] = evictedKey;
                valueTable[index1] = evictedValue;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index2 = this.hash2(evictedKey);
            key2 = keyTable[index2];
            if (key2 == 0L) {
                keyTable[index2] = evictedKey;
                valueTable[index2] = evictedValue;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index3 = this.hash3(evictedKey);
            key3 = keyTable[index3];
            if (key3 == 0L) {
                keyTable[index3] = evictedKey;
                valueTable[index3] = evictedValue;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            if (++i == pushIterations) break;
            insertKey = evictedKey;
            insertValue = evictedValue;
        }
        this.putStash(evictedKey, evictedValue);
    }

    private void putStash(long key, V value) {
        if (this.stashSize == this.stashCapacity) {
            this.resize(this.capacity << 1);
            this.put(key, value);
            return;
        }
        int index = this.capacity + this.stashSize;
        this.keyTable[index] = key;
        this.valueTable[index] = value;
        ++this.stashSize;
        ++this.size;
    }

    public V get(long key) {
        if (key == 0L) {
            if (!this.hasZeroValue) {
                return null;
            }
            return this.zeroValue;
        }
        int index = (int)(key & (long)this.mask);
        if (this.keyTable[index] != key && this.keyTable[index = this.hash2(key)] != key && this.keyTable[index = this.hash3(key)] != key) {
            return this.getStash(key, null);
        }
        return this.valueTable[index];
    }

    public V get(long key, V defaultValue) {
        if (key == 0L) {
            if (!this.hasZeroValue) {
                return defaultValue;
            }
            return this.zeroValue;
        }
        int index = (int)(key & (long)this.mask);
        if (this.keyTable[index] != key && this.keyTable[index = this.hash2(key)] != key && this.keyTable[index = this.hash3(key)] != key) {
            return this.getStash(key, defaultValue);
        }
        return this.valueTable[index];
    }

    private V getStash(long key, V defaultValue) {
        int i;
        long[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (keyTable[i] != key) continue;
            return this.valueTable[i];
        }
        return defaultValue;
    }

    public V remove(long key) {
        if (key == 0L) {
            if (!this.hasZeroValue) {
                return null;
            }
            V oldValue = this.zeroValue;
            this.zeroValue = null;
            this.hasZeroValue = false;
            --this.size;
            return oldValue;
        }
        int index = (int)(key & (long)this.mask);
        if (this.keyTable[index] == key) {
            this.keyTable[index] = 0L;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            --this.size;
            return oldValue;
        }
        index = this.hash2(key);
        if (this.keyTable[index] == key) {
            this.keyTable[index] = 0L;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            --this.size;
            return oldValue;
        }
        index = this.hash3(key);
        if (this.keyTable[index] == key) {
            this.keyTable[index] = 0L;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            --this.size;
            return oldValue;
        }
        return this.removeStash(key);
    }

    V removeStash(long key) {
        int i;
        long[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (keyTable[i] != key) continue;
            V oldValue = this.valueTable[i];
            this.removeStashIndex(i);
            --this.size;
            return oldValue;
        }
        return null;
    }

    void removeStashIndex(int index) {
        --this.stashSize;
        int lastIndex = this.capacity + this.stashSize;
        if (index < lastIndex) {
            this.keyTable[index] = this.keyTable[lastIndex];
            this.valueTable[index] = this.valueTable[lastIndex];
            this.valueTable[lastIndex] = null;
        } else {
            this.valueTable[index] = null;
        }
    }

    public void shrink(int maximumCapacity) {
        if (maximumCapacity < 0) {
            throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        }
        if (this.size > maximumCapacity) {
            maximumCapacity = this.size;
        }
        if (this.capacity <= maximumCapacity) {
            return;
        }
        maximumCapacity = MathUtils.nextPowerOfTwo(maximumCapacity);
        this.resize(maximumCapacity);
    }

    public void clear(int maximumCapacity) {
        if (this.capacity <= maximumCapacity) {
            this.clear();
            return;
        }
        this.zeroValue = null;
        this.hasZeroValue = false;
        this.size = 0;
        this.resize(maximumCapacity);
    }

    public void clear() {
        if (this.size == 0) {
            return;
        }
        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            keyTable[i] = 0L;
            valueTable[i] = null;
        }
        this.size = 0;
        this.stashSize = 0;
        this.zeroValue = null;
        this.hasZeroValue = false;
    }

    public boolean containsValue(Object value, boolean identity) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return true;
            }
            long[] keyTable = this.keyTable;
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (keyTable[i] == 0L || valueTable[i] != null) continue;
                return true;
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return true;
            }
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (valueTable[i] != value) continue;
                return true;
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return true;
            }
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (!value.equals(valueTable[i])) continue;
                return true;
            }
        }
        return false;
    }

    public boolean containsKey(long key) {
        if (key == 0L) {
            return this.hasZeroValue;
        }
        int index = (int)(key & (long)this.mask);
        if (this.keyTable[index] != key && this.keyTable[index = this.hash2(key)] != key && this.keyTable[index = this.hash3(key)] != key) {
            return this.containsKeyStash(key);
        }
        return true;
    }

    private boolean containsKeyStash(long key) {
        int i;
        long[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (keyTable[i] != key) continue;
            return true;
        }
        return false;
    }

    public long findKey(Object value, boolean identity, long notFound) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return 0L;
            }
            long[] keyTable = this.keyTable;
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (keyTable[i] == 0L || valueTable[i] != null) continue;
                return keyTable[i];
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return 0L;
            }
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (valueTable[i] != value) continue;
                return this.keyTable[i];
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return 0L;
            }
            int i = this.capacity + this.stashSize;
            while (i-- > 0) {
                if (!value.equals(valueTable[i])) continue;
                return this.keyTable[i];
            }
        }
        return notFound;
    }

    public void ensureCapacity(int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded >= this.threshold) {
            this.resize(MathUtils.nextPowerOfTwo((int)Math.ceil((float)sizeNeeded / this.loadFactor)));
        }
    }

    private void resize(int newSize) {
        int oldEndIndex = this.capacity + this.stashSize;
        this.capacity = newSize;
        this.threshold = (int)((float)newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.hashShift = 63 - Long.numberOfTrailingZeros(newSize);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
        this.pushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);
        long[] oldKeyTable = this.keyTable;
        V[] oldValueTable = this.valueTable;
        this.keyTable = new long[newSize + this.stashCapacity];
        this.valueTable = new Object[newSize + this.stashCapacity];
        int oldSize = this.size;
        this.size = this.hasZeroValue ? 1 : 0;
        this.stashSize = 0;
        if (oldSize > 0) {
            for (int i = 0; i < oldEndIndex; ++i) {
                long key = oldKeyTable[i];
                if (key == 0L) continue;
                this.putResize(key, oldValueTable[i]);
            }
        }
    }

    private int hash2(long h) {
        return (int)(((h *= -1262997959L) ^ h >>> this.hashShift) & (long)this.mask);
    }

    private int hash3(long h) {
        return (int)(((h *= -825114047L) ^ h >>> this.hashShift) & (long)this.mask);
    }

    public int hashCode() {
        int h = 0;
        if (this.hasZeroValue && this.zeroValue != null) {
            h += this.zeroValue.hashCode();
        }
        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            long key = keyTable[i];
            if (key == 0L) continue;
            h += (int)(key ^ key >>> 32) * 31;
            V value = valueTable[i];
            if (value == null) continue;
            h += value.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LongMap)) {
            return false;
        }
        LongMap other = (LongMap)obj;
        if (other.size != this.size) {
            return false;
        }
        if (other.hasZeroValue != this.hasZeroValue) {
            return false;
        }
        if (this.hasZeroValue && (other.zeroValue == null ? this.zeroValue != null : !other.zeroValue.equals(this.zeroValue))) {
            return false;
        }
        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            V value;
            long key = keyTable[i];
            if (key == 0L || !((value = valueTable[i]) == null ? !other.containsKey(key) || other.get(key) != null : !value.equals(other.get(key)))) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        long key;
        if (this.size == 0) {
            return "[]";
        }
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = keyTable.length;
        while (i-- > 0) {
            key = keyTable[i];
            if (key == 0L) continue;
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
            break;
        }
        while (i-- > 0) {
            key = keyTable[i];
            if (key == 0L) continue;
            buffer.append(", ");
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    @Override
    public Iterator<Entry<V>> iterator() {
        return this.entries();
    }

    public Entries<V> entries() {
        if (this.entries1 == null) {
            this.entries1 = new Entries(this);
            this.entries2 = new Entries(this);
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

    public Values<V> values() {
        if (this.values1 == null) {
            this.values1 = new Values(this);
            this.values2 = new Values(this);
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

    public Keys keys() {
        if (this.keys1 == null) {
            this.keys1 = new Keys(this);
            this.keys2 = new Keys(this);
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

    public static class Keys
    extends MapIterator {
        public Keys(LongMap map) {
            super(map);
        }

        public long next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            long key = this.nextIndex == -1 ? 0L : this.map.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return key;
        }

        public LongArray toArray() {
            LongArray array = new LongArray(true, this.map.size);
            while (this.hasNext) {
                array.add(this.next());
            }
            return array;
        }
    }

    public static class Values<V>
    extends MapIterator<V>
    implements Iterable<V>,
    Iterator<V> {
        public Values(LongMap<V> map) {
            super(map);
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.hasNext;
        }

        @Override
        public V next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            Object value = this.nextIndex == -1 ? this.map.zeroValue : this.map.valueTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return value;
        }

        @Override
        public Iterator<V> iterator() {
            return this;
        }

        public Array<V> toArray() {
            Array<V> array = new Array<V>(true, this.map.size);
            while (this.hasNext) {
                array.add(this.next());
            }
            return array;
        }

        @Override
        public void remove() {
            super.remove();
        }
    }

    public static class Entries<V>
    extends MapIterator<V>
    implements Iterable<Entry<V>>,
    Iterator<Entry<V>> {
        private Entry<V> entry = new Entry();

        public Entries(LongMap map) {
            super(map);
        }

        @Override
        public Entry<V> next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            long[] keyTable = this.map.keyTable;
            if (this.nextIndex == -1) {
                this.entry.key = 0L;
                this.entry.value = this.map.zeroValue;
            } else {
                this.entry.key = keyTable[this.nextIndex];
                this.entry.value = this.map.valueTable[this.nextIndex];
            }
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return this.entry;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.hasNext;
        }

        @Override
        public Iterator<Entry<V>> iterator() {
            return this;
        }

        @Override
        public void remove() {
            super.remove();
        }
    }

    private static class MapIterator<V> {
        static final int INDEX_ILLEGAL = -2;
        static final int INDEX_ZERO = -1;
        public boolean hasNext;
        final LongMap<V> map;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public MapIterator(LongMap<V> map) {
            this.map = map;
            this.reset();
        }

        public void reset() {
            this.currentIndex = -2;
            this.nextIndex = -1;
            if (this.map.hasZeroValue) {
                this.hasNext = true;
            } else {
                this.findNextIndex();
            }
        }

        void findNextIndex() {
            this.hasNext = false;
            long[] keyTable = this.map.keyTable;
            int n = this.map.capacity + this.map.stashSize;
            while (++this.nextIndex < n) {
                if (keyTable[this.nextIndex] == 0L) continue;
                this.hasNext = true;
                break;
            }
        }

        public void remove() {
            if (this.currentIndex == -1 && this.map.hasZeroValue) {
                this.map.zeroValue = null;
                this.map.hasZeroValue = false;
            } else {
                if (this.currentIndex < 0) {
                    throw new IllegalStateException("next must be called before remove.");
                }
                if (this.currentIndex >= this.map.capacity) {
                    this.map.removeStashIndex(this.currentIndex);
                    this.nextIndex = this.currentIndex - 1;
                    this.findNextIndex();
                } else {
                    this.map.keyTable[this.currentIndex] = 0L;
                    this.map.valueTable[this.currentIndex] = null;
                }
            }
            this.currentIndex = -2;
            --this.map.size;
        }
    }

    public static class Entry<V> {
        public long key;
        public V value;

        public String toString() {
            return this.key + "=" + this.value;
        }
    }
}

