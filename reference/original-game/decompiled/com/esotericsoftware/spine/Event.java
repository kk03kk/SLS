/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.esotericsoftware.spine.EventData;

public class Event {
    private final EventData data;
    int intValue;
    float floatValue;
    String stringValue;
    final float time;

    public Event(float time, EventData data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        this.time = time;
        this.data = data;
    }

    public int getInt() {
        return this.intValue;
    }

    public void setInt(int intValue) {
        this.intValue = intValue;
    }

    public float getFloat() {
        return this.floatValue;
    }

    public void setFloat(float floatValue) {
        this.floatValue = floatValue;
    }

    public String getString() {
        return this.stringValue;
    }

    public void setString(String stringValue) {
        this.stringValue = stringValue;
    }

    public float getTime() {
        return this.time;
    }

    public EventData getData() {
        return this.data;
    }

    public String toString() {
        return this.data.name;
    }
}

