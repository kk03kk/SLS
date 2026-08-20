/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Pool;

public class SortedIntList<E>
implements Iterable<Node<E>> {
    private NodePool<E> nodePool = new NodePool();
    private Iterator iterator;
    int size = 0;
    Node<E> first;

    public E insert(int index, E value) {
        if (this.first != null) {
            Node<E> c = this.first;
            while (c.n != null && c.n.index <= index) {
                c = c.n;
            }
            if (index > c.index) {
                c.n = this.nodePool.obtain(c, c.n, value, index);
                if (c.n.n != null) {
                    c.n.n.p = c.n;
                }
                ++this.size;
            } else if (index < c.index) {
                Node<E> newFirst = this.nodePool.obtain(null, this.first, value, index);
                this.first.p = newFirst;
                this.first = newFirst;
                ++this.size;
            } else {
                c.value = value;
            }
        } else {
            this.first = this.nodePool.obtain(null, null, value, index);
            ++this.size;
        }
        return null;
    }

    public E get(int index) {
        E match = null;
        if (this.first != null) {
            Node<E> c = this.first;
            while (c.n != null && c.index < index) {
                c = c.n;
            }
            if (c.index == index) {
                match = c.value;
            }
        }
        return match;
    }

    public void clear() {
        while (this.first != null) {
            this.nodePool.free(this.first);
            this.first = this.first.n;
        }
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    @Override
    public java.util.Iterator<Node<E>> iterator() {
        if (this.iterator == null) {
            this.iterator = new Iterator();
        }
        return this.iterator.reset();
    }

    static class NodePool<E>
    extends Pool<Node<E>> {
        NodePool() {
        }

        @Override
        protected Node<E> newObject() {
            return new Node();
        }

        public Node<E> obtain(Node<E> p, Node<E> n, E value, int index) {
            Node newNode = (Node)super.obtain();
            newNode.p = p;
            newNode.n = n;
            newNode.value = value;
            newNode.index = index;
            return newNode;
        }
    }

    public static class Node<E> {
        protected Node<E> p;
        protected Node<E> n;
        public E value;
        public int index;
    }

    class Iterator
    implements java.util.Iterator<Node<E>> {
        private Node<E> position;
        private Node<E> previousPosition;

        Iterator() {
        }

        @Override
        public boolean hasNext() {
            return this.position != null;
        }

        @Override
        public Node<E> next() {
            this.previousPosition = this.position;
            this.position = this.position.n;
            return this.previousPosition;
        }

        @Override
        public void remove() {
            if (this.previousPosition != null) {
                if (this.previousPosition == SortedIntList.this.first) {
                    SortedIntList.this.first = this.position;
                } else {
                    this.previousPosition.p.n = this.position;
                    if (this.position != null) {
                        this.position.p = this.previousPosition.p;
                    }
                }
                --SortedIntList.this.size;
            }
        }

        public Iterator reset() {
            this.position = SortedIntList.this.first;
            this.previousPosition = null;
            return this;
        }
    }
}

