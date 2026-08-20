/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.WindowedMean;

public class FloatCounter {
    public int count;
    public float total;
    public float min;
    public float max;
    public float average;
    public float latest;
    public float value;
    public final WindowedMean mean;

    public FloatCounter(int windowSize) {
        this.mean = windowSize > 1 ? new WindowedMean(windowSize) : null;
        this.reset();
    }

    public void put(float value) {
        this.latest = value;
        this.total += value;
        ++this.count;
        this.average = this.total / (float)this.count;
        if (this.mean != null) {
            this.mean.addValue(value);
            this.value = this.mean.getMean();
        } else {
            this.value = this.latest;
        }
        if (this.mean == null || this.mean.hasEnoughData()) {
            if (this.value < this.min) {
                this.min = this.value;
            }
            if (this.value > this.max) {
                this.max = this.value;
            }
        }
    }

    public void reset() {
        this.count = 0;
        this.total = 0.0f;
        this.min = Float.MAX_VALUE;
        this.max = Float.MIN_VALUE;
        this.average = 0.0f;
        this.latest = 0.0f;
        this.value = 0.0f;
        if (this.mean != null) {
            this.mean.clear();
        }
    }
}

