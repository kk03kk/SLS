/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class GradientColorValue
extends ParticleValue {
    private static float[] temp = new float[3];
    private float[] colors = new float[]{1.0f, 1.0f, 1.0f};
    public float[] timeline = new float[]{0.0f};

    public float[] getTimeline() {
        return this.timeline;
    }

    public void setTimeline(float[] timeline) {
        this.timeline = timeline;
    }

    public float[] getColors() {
        return this.colors;
    }

    public void setColors(float[] colors) {
        this.colors = colors;
    }

    public float[] getColor(float percent) {
        this.getColor(percent, temp, 0);
        return temp;
    }

    public void getColor(float percent, float[] out, int index) {
        int startIndex = 0;
        int endIndex = -1;
        float[] timeline = this.timeline;
        int n = timeline.length;
        int i = 1;
        while (i < n) {
            float t = timeline[i];
            if (t > percent) {
                endIndex = i;
                break;
            }
            startIndex = i++;
        }
        float startTime = timeline[startIndex];
        float r1 = this.colors[startIndex *= 3];
        float g1 = this.colors[startIndex + 1];
        float b1 = this.colors[startIndex + 2];
        if (endIndex == -1) {
            out[index] = r1;
            out[index + 1] = g1;
            out[index + 2] = b1;
            return;
        }
        float factor = (percent - startTime) / (timeline[endIndex] - startTime);
        out[index] = r1 + (this.colors[endIndex *= 3] - r1) * factor;
        out[index + 1] = g1 + (this.colors[endIndex + 1] - g1) * factor;
        out[index + 2] = b1 + (this.colors[endIndex + 2] - b1) * factor;
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("colors", this.colors);
        json.writeValue("timeline", this.timeline);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.colors = json.readValue("colors", float[].class, jsonData);
        this.timeline = json.readValue("timeline", float[].class, jsonData);
    }

    public void load(GradientColorValue value) {
        super.load(value);
        this.colors = new float[value.colors.length];
        System.arraycopy(value.colors, 0, this.colors, 0, this.colors.length);
        this.timeline = new float[value.timeline.length];
        System.arraycopy(value.timeline, 0, this.timeline, 0, this.timeline.length);
    }
}

