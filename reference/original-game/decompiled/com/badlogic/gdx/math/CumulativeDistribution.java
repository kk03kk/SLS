/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class CumulativeDistribution<T> {
    private Array<CumulativeValue> values = new Array(false, 10, CumulativeValue.class);

    public void add(T value, float intervalSize) {
        this.values.add(new CumulativeValue(value, 0.0f, intervalSize));
    }

    public void add(T value) {
        this.values.add(new CumulativeValue(value, 0.0f, 0.0f));
    }

    public void generate() {
        float sum = 0.0f;
        for (int i = 0; i < this.values.size; ++i) {
            ((CumulativeValue[])this.values.items)[i].frequency = sum += ((CumulativeValue[])this.values.items)[i].interval;
        }
    }

    public void generateNormalized() {
        float sum = 0.0f;
        for (int i = 0; i < this.values.size; ++i) {
            sum += ((CumulativeValue[])this.values.items)[i].interval;
        }
        float intervalSum = 0.0f;
        for (int i = 0; i < this.values.size; ++i) {
            ((CumulativeValue[])this.values.items)[i].frequency = intervalSum += ((CumulativeValue[])this.values.items)[i].interval / sum;
        }
    }

    public void generateUniform() {
        float freq = 1.0f / (float)this.values.size;
        for (int i = 0; i < this.values.size; ++i) {
            ((CumulativeValue[])this.values.items)[i].interval = freq;
            ((CumulativeValue[])this.values.items)[i].frequency = (float)(i + 1) * freq;
        }
    }

    public T value(float probability) {
        CumulativeValue value = null;
        int imax = this.values.size - 1;
        int imin = 0;
        while (imin <= imax) {
            int imid = imin + (imax - imin) / 2;
            value = ((CumulativeValue[])this.values.items)[imid];
            if (probability < value.frequency) {
                imax = imid - 1;
                continue;
            }
            if (!(probability > value.frequency)) break;
            imin = imid + 1;
        }
        return ((CumulativeValue[])this.values.items)[imin].value;
    }

    public T value() {
        return this.value(MathUtils.random());
    }

    public int size() {
        return this.values.size;
    }

    public float getInterval(int index) {
        return ((CumulativeValue[])this.values.items)[index].interval;
    }

    public T getValue(int index) {
        return ((CumulativeValue[])this.values.items)[index].value;
    }

    public void setInterval(T obj, float intervalSize) {
        for (CumulativeValue value : this.values) {
            if (value.value != obj) continue;
            value.interval = intervalSize;
            return;
        }
    }

    public void setInterval(int index, float intervalSize) {
        ((CumulativeValue[])this.values.items)[index].interval = intervalSize;
    }

    public void clear() {
        this.values.clear();
    }

    public class CumulativeValue {
        public T value;
        public float frequency;
        public float interval;

        public CumulativeValue(T value, float frequency, float interval) {
            this.value = value;
            this.frequency = frequency;
            this.interval = interval;
        }
    }
}

