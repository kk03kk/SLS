/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

public class DisplayOption
implements Comparable<DisplayOption> {
    public int width;
    public int height;
    public String aspectRatio = " TAB TAB";

    public DisplayOption(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public DisplayOption(int width, int height, boolean showAspectRatio) {
        this.width = width;
        this.height = height;
        if (showAspectRatio) {
            this.appendAspectRatio();
        }
    }

    private void appendAspectRatio() {
        float ratio = (float)this.width / (float)this.height;
        this.aspectRatio = ratio > 1.25f && ratio < 1.26f ? " (5:4)" : (ratio > 1.32f && ratio < 1.34f ? " (4:3)" : (ratio > 1.76f && ratio < 1.78f ? " (16:9)" : (ratio > 1.59f && ratio < 1.61f ? " (16:10)" : (ratio > 2.32f && ratio < 2.34f ? " (21:9)" : " (" + String.format("#.##", Float.valueOf(ratio)) + ")"))));
    }

    @Override
    public int compareTo(DisplayOption other) {
        if (this.width == other.width) {
            if (this.height == other.height) {
                return 0;
            }
            if (this.height < other.height) {
                return -1;
            }
            return 1;
        }
        if (this.width < other.width) {
            return -1;
        }
        return 1;
    }

    public boolean equals(Object other) {
        return ((DisplayOption)other).width == this.width && ((DisplayOption)other).height == this.height;
    }

    public String toString() {
        return "(" + this.width + "," + this.height + ")";
    }

    public String uiString() {
        return this.width + " x " + this.height + this.aspectRatio;
    }
}

