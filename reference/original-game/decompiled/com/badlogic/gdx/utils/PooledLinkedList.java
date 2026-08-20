/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Pool;

public class PooledLinkedList<T> {
    private Item<T> head;
    private Item<T> tail;
    private Item<T> iter;
    private Item<T> curr;
    private int size = 0;
    private final Pool<Item<T>> pool;

    public PooledLinkedList(int maxPoolSize) {
        this.pool = new Pool<Item<T>>(16, maxPoolSize){

            @Override
            protected Item<T> newObject() {
                return new Item();
            }
        };
    }

    public void add(T object) {
        Item<T> item = this.pool.obtain();
        item.payload = object;
        item.next = null;
        item.prev = null;
        if (this.head == null) {
            this.head = item;
            this.tail = item;
            ++this.size;
            return;
        }
        item.prev = this.tail;
        this.tail.next = item;
        this.tail = item;
        ++this.size;
    }

    public int size() {
        return this.size;
    }

    public void iter() {
        this.iter = this.head;
    }

    public void iterReverse() {
        this.iter = this.tail;
    }

    public T next() {
        if (this.iter == null) {
            return null;
        }
        Object payload = this.iter.payload;
        this.curr = this.iter;
        this.iter = this.iter.next;
        return payload;
    }

    public T previous() {
        if (this.iter == null) {
            return null;
        }
        Object payload = this.iter.payload;
        this.curr = this.iter;
        this.iter = this.iter.prev;
        return payload;
    }

    public void remove() {
        if (this.curr == null) {
            return;
        }
        --this.size;
        this.pool.free(this.curr);
        Item<T> c = this.curr;
        Item n = this.curr.next;
        Item p = this.curr.prev;
        this.curr = null;
        if (this.size == 0) {
            this.head = null;
            this.tail = null;
            return;
        }
        if (c == this.head) {
            n.prev = null;
            this.head = n;
            return;
        }
        if (c == this.tail) {
            p.next = null;
            this.tail = p;
            return;
        }
        p.next = n;
        n.prev = p;
    }

    public void clear() {
        this.iter();
        Object v = null;
        while (true) {
            T t = this.next();
            v = t;
            if (t == null) break;
            this.remove();
        }
    }

    static final class Item<T> {
        public T payload;
        public Item<T> next;
        public Item<T> prev;

        Item() {
        }
    }
}

