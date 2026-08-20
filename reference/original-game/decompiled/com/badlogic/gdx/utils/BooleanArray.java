/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.StringBuilder;

public class BooleanArray {
    public boolean[] items;
    public int size;
    public boolean ordered;

    public BooleanArray() {
        this(true, 16);
    }

    public BooleanArray(int capacity) {
        this(true, capacity);
    }

    public BooleanArray(boolean ordered, int capacity) {
        this.ordered = ordered;
        this.items = new boolean[capacity];
    }

    public BooleanArray(BooleanArray array) {
        this.ordered = array.ordered;
        this.size = array.size;
        this.items = new boolean[this.size];
        System.arraycopy(array.items, 0, this.items, 0, this.size);
    }

    public BooleanArray(boolean[] array) {
        this(true, array, 0, array.length);
    }

    public BooleanArray(boolean ordered, boolean[] array, int startIndex, int count) {
        this(ordered, count);
        this.size = count;
        System.arraycopy(array, startIndex, this.items, 0, count);
    }

    public void add(boolean value) {
        boolean[] items = this.items;
        if (this.size == items.length) {
            items = this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
        }
        items[this.size++] = value;
    }

    public void addAll(BooleanArray array) {
        this.addAll(array, 0, array.size);
    }

    public void addAll(BooleanArray array, int offset, int length) {
        if (offset + length > array.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        }
        this.addAll(array.items, offset, length);
    }

    public void addAll(boolean ... array) {
        this.addAll(array, 0, array.length);
    }

    public void addAll(boolean[] array, int offset, int length) {
        int sizeNeeded = this.size + length;
        boolean[] items = this.items;
        if (sizeNeeded > items.length) {
            items = this.resize(Math.max(8, (int)((float)sizeNeeded * 1.75f)));
        }
        System.arraycopy(array, offset, items, this.size, length);
        this.size += length;
    }

    public boolean get(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        return this.items[index];
    }

    public void set(int index, boolean value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        this.items[index] = value;
    }

    public void insert(int index, boolean value) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + this.size);
        }
        boolean[] items = this.items;
        if (this.size == items.length) {
            items = this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
        }
        if (this.ordered) {
            System.arraycopy(items, index, items, index + 1, this.size - index);
        } else {
            items[this.size] = items[index];
        }
        ++this.size;
        items[index] = value;
    }

    public void swap(int first, int second) {
        if (first >= this.size) {
            throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + this.size);
        }
        if (second >= this.size) {
            throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + this.size);
        }
        boolean[] items = this.items;
        boolean firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    public boolean removeIndex(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        boolean[] items = this.items;
        boolean value = items[index];
        --this.size;
        if (this.ordered) {
            System.arraycopy(items, index + 1, items, index, this.size - index);
        } else {
            items[index] = items[this.size];
        }
        return value;
    }

    public void removeRange(int start, int end) {
        if (end >= this.size) {
            throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + this.size);
        }
        if (start > end) {
            throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        }
        boolean[] items = this.items;
        int count = end - start + 1;
        if (this.ordered) {
            System.arraycopy(items, start + count, items, start, this.size - (start + count));
        } else {
            int lastIndex = this.size - 1;
            for (int i = 0; i < count; ++i) {
                items[start + i] = items[lastIndex - i];
            }
        }
        this.size -= count;
    }

    public boolean removeAll(BooleanArray array) {
        int size;
        int startSize = size = this.size;
        boolean[] items = this.items;
        int n = array.size;
        block0: for (int i = 0; i < n; ++i) {
            boolean item = array.get(i);
            for (int ii = 0; ii < size; ++ii) {
                if (item != items[ii]) continue;
                this.removeIndex(ii);
                --size;
                continue block0;
            }
        }
        return size != startSize;
    }

    public boolean pop() {
        return this.items[--this.size];
    }

    public boolean peek() {
        return this.items[this.size - 1];
    }

    public boolean first() {
        if (this.size == 0) {
            throw new IllegalStateException("Array is empty.");
        }
        return this.items[0];
    }

    public void clear() {
        this.size = 0;
    }

    public boolean[] shrink() {
        if (this.items.length != this.size) {
            this.resize(this.size);
        }
        return this.items;
    }

    public boolean[] ensureCapacity(int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded > this.items.length) {
            this.resize(Math.max(8, sizeNeeded));
        }
        return this.items;
    }

    public boolean[] setSize(int newSize) {
        if (newSize > this.items.length) {
            this.resize(Math.max(8, newSize));
        }
        this.size = newSize;
        return this.items;
    }

    protected boolean[] resize(int newSize) {
        boolean[] newItems = new boolean[newSize];
        boolean[] items = this.items;
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void reverse() {
        boolean[] items = this.items;
        int lastIndex = this.size - 1;
        int n = this.size / 2;
        for (int i = 0; i < n; ++i) {
            int ii = lastIndex - i;
            boolean temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle() {
        boolean[] items = this.items;
        for (int i = this.size - 1; i >= 0; --i) {
            int ii = MathUtils.random(i);
            boolean temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void truncate(int newSize) {
        if (this.size > newSize) {
            this.size = newSize;
        }
    }

    public boolean random() {
        if (this.size == 0) {
            return false;
        }
        return this.items[MathUtils.random(0, this.size - 1)];
    }

    public boolean[] toArray() {
        boolean[] array = new boolean[this.size];
        System.arraycopy(this.items, 0, array, 0, this.size);
        return array;
    }

    public int hashCode() {
        if (!this.ordered) {
            return super.hashCode();
        }
        boolean[] items = this.items;
        int h = 1;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            h = h * 31 + (items[i] ? 1231 : 1237);
        }
        return h;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!this.ordered) {
            return false;
        }
        if (!(object instanceof BooleanArray)) {
            return false;
        }
        BooleanArray array = (BooleanArray)object;
        if (!array.ordered) {
            return false;
        }
        int n = this.size;
        if (n != array.size) {
            return false;
        }
        boolean[] items1 = this.items;
        boolean[] items2 = array.items;
        for (int i = 0; i < n; ++i) {
            if (items1[i] == items2[i]) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        if (this.size == 0) {
            return "[]";
        }
        boolean[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public String toString(String separator) {
        if (this.size == 0) {
            return "";
        }
        boolean[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }

    public static BooleanArray with(boolean ... array) {
        return new BooleanArray(array);
    }
}

