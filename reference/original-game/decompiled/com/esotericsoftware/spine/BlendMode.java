/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

public enum BlendMode {
    normal(770, 1, 771),
    additive(770, 1, 1),
    multiply(774, 774, 771),
    screen(1, 1, 769);

    int source;
    int sourcePMA;
    int dest;
    public static BlendMode[] values;

    private BlendMode(int source, int sourcePremultipledAlpha, int dest) {
        this.source = source;
        this.sourcePMA = sourcePremultipledAlpha;
        this.dest = dest;
    }

    public int getSource(boolean premultipliedAlpha) {
        return premultipliedAlpha ? this.sourcePMA : this.source;
    }

    public int getDest() {
        return this.dest;
    }

    static {
        values = BlendMode.values();
    }
}

