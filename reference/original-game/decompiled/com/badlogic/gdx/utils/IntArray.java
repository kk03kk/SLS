/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.Arrays;

public class IntArray {
    public int[] items;
    public int size;
    public boolean ordered;

    public IntArray() {
        this(true, 16);
    }

    public IntArray(int capacity) {
        this(true, capacity);
    }

    public IntArray(boolean ordered, int capacity) {
        this.ordered = ordered;
        this.items = new int[capacity];
    }

    public IntArray(IntArray array) {
        this.ordered = array.ordered;
        this.size = array.size;
        this.items = new int[this.size];
        System.arraycopy(array.items, 0, this.items, 0, this.size);
    }

    public IntArray(int[] array) {
        this(true, array, 0, array.length);
    }

    public IntArray(boolean ordered, int[] array, int startIndex, int count) {
        this(ordered, count);
        this.size = count;
        System.arraycopy(array, startIndex, this.items, 0, count);
    }

    public void add(int value) {
        int[] items = this.items;
        if (this.size == items.length) {
            items = this.resize(Math.max(8, (int)((float)this.size * 1.75f)));
        }
        items[this.size++] = value;
    }

    public void addAll(IntArray array) {
        this.addAll(array, 0, array.size);
    }

    public void addAll(IntArray array, int offset, int length) {
        if (offset + length > array.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        }
        this.addAll(array.items, offset, length);
    }

    public void addAll(int ... array) {
        this.addAll(array, 0, array.length);
    }

    public void addAll(int[] array, int offset, int length) {
        int sizeNeeded = this.size + length;
        int[] items = this.items;
        if (sizeNeeded > items.length) {
            items = this.resize(Math.max(8, (int)((float)sizeNeeded * 1.75f)));
        }
        System.arraycopy(array, offset, items, this.size, length);
        this.size += length;
    }

    public int get(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        return this.items[index];
    }

    public void set(int index, int value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        this.items[index] = value;
    }

    public void incr(int index, int value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        int n = index;
        this.items[n] = this.items[n] + value;
    }

    public void mul(int index, int value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        int n = index;
        this.items[n] = this.items[n] * value;
    }

    public void insert(int index, int value) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + this.size);
        }
        int[] items = this.items;
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
        int[] items = this.items;
        int firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    public boolean contains(int value) {
        int i = this.size - 1;
        int[] items = this.items;
        while (i >= 0) {
            if (items[i--] != value) continue;
            return true;
        }
        return false;
    }

    public int indexOf(int value) {
        int[] items = this.items;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            if (items[i] != value) continue;
            return i;
        }
        return -1;
    }

    public int lastIndexOf(int value) {
        int[] items = this.items;
        for (int i = this.size - 1; i >= 0; --i) {
            if (items[i] != value) continue;
            return i;
        }
        return -1;
    }

    public boolean removeValue(int value) {
        int[] items = this.items;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            if (items[i] != value) continue;
            this.removeIndex(i);
            return true;
        }
        return false;
    }

    public int removeIndex(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        int[] items = this.items;
        int value = items[index];
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
        int[] items = this.items;
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

    public boolean removeAll(IntArray array) {
        int size;
        int startSize = size = this.size;
        int[] items = this.items;
        int n = array.size;
        block0: for (int i = 0; i < n; ++i) {
            int item = array.get(i);
            for (int ii = 0; ii < size; ++ii) {
                if (item != items[ii]) continue;
                this.removeIndex(ii);
                --size;
                continue block0;
            }
        }
        return size != startSize;
    }

    public int pop() {
        return this.items[--this.size];
    }

    public int peek() {
        return this.items[this.size - 1];
    }

    public int first() {
        if (this.size == 0) {
            throw new IllegalStateException("Array is empty.");
        }
        return this.items[0];
    }

    public void clear() {
        this.size = 0;
    }

    public int[] shrink() {
        if (this.items.length != this.size) {
            this.resize(this.size);
        }
        return this.items;
    }

    public int[] ensureCapacity(int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded > this.items.length) {
            this.resize(Math.max(8, sizeNeeded));
        }
        return this.items;
    }

    public int[] setSize(int newSize) {
        if (newSize > this.items.length) {
            this.resize(Math.max(8, newSize));
        }
        this.size = newSize;
        return this.items;
    }

    protected int[] resize(int newSize) {
        int[] newItems = new int[newSize];
        int[] items = this.items;
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort() {
        Arrays.sort(this.items, 0, this.size);
    }

    public void reverse() {
        int[] items = this.items;
        int lastIndex = this.size - 1;
        int n = this.size / 2;
        for (int i = 0; i < n; ++i) {
            int ii = lastIndex - i;
            int temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle() {
        int[] items = this.items;
        for (int i = this.size - 1; i >= 0; --i) {
            int ii = MathUtils.random(i);
            int temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void truncate(int newSize) {
        if (this.size > newSize) {
            this.size = newSize;
        }
    }

    public int random() {
        if (this.size == 0) {
            return 0;
        }
        return this.items[MathUtils.random(0, this.size - 1)];
    }

    public int[] toArray() {
        int[] array = new int[this.size];
        System.arraycopy(this.items, 0, array, 0, this.size);
        return array;
    }

    public int hashCode() {
        if (!this.ordered) {
            return super.hashCode();
        }
        int[] items = this.items;
        int h = 1;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            h = h * 31 + items[i];
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
        if (!(object instanceof IntArray)) {
            return false;
        }
        IntArray array = (IntArray)object;
        if (!array.ordered) {
            return false;
        }
        int n = this.size;
        if (n != array.size) {
            return false;
        }
        int[] items1 = this.items;
        int[] items2 = array.items;
        for (int i = 0; i < n; ++i) {
            if (this.items[i] == array.items[i]) continue;
            return false;
        }
        return true;
    }

    public String toString() {
        if (this.size == 0) {
            return "[]";
        }
        int[] items = this.items;
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
        int[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }

    public static IntArray with(int ... array) {
        return new IntArray(array);
    }
}

