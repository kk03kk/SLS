/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntFloatMap
implements Iterable<Entry> {
    private static final int PRIME1 = -1105259343;
    private static final int PRIME2 = -1262997959;
    private static final int PRIME3 = -825114047;
    private static final int EMPTY = 0;
    public int size;
    int[] keyTable;
    float[] valueTable;
    int capacity;
    int stashSize;
    float zeroValue;
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

    public IntFloatMap() {
        this(51, 0.8f);
    }

    public IntFloatMap(int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    public IntFloatMap(int initialCapacity, float loadFactor) {
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
        this.hashShift = 31 - Integer.numberOfTrailingZeros(this.capacity);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(this.capacity)) * 2);
        this.pushIterations = Math.max(Math.min(this.capacity, 8), (int)Math.sqrt(this.capacity) / 8);
        this.keyTable = new int[this.capacity + this.stashCapacity];
        this.valueTable = new float[this.keyTable.length];
    }

    public IntFloatMap(IntFloatMap map) {
        this((int)Math.floor((float)map.capacity * map.loadFactor), map.loadFactor);
        this.stashSize = map.stashSize;
        System.arraycopy(map.keyTable, 0, this.keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, this.valueTable, 0, map.valueTable.length);
        this.size = map.size;
        this.zeroValue = map.zeroValue;
        this.hasZeroValue = map.hasZeroValue;
    }

    public void put(int key, float value) {
        int i;
        if (key == 0) {
            this.zeroValue = value;
            if (!this.hasZeroValue) {
                this.hasZeroValue = true;
                ++this.size;
            }
            return;
        }
        int[] keyTable = this.keyTable;
        int index1 = key & this.mask;
        int key1 = keyTable[index1];
        if (key == key1) {
            this.valueTable[index1] = value;
            return;
        }
        int index2 = this.hash2(key);
        int key2 = keyTable[index2];
        if (key == key2) {
            this.valueTable[index2] = value;
            return;
        }
        int index3 = this.hash3(key);
        int key3 = keyTable[index3];
        if (key == key3) {
            this.valueTable[index3] = value;
            return;
        }
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (key != keyTable[i]) continue;
            this.valueTable[i] = value;
            return;
        }
        if (key1 == 0) {
            keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        if (key2 == 0) {
            keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        if (key3 == 0) {
            keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        this.push(key, value, index1, key1, index2, key2, index3, key3);
    }

    public void putAll(IntFloatMap map) {
        for (Entry entry : map.entries()) {
            this.put(entry.key, entry.value);
        }
    }

    private void putResize(int key, float value) {
        if (key == 0) {
            this.zeroValue = value;
            this.hasZeroValue = true;
            return;
        }
        int index1 = key & this.mask;
        int key1 = this.keyTable[index1];
        if (key1 == 0) {
            this.keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index2 = this.hash2(key);
        int key2 = this.keyTable[index2];
        if (key2 == 0) {
            this.keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index3 = this.hash3(key);
        int key3 = this.keyTable[index3];
        if (key3 == 0) {
            this.keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        this.push(key, value, index1, key1, index2, key2, index3, key3);
    }

    private void push(int insertKey, float insertValue, int index1, int key1, int index2, int key2, int index3, int key3) {
        float evictedValue;
        int evictedKey;
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
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
            index1 = evictedKey & mask;
            key1 = keyTable[index1];
            if (key1 == 0) {
                keyTable[index1] = evictedKey;
                valueTable[index1] = evictedValue;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index2 = this.hash2(evictedKey);
            key2 = keyTable[index2];
            if (key2 == 0) {
                keyTable[index2] = evictedKey;
                valueTable[index2] = evictedValue;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index3 = this.hash3(evictedKey);
            key3 = keyTable[index3];
            if (key3 == 0) {
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

    private void putStash(int key, float value) {
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

    public float get(int key, float defaultValue) {
        if (key == 0) {
            if (!this.hasZeroValue) {
                return defaultValue;
            }
            return this.zeroValue;
        }
        int index = key & this.mask;
        if (this.keyTable[index] != key && this.keyTable[index = this.hash2(key)] != key && this.keyTable[index = this.hash3(key)] != key) {
            return this.getStash(key, defaultValue);
        }
        return this.valueTable[index];
    }

    private float getStash(int key, float defaultValue) {
        int i;
        int[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (key != keyTable[i]) continue;
            return this.valueTable[i];
        }
        return defaultValue;
    }

    public float getAndIncrement(int key, float defaultValue, float increment) {
        if (key == 0) {
            if (this.hasZeroValue) {
                float value = this.zeroValue;
                this.zeroValue += increment;
                return value;
            }
            this.hasZeroValue = true;
            this.zeroValue = defaultValue + increment;
            ++this.size;
            return defaultValue;
        }
        int index = key & this.mask;
        if (key != this.keyTable[index] && key != this.keyTable[index = this.hash2(key)] && key != this.keyTable[index = this.hash3(key)]) {
            return this.getAndIncrementStash(key, defaultValue, increment);
        }
        float value = this.valueTable[index];
        this.valueTable[index] = value + increment;
        return value;
    }

    private float getAndIncrementStash(int key, float defaultValue, float increment) {
        int i;
        int[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (key != keyTable[i]) continue;
            float value = this.valueTable[i];
            this.valueTable[i] = value + increment;
            return value;
        }
        this.put(key, defaultValue + increment);
        return defaultValue;
    }

    public float remove(int key, float defaultValue) {
        if (key == 0) {
            if (!this.hasZeroValue) {
                return defaultValue;
            }
            this.hasZeroValue = false;
            --this.size;
            return this.zeroValue;
        }
        int index = key & this.mask;
        if (key == this.keyTable[index]) {
            this.keyTable[index] = 0;
            float oldValue = this.valueTable[index];
            --this.size;
            return oldValue;
        }
        index = this.hash2(key);
        if (key == this.keyTable[index]) {
            this.keyTable[index] = 0;
            float oldValue = this.valueTable[index];
            --this.size;
            return oldValue;
        }
        index = this.hash3(key);
        if (key == this.keyTable[index]) {
            this.keyTable[index] = 0;
            float oldValue = this.valueTable[index];
            --this.size;
            return oldValue;
        }
        return this.removeStash(key, defaultValue);
    }

    float removeStash(int key, float defaultValue) {
        int i;
        int[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (key != keyTable[i]) continue;
            float oldValue = this.valueTable[i];
            this.removeStashIndex(i);
            --this.size;
            return oldValue;
        }
        return defaultValue;
    }

    void removeStashIndex(int index) {
        --this.stashSize;
        int lastIndex = this.capacity + this.stashSize;
        if (index < lastIndex) {
            this.keyTable[index] = this.keyTable[lastIndex];
            this.valueTable[index] = this.valueTable[lastIndex];
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
        this.hasZeroValue = false;
        this.size = 0;
        this.resize(maximumCapacity);
    }

    public void clear() {
        if (this.size == 0) {
            return;
        }
        int[] keyTable = this.keyTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            keyTable[i] = 0;
        }
        this.hasZeroValue = false;
        this.size = 0;
        this.stashSize = 0;
    }

    public boolean containsValue(float value) {
        if (this.hasZeroValue && this.zeroValue == value) {
            return true;
        }
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            if (keyTable[i] == 0 || valueTable[i] != value) continue;
            return true;
        }
        return false;
    }

    public boolean containsValue(float value, float epsilon) {
        if (this.hasZeroValue && Math.abs(this.zeroValue - value) <= epsilon) {
            return true;
        }
        float[] valueTable = this.valueTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            if (!(Math.abs(valueTable[i] - value) <= epsilon)) continue;
            return true;
        }
        return false;
    }

    public boolean containsKey(int key) {
        if (key == 0) {
            return this.hasZeroValue;
        }
        int index = key & this.mask;
        if (this.keyTable[index] != key && this.keyTable[index = this.hash2(key)] != key && this.keyTable[index = this.hash3(key)] != key) {
            return this.containsKeyStash(key);
        }
        return true;
    }

    private boolean containsKeyStash(int key) {
        int i;
        int[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (key != keyTable[i]) continue;
            return true;
        }
        return false;
    }

    public int findKey(float value, int notFound) {
        if (this.hasZeroValue && this.zeroValue == value) {
            return 0;
        }
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            if (keyTable[i] == 0 || valueTable[i] != value) continue;
            return keyTable[i];
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
        this.hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
        this.pushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);
        int[] oldKeyTable = this.keyTable;
        float[] oldValueTable = this.valueTable;
        this.keyTable = new int[newSize + this.stashCapacity];
        this.valueTable = new float[newSize + this.stashCapacity];
        int oldSize = this.size;
        this.size = this.hasZeroValue ? 1 : 0;
        this.stashSize = 0;
        if (oldSize > 0) {
            for (int i = 0; i < oldEndIndex; ++i) {
                int key = oldKeyTable[i];
                if (key == 0) continue;
                this.putResize(key, oldValueTable[i]);
            }
        }
    }

    private int hash2(int h) {
        return ((h *= -1262997959) ^ h >>> this.hashShift) & this.mask;
    }

    private int hash3(int h) {
        return ((h *= -825114047) ^ h >>> this.hashShift) & this.mask;
    }

    public int hashCode() {
        int h = 0;
        if (this.hasZeroValue) {
            h += Float.floatToIntBits(this.zeroValue);
        }
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            int key = keyTable[i];
            if (key == 0) continue;
            h += key * 31;
            float value = valueTable[i];
            h += Float.floatToIntBits(value);
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntFloatMap)) {
            return false;
        }
        IntFloatMap other = (IntFloatMap)obj;
        if (other.size != this.size) {
            return false;
        }
        if (other.hasZeroValue != this.hasZeroValue) {
            return false;
        }
        if (this.hasZeroValue && other.zeroValue != this.zeroValue) {
            return false;
        }
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            int key = keyTable[i];
            if (key == 0) continue;
            float otherValue = other.get(key, 0.0f);
            if (otherValue == 0.0f && !other.containsKey(key)) {
                return false;
            }
            float value = valueTable[i];
            if (otherValue == value) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        int key;
        if (this.size == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        int[] keyTable = this.keyTable;
        float[] valueTable = this.valueTable;
        int i = keyTable.length;
        if (this.hasZeroValue) {
            buffer.append("0=");
            buffer.append(this.zeroValue);
        } else {
            while (i-- > 0) {
                key = keyTable[i];
                if (key == 0) continue;
                buffer.append(key);
                buffer.append('=');
                buffer.append(valueTable[i]);
                break;
            }
        }
        while (i-- > 0) {
            key = keyTable[i];
            if (key == 0) continue;
            buffer.append(", ");
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public Iterator<Entry> iterator() {
        return this.entries();
    }

    public Entries entries() {
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

    public Values values() {
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
        public Keys(IntFloatMap map) {
            super(map);
        }

        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.hasNext;
        }

        public int next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            int key = this.nextIndex == -1 ? 0 : this.map.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return key;
        }

        public IntArray toArray() {
            IntArray array = new IntArray(true, this.map.size);
            while (this.hasNext) {
                array.add(this.next());
            }
            return array;
        }
    }

    public static class Values
    extends MapIterator {
        public Values(IntFloatMap map) {
            super(map);
        }

        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.hasNext;
        }

        public float next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            float value = this.nextIndex == -1 ? this.map.zeroValue : this.map.valueTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return value;
        }

        public FloatArray toArray() {
            FloatArray array = new FloatArray(true, this.map.size);
            while (this.hasNext) {
                array.add(this.next());
            }
            return array;
        }
    }

    public static class Entries
    extends MapIterator
    implements Iterable<Entry>,
    Iterator<Entry> {
        private Entry entry = new Entry();

        public Entries(IntFloatMap map) {
            super(map);
        }

        @Override
        public Entry next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            int[] keyTable = this.map.keyTable;
            if (this.nextIndex == -1) {
                this.entry.key = 0;
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
        public Iterator<Entry> iterator() {
            return this;
        }

        @Override
        public void remove() {
            super.remove();
        }
    }

    private static class MapIterator {
        static final int INDEX_ILLEGAL = -2;
        static final int INDEX_ZERO = -1;
        public boolean hasNext;
        final IntFloatMap map;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public MapIterator(IntFloatMap map) {
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
            int[] keyTable = this.map.keyTable;
            int n = this.map.capacity + this.map.stashSize;
            while (++this.nextIndex < n) {
                if (keyTable[this.nextIndex] == 0) continue;
                this.hasNext = true;
                break;
            }
        }

        public void remove() {
            if (this.currentIndex == -1 && this.map.hasZeroValue) {
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
                    this.map.keyTable[this.currentIndex] = 0;
                }
            }
            this.currentIndex = -2;
            --this.map.size;
        }
    }

    public static class Entry {
        public int key;
        public float value;

        public String toString() {
            return this.key + "=" + this.value;
        }
    }
}

