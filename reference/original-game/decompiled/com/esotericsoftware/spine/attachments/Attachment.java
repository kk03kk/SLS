/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

public abstract class Attachment {
    String name;

    public Attachment(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.getName();
    }
}

