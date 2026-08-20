/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class NumericValue
extends ParticleValue {
    private float value;

    public float getValue() {
        return this.value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void load(NumericValue value) {
        super.load(value);
        this.value = value.value;
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("value", Float.valueOf(this.value));
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.value = json.readValue("value", Float.TYPE, jsonData).floatValue();
    }
}

