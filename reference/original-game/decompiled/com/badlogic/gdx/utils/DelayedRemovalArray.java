/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import java.util.Comparator;

public class DelayedRemovalArray<T>
extends Array<T> {
    private int iterating;
    private IntArray remove = new IntArray(0);

    public DelayedRemovalArray() {
    }

    public DelayedRemovalArray(Array array) {
        super(array);
    }

    public DelayedRemovalArray(boolean ordered, int capacity, Class arrayType) {
        super(ordered, capacity, arrayType);
    }

    public DelayedRemovalArray(boolean ordered, int capacity) {
        super(ordered, capacity);
    }

    public DelayedRemovalArray(boolean ordered, T[] array, int startIndex, int count) {
        super(ordered, array, startIndex, count);
    }

    public DelayedRemovalArray(Class arrayType) {
        super(arrayType);
    }

    public DelayedRemovalArray(int capacity) {
        super(capacity);
    }

    public DelayedRemovalArray(T[] array) {
        super(array);
    }

    public void begin() {
        ++this.iterating;
    }

    public void end() {
        if (this.iterating == 0) {
            throw new IllegalStateException("begin must be called before end.");
        }
        --this.iterating;
        if (this.iterating == 0) {
            int n = this.remove.size;
            for (int i = 0; i < n; ++i) {
                this.removeIndex(this.remove.pop());
            }
        }
    }

    private void remove(int index) {
        int n = this.remove.size;
        for (int i = 0; i < n; ++i) {
            int removeIndex = this.remove.get(i);
            if (index == removeIndex) {
                return;
            }
            if (index >= removeIndex) continue;
            this.remove.insert(i, index);
            return;
        }
        this.remove.add(index);
    }

    @Override
    public boolean removeValue(T value, boolean identity) {
        if (this.iterating > 0) {
            int index = this.indexOf(value, identity);
            if (index == -1) {
                return false;
            }
            this.remove(index);
            return true;
        }
        return super.removeValue(value, identity);
    }

    @Override
    public T removeIndex(int index) {
        if (this.iterating > 0) {
            this.remove(index);
            return this.get(index);
        }
        return super.removeIndex(index);
    }

    @Override
    public void removeRange(int start, int end) {
        if (this.iterating > 0) {
            for (int i = end; i >= start; --i) {
                this.remove(i);
            }
        } else {
            super.removeRange(start, end);
        }
    }

    @Override
    public void set(int index, T value) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.set(index, value);
    }

    @Override
    public void insert(int index, T value) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.insert(index, value);
    }

    @Override
    public void swap(int first, int second) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.swap(first, second);
    }

    @Override
    public T pop() {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        return super.pop();
    }

    @Override
    public void clear() {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.clear();
    }

    @Override
    public void sort() {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.sort();
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.sort(comparator);
    }

    @Override
    public void reverse() {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.reverse();
    }

    @Override
    public void shuffle() {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.shuffle();
    }

    @Override
    public void truncate(int newSize) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        super.truncate(newSize);
    }

    @Override
    public T[] setSize(int newSize) {
        if (this.iterating > 0) {
            throw new IllegalStateException("Invalid between begin/end.");
        }
        return super.setSize(newSize);
    }

    public static <T> DelayedRemovalArray<T> with(T ... array) {
        return new DelayedRemovalArray<T>(array);
    }
}

