/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.StringBuilder;

public class BinaryHeap<T extends Node> {
    public int size;
    private Node[] nodes;
    private final boolean isMaxHeap;

    public BinaryHeap() {
        this(16, false);
    }

    public BinaryHeap(int capacity, boolean isMaxHeap) {
        this.isMaxHeap = isMaxHeap;
        this.nodes = new Node[capacity];
    }

    public T add(T node) {
        if (this.size == this.nodes.length) {
            Node[] newNodes = new Node[this.size << 1];
            System.arraycopy(this.nodes, 0, newNodes, 0, this.size);
            this.nodes = newNodes;
        }
        ((Node)node).index = this.size;
        this.nodes[this.size] = node;
        this.up(this.size++);
        return node;
    }

    public T add(T node, float value) {
        ((Node)node).value = value;
        return this.add(node);
    }

    public T peek() {
        if (this.size == 0) {
            throw new IllegalStateException("The heap is empty.");
        }
        return (T)this.nodes[0];
    }

    public T pop() {
        return this.remove(0);
    }

    public T remove(T node) {
        return this.remove(((Node)node).index);
    }

    private T remove(int index) {
        Node[] nodes = this.nodes;
        Node removed = nodes[index];
        nodes[index] = nodes[--this.size];
        nodes[this.size] = null;
        if (this.size > 0 && index < this.size) {
            this.down(index);
        }
        return (T)removed;
    }

    public void clear() {
        Node[] nodes = this.nodes;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            nodes[i] = null;
        }
        this.size = 0;
    }

    public void setValue(T node, float value) {
        float oldValue = ((Node)node).value;
        ((Node)node).value = value;
        if (value < oldValue ^ this.isMaxHeap) {
            this.up(((Node)node).index);
        } else {
            this.down(((Node)node).index);
        }
    }

    private void up(int index) {
        Node[] nodes = this.nodes;
        Node node = nodes[index];
        float value = node.value;
        while (index > 0) {
            int parentIndex = index - 1 >> 1;
            Node parent = nodes[parentIndex];
            if (!(value < parent.value ^ this.isMaxHeap)) break;
            nodes[index] = parent;
            parent.index = index;
            index = parentIndex;
        }
        nodes[index] = node;
        node.index = index;
    }

    private void down(int index) {
        int leftIndex;
        Node[] nodes = this.nodes;
        int size = this.size;
        Node node = nodes[index];
        float value = node.value;
        while ((leftIndex = 1 + (index << 1)) < size) {
            float rightValue;
            Node rightNode;
            int rightIndex = leftIndex + 1;
            Node leftNode = nodes[leftIndex];
            float leftValue = leftNode.value;
            if (rightIndex >= size) {
                rightNode = null;
                rightValue = this.isMaxHeap ? Float.MIN_VALUE : Float.MAX_VALUE;
            } else {
                rightNode = nodes[rightIndex];
                rightValue = rightNode.value;
            }
            if (leftValue < rightValue ^ this.isMaxHeap) {
                if (leftValue == value || leftValue > value ^ this.isMaxHeap) break;
                nodes[index] = leftNode;
                leftNode.index = index;
                index = leftIndex;
                continue;
            }
            if (rightValue == value || rightValue > value ^ this.isMaxHeap) break;
            nodes[index] = rightNode;
            rightNode.index = index;
            index = rightIndex;
        }
        nodes[index] = node;
        node.index = index;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BinaryHeap)) {
            return false;
        }
        BinaryHeap other = (BinaryHeap)obj;
        if (other.size != this.size) {
            return false;
        }
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            if (other.nodes[i].value == this.nodes[i].value) continue;
            return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 1;
        int n = this.size;
        for (int i = 0; i < n; ++i) {
            h = h * 31 + Float.floatToIntBits(this.nodes[i].value);
        }
        return h;
    }

    public String toString() {
        if (this.size == 0) {
            return "[]";
        }
        Node[] nodes = this.nodes;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(nodes[0].value);
        for (int i = 1; i < this.size; ++i) {
            buffer.append(", ");
            buffer.append(nodes[i].value);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public static class Node {
        float value;
        int index;

        public Node(float value) {
            this.value = value;
        }

        public float getValue() {
            return this.value;
        }

        public String toString() {
            return Float.toString(this.value);
        }
    }
}

