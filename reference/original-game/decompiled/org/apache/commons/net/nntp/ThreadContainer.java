/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import org.apache.commons.net.nntp.Threadable;

class ThreadContainer {
    Threadable threadable;
    ThreadContainer parent;
    ThreadContainer next;
    ThreadContainer child;

    ThreadContainer() {
    }

    boolean findChild(ThreadContainer target) {
        if (this.child == null) {
            return false;
        }
        if (this.child == target) {
            return true;
        }
        return this.child.findChild(target);
    }

    void flush() {
        if (this.parent != null && this.threadable == null) {
            throw new RuntimeException("no threadable in " + this.toString());
        }
        this.parent = null;
        if (this.threadable != null) {
            this.threadable.setChild(this.child == null ? null : this.child.threadable);
        }
        if (this.child != null) {
            this.child.flush();
            this.child = null;
        }
        if (this.threadable != null) {
            this.threadable.setNext(this.next == null ? null : this.next.threadable);
        }
        if (this.next != null) {
            this.next.flush();
            this.next = null;
        }
        this.threadable = null;
    }

    void reverseChildren() {
        if (this.child != null) {
            ThreadContainer prev = null;
            ThreadContainer kid = this.child;
            ThreadContainer rest = kid.next;
            while (kid != null) {
                kid.next = prev;
                prev = kid;
                kid = rest;
                rest = rest == null ? null : rest.next;
            }
            kid = this.child = prev;
            while (kid != null) {
                kid.reverseChildren();
                kid = kid.next;
            }
        }
    }
}

