/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import java.util.LinkedList;

class OutputQueue {
    private final LinkedList<String> queue = new LinkedList();

    OutputQueue() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void add(String s) {
        LinkedList<String> linkedList = this.queue;
        synchronized (linkedList) {
            this.queue.add(s);
            this.queue.notify();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addFirst(String s) {
        LinkedList<String> linkedList = this.queue;
        synchronized (linkedList) {
            this.queue.addFirst(s);
            this.queue.notify();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String next() {
        LinkedList<String> linkedList = this.queue;
        synchronized (linkedList) {
            if (!this.hasNext()) {
                try {
                    this.queue.wait();
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
            if (!this.hasNext()) {
                return null;
            }
            String message = this.queue.getFirst();
            this.queue.removeFirst();
            return message;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean hasNext() {
        LinkedList<String> linkedList = this.queue;
        synchronized (linkedList) {
            return this.queue.size() > 0;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void releaseWaitingThreads() {
        LinkedList<String> linkedList = this.queue;
        synchronized (linkedList) {
            this.queue.notifyAll();
        }
    }
}

