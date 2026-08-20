/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Queue<T>
implements Iterable<T> {
    protected T[] values;
    protected int head = 0;
    protected int tail = 0;
    public int size = 0;
    private QueueIterable iterable;

    public Queue() {
        this(16);
    }

    public Queue(int initialSize) {
        this.values = new Object[initialSize];
    }

    public Queue(int initialSize, Class<T> type) {
        this.values = (Object[])ArrayReflection.newInstance(type, initialSize);
    }

    public void addLast(T object) {
        T[] values = this.values;
        if (this.size == values.length) {
            this.resize(values.length << 1);
            values = this.values;
        }
        values[this.tail++] = object;
        if (this.tail == values.length) {
            this.tail = 0;
        }
        ++this.size;
    }

    public void addFirst(T object) {
        T[] values = this.values;
        if (this.size == values.length) {
            this.resize(values.length << 1);
            values = this.values;
        }
        int head = this.head;
        if (--head == -1) {
            head = values.length - 1;
        }
        values[head] = object;
        this.head = head;
        ++this.size;
    }

    public void ensureCapacity(int additional) {
        int needed = this.size + additional;
        if (this.values.length < needed) {
            this.resize(needed);
        }
    }

    protected void resize(int newSize) {
        T[] values = this.values;
        int head = this.head;
        int tail = this.tail;
        Object[] newArray = (Object[])ArrayReflection.newInstance(values.getClass().getComponentType(), newSize);
        if (head < tail) {
            System.arraycopy(values, head, newArray, 0, tail - head);
        } else if (this.size > 0) {
            int rest = values.length - head;
            System.arraycopy(values, head, newArray, 0, rest);
            System.arraycopy(values, 0, newArray, rest, tail);
        }
        this.values = newArray;
        this.head = 0;
        this.tail = this.size;
    }

    public T removeFirst() {
        if (this.size == 0) {
            throw new NoSuchElementException("Queue is empty.");
        }
        T[] values = this.values;
        T result = values[this.head];
        values[this.head] = null;
        ++this.head;
        if (this.head == values.length) {
            this.head = 0;
        }
        --this.size;
        return result;
    }

    public T removeLast() {
        if (this.size == 0) {
            throw new NoSuchElementException("Queue is empty.");
        }
        T[] values = this.values;
        int tail = this.tail;
        if (--tail == -1) {
            tail = values.length - 1;
        }
        T result = values[tail];
        values[tail] = null;
        this.tail = tail;
        --this.size;
        return result;
    }

    public int indexOf(T value, boolean identity) {
        if (this.size == 0) {
            return -1;
        }
        T[] values = this.values;
        int head = this.head;
        int tail = this.tail;
        if (identity || value == null) {
            if (head < tail) {
                for (int i = head; i < tail; ++i) {
                    if (values[i] != value) continue;
                    return i;
                }
            } else {
                int i;
                int n = values.length;
                for (i = head; i < n; ++i) {
                    if (values[i] != value) continue;
                    return i - head;
                }
                for (i = 0; i < tail; ++i) {
                    if (values[i] != value) continue;
                    return i + values.length - head;
                }
            }
        } else if (head < tail) {
            for (int i = head; i < tail; ++i) {
                if (!value.equals(values[i])) continue;
                return i;
            }
        } else {
            int i;
            int n = values.length;
            for (i = head; i < n; ++i) {
                if (!value.equals(values[i])) continue;
                return i - head;
            }
            for (i = 0; i < tail; ++i) {
                if (!value.equals(values[i])) continue;
                return i + values.length - head;
            }
        }
        return -1;
    }

    public boolean removeValue(T value, boolean identity) {
        int index = this.indexOf(value, identity);
        if (index == -1) {
            return false;
        }
        this.removeIndex(index);
        return true;
    }

    public T removeIndex(int index) {
        T value;
        if (index < 0) {
            throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        }
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        T[] values = this.values;
        int head = this.head++;
        int tail = this.tail--;
        index += head;
        if (head < tail) {
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
            values[tail] = null;
        } else if (index >= values.length) {
            value = values[index -= values.length];
            System.arraycopy(values, index + 1, values, index, tail - index);
            --this.tail;
        } else {
            value = values[index];
            System.arraycopy(values, head, values, head + 1, index - head);
            values[head] = null;
            if (this.head == values.length) {
                this.head = 0;
            }
        }
        --this.size;
        return value;
    }

    public T first() {
        if (this.size == 0) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return this.values[this.head];
    }

    public T last() {
        if (this.size == 0) {
            throw new NoSuchElementException("Queue is empty.");
        }
        T[] values = this.values;
        int tail = this.tail;
        if (--tail == -1) {
            tail = values.length - 1;
        }
        return values[tail];
    }

    public T get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        }
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        }
        int i = this.head + index;
        T[] values = this.values;
        if (i >= values.length) {
            i -= values.length;
        }
        return values[i];
    }

    public void clear() {
        if (this.size == 0) {
            return;
        }
        T[] values = this.values;
        int head = this.head;
        int tail = this.tail;
        if (head < tail) {
            for (int i = head; i < tail; ++i) {
                values[i] = null;
            }
        } else {
            int i;
            for (i = head; i < values.length; ++i) {
                values[i] = null;
            }
            for (i = 0; i < tail; ++i) {
                values[i] = null;
            }
        }
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        if (this.iterable == null) {
            this.iterable = new QueueIterable(this);
        }
        return this.iterable.iterator();
    }

    public String toString() {
        if (this.size == 0) {
            return "[]";
        }
        T[] values = this.values;
        int head = this.head;
        int tail = this.tail;
        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        sb.append(values[head]);
        int i = (head + 1) % values.length;
        while (i != tail) {
            sb.append(", ").append(values[i]);
            i = (i + 1) % values.length;
        }
        sb.append(']');
        return sb.toString();
    }

    public int hashCode() {
        int size = this.size;
        T[] values = this.values;
        int backingLength = values.length;
        int index = this.head;
        int hash = size + 1;
        for (int s = 0; s < size; ++s) {
            T value = values[index];
            hash *= 31;
            if (value != null) {
                hash += value.hashCode();
            }
            if (++index != backingLength) continue;
            index = 0;
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Queue)) {
            return false;
        }
        Queue q = (Queue)o;
        int size = this.size;
        if (q.size != size) {
            return false;
        }
        T[] myValues = this.values;
        int myBackingLength = myValues.length;
        T[] itsValues = q.values;
        int itsBackingLength = itsValues.length;
        int myIndex = this.head;
        int itsIndex = q.head;
        for (int s = 0; s < size; ++s) {
            T myValue = myValues[myIndex];
            T itsValue = itsValues[itsIndex];
            if (!(myValue != null ? myValue.equals(itsValue) : itsValue == null)) {
                return false;
            }
            ++itsIndex;
            if (++myIndex == myBackingLength) {
                myIndex = 0;
            }
            if (itsIndex != itsBackingLength) continue;
            itsIndex = 0;
        }
        return true;
    }

    public static class QueueIterable<T>
    implements Iterable<T> {
        private final Queue<T> queue;
        private final boolean allowRemove;
        private QueueIterator iterator1;
        private QueueIterator iterator2;

        public QueueIterable(Queue<T> queue) {
            this(queue, true);
        }

        public QueueIterable(Queue<T> queue, boolean allowRemove) {
            this.queue = queue;
            this.allowRemove = allowRemove;
        }

        @Override
        public Iterator<T> iterator() {
            if (this.iterator1 == null) {
                this.iterator1 = new QueueIterator<T>(this.queue, this.allowRemove);
                this.iterator2 = new QueueIterator<T>(this.queue, this.allowRemove);
            }
            if (!this.iterator1.valid) {
                this.iterator1.index = 0;
                this.iterator1.valid = true;
                this.iterator2.valid = false;
                return this.iterator1;
            }
            this.iterator2.index = 0;
            this.iterator2.valid = true;
            this.iterator1.valid = false;
            return this.iterator2;
        }
    }

    public static class QueueIterator<T>
    implements Iterator<T>,
    Iterable<T> {
        private final Queue<T> queue;
        private final boolean allowRemove;
        int index;
        boolean valid = true;

        public QueueIterator(Queue<T> queue) {
            this(queue, true);
        }

        public QueueIterator(Queue<T> queue, boolean allowRemove) {
            this.queue = queue;
            this.allowRemove = allowRemove;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.index < this.queue.size;
        }

        @Override
        public T next() {
            if (this.index >= this.queue.size) {
                throw new NoSuchElementException(String.valueOf(this.index));
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.queue.get(this.index++);
        }

        @Override
        public void remove() {
            if (!this.allowRemove) {
                throw new GdxRuntimeException("Remove not allowed.");
            }
            --this.index;
            this.queue.removeIndex(this.index);
        }

        public void reset() {
            this.index = 0;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }
}

