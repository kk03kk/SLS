/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectSet<T>
implements Iterable<T> {
    private static final int PRIME1 = -1105259343;
    private static final int PRIME2 = -1262997959;
    private static final int PRIME3 = -825114047;
    public int size;
    T[] keyTable;
    int capacity;
    int stashSize;
    private float loadFactor;
    private int hashShift;
    private int mask;
    private int threshold;
    private int stashCapacity;
    private int pushIterations;
    private ObjectSetIterator iterator1;
    private ObjectSetIterator iterator2;

    public ObjectSet() {
        this(51, 0.8f);
    }

    public ObjectSet(int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    public ObjectSet(int initialCapacity, float loadFactor) {
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
        this.keyTable = new Object[this.capacity + this.stashCapacity];
    }

    public ObjectSet(ObjectSet set) {
        this((int)Math.floor((float)set.capacity * set.loadFactor), set.loadFactor);
        this.stashSize = set.stashSize;
        System.arraycopy(set.keyTable, 0, this.keyTable, 0, set.keyTable.length);
        this.size = set.size;
    }

    public boolean add(T key) {
        int i;
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null.");
        }
        T[] keyTable = this.keyTable;
        int hashCode = key.hashCode();
        int index1 = hashCode & this.mask;
        T key1 = keyTable[index1];
        if (key.equals(key1)) {
            return false;
        }
        int index2 = this.hash2(hashCode);
        T key2 = keyTable[index2];
        if (key.equals(key2)) {
            return false;
        }
        int index3 = this.hash3(hashCode);
        T key3 = keyTable[index3];
        if (key.equals(key3)) {
            return false;
        }
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (!key.equals(keyTable[i])) continue;
            return false;
        }
        if (key1 == null) {
            keyTable[index1] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return true;
        }
        if (key2 == null) {
            keyTable[index2] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return true;
        }
        if (key3 == null) {
            keyTable[index3] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return true;
        }
        this.push(key, index1, key1, index2, key2, index3, key3);
        return true;
    }

    public void addAll(Array<? extends T> array) {
        this.addAll(array, 0, array.size);
    }

    public void addAll(Array<? extends T> array, int offset, int length) {
        if (offset + length > array.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        }
        this.addAll(array.items, offset, length);
    }

    public void addAll(T ... array) {
        this.addAll(array, 0, array.length);
    }

    public void addAll(T[] array, int offset, int length) {
        int i;
        this.ensureCapacity(length);
        int n = i + length;
        for (i = offset; i < n; ++i) {
            this.add(array[i]);
        }
    }

    public void addAll(ObjectSet<T> set) {
        this.ensureCapacity(set.size);
        for (Object key : set) {
            this.add(key);
        }
    }

    private void addResize(T key) {
        int hashCode = key.hashCode();
        int index1 = hashCode & this.mask;
        T key1 = this.keyTable[index1];
        if (key1 == null) {
            this.keyTable[index1] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index2 = this.hash2(hashCode);
        T key2 = this.keyTable[index2];
        if (key2 == null) {
            this.keyTable[index2] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        int index3 = this.hash3(hashCode);
        T key3 = this.keyTable[index3];
        if (key3 == null) {
            this.keyTable[index3] = key;
            if (this.size++ >= this.threshold) {
                this.resize(this.capacity << 1);
            }
            return;
        }
        this.push(key, index1, key1, index2, key2, index3, key3);
    }

    private void push(T insertKey, int index1, T key1, int index2, T key2, int index3, T key3) {
        T evictedKey;
        T[] keyTable = this.keyTable;
        int mask = this.mask;
        int i = 0;
        int pushIterations = this.pushIterations;
        while (true) {
            switch (MathUtils.random(2)) {
                case 0: {
                    evictedKey = key1;
                    keyTable[index1] = insertKey;
                    break;
                }
                case 1: {
                    evictedKey = key2;
                    keyTable[index2] = insertKey;
                    break;
                }
                default: {
                    evictedKey = key3;
                    keyTable[index3] = insertKey;
                }
            }
            int hashCode = evictedKey.hashCode();
            index1 = hashCode & mask;
            key1 = keyTable[index1];
            if (key1 == null) {
                keyTable[index1] = evictedKey;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index2 = this.hash2(hashCode);
            key2 = keyTable[index2];
            if (key2 == null) {
                keyTable[index2] = evictedKey;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            index3 = this.hash3(hashCode);
            key3 = keyTable[index3];
            if (key3 == null) {
                keyTable[index3] = evictedKey;
                if (this.size++ >= this.threshold) {
                    this.resize(this.capacity << 1);
                }
                return;
            }
            if (++i == pushIterations) break;
            insertKey = evictedKey;
        }
        this.addStash(evictedKey);
    }

    private void addStash(T key) {
        if (this.stashSize == this.stashCapacity) {
            this.resize(this.capacity << 1);
            this.add(key);
            return;
        }
        int index = this.capacity + this.stashSize;
        this.keyTable[index] = key;
        ++this.stashSize;
        ++this.size;
    }

    public boolean remove(T key) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            --this.size;
            return true;
        }
        index = this.hash2(hashCode);
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            --this.size;
            return true;
        }
        index = this.hash3(hashCode);
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            --this.size;
            return true;
        }
        return this.removeStash(key);
    }

    boolean removeStash(T key) {
        int i;
        T[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (!key.equals(keyTable[i])) continue;
            this.removeStashIndex(i);
            --this.size;
            return true;
        }
        return false;
    }

    void removeStashIndex(int index) {
        --this.stashSize;
        int lastIndex = this.capacity + this.stashSize;
        if (index < lastIndex) {
            this.keyTable[index] = this.keyTable[lastIndex];
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
        this.size = 0;
        this.resize(maximumCapacity);
    }

    public void clear() {
        if (this.size == 0) {
            return;
        }
        T[] keyTable = this.keyTable;
        int i = this.capacity + this.stashSize;
        while (i-- > 0) {
            keyTable[i] = null;
        }
        this.size = 0;
        this.stashSize = 0;
    }

    public boolean contains(T key) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (!(key.equals(this.keyTable[index]) || key.equals(this.keyTable[index = this.hash2(hashCode)]) || key.equals(this.keyTable[index = this.hash3(hashCode)]))) {
            return this.containsKeyStash(key);
        }
        return true;
    }

    private boolean containsKeyStash(T key) {
        int i;
        T[] keyTable = this.keyTable;
        int n = i + this.stashSize;
        for (i = this.capacity; i < n; ++i) {
            if (!key.equals(keyTable[i])) continue;
            return true;
        }
        return false;
    }

    public T first() {
        T[] keyTable = this.keyTable;
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            if (keyTable[i] == null) continue;
            return keyTable[i];
        }
        throw new IllegalStateException("ObjectSet is empty.");
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
        T[] oldKeyTable = this.keyTable;
        this.keyTable = new Object[newSize + this.stashCapacity];
        int oldSize = this.size;
        this.size = 0;
        this.stashSize = 0;
        if (oldSize > 0) {
            for (int i = 0; i < oldEndIndex; ++i) {
                T key = oldKeyTable[i];
                if (key == null) continue;
                this.addResize(key);
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
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            if (this.keyTable[i] == null) continue;
            h += this.keyTable[i].hashCode();
        }
        return h;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectSet)) {
            return false;
        }
        ObjectSet other = (ObjectSet)obj;
        if (other.size != this.size) {
            return false;
        }
        int n = this.capacity + this.stashSize;
        for (int i = 0; i < n; ++i) {
            if (this.keyTable[i] == null || other.contains(this.keyTable[i])) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        return '{' + this.toString(", ") + '}';
    }

    public String toString(String separator) {
        T key;
        if (this.size == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(32);
        T[] keyTable = this.keyTable;
        int i = keyTable.length;
        while (i-- > 0) {
            key = keyTable[i];
            if (key == null) continue;
            buffer.append(key);
            break;
        }
        while (i-- > 0) {
            key = keyTable[i];
            if (key == null) continue;
            buffer.append(separator);
            buffer.append(key);
        }
        return buffer.toString();
    }

    @Override
    public ObjectSetIterator<T> iterator() {
        if (this.iterator1 == null) {
            this.iterator1 = new ObjectSetIterator(this);
            this.iterator2 = new ObjectSetIterator(this);
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

    public static <T> ObjectSet<T> with(T ... array) {
        ObjectSet<T> set = new ObjectSet<T>();
        set.addAll(array);
        return set;
    }

    public static class ObjectSetIterator<K>
    implements Iterable<K>,
    Iterator<K> {
        public boolean hasNext;
        final ObjectSet<K> set;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public ObjectSetIterator(ObjectSet<K> set) {
            this.set = set;
            this.reset();
        }

        public void reset() {
            this.currentIndex = -1;
            this.nextIndex = -1;
            this.findNextIndex();
        }

        void findNextIndex() {
            this.hasNext = false;
            T[] keyTable = this.set.keyTable;
            int n = this.set.capacity + this.set.stashSize;
            while (++this.nextIndex < n) {
                if (keyTable[this.nextIndex] == null) continue;
                this.hasNext = true;
                break;
            }
        }

        @Override
        public void remove() {
            if (this.currentIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            }
            if (this.currentIndex >= this.set.capacity) {
                this.set.removeStashIndex(this.currentIndex);
                this.nextIndex = this.currentIndex - 1;
                this.findNextIndex();
            } else {
                this.set.keyTable[this.currentIndex] = null;
            }
            this.currentIndex = -1;
            --this.set.size;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.hasNext;
        }

        @Override
        public K next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            Object key = this.set.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            this.findNextIndex();
            return (K)key;
        }

        @Override
        public ObjectSetIterator<K> iterator() {
            return this;
        }

        public Array<K> toArray(Array<K> array) {
            while (this.hasNext) {
                array.add(this.next());
            }
            return array;
        }

        public Array<K> toArray() {
            return this.toArray(new Array(true, this.set.size));
        }
    }
}

