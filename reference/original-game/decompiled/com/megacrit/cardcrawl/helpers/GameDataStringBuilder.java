/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

public class GameDataStringBuilder {
    private StringBuilder bldr = new StringBuilder();

    public void addFieldData(String value) {
        this.bldr.append(value).append("\t");
    }

    public void addFieldData(int value) {
        this.addFieldData(Integer.toString(value));
    }

    public void addFieldData(boolean value) {
        this.addFieldData(Boolean.toString(value));
    }

    public String toString() {
        return this.bldr.toString();
    }
}

