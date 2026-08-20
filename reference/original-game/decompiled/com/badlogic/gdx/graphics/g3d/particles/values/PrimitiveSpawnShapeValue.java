/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class PrimitiveSpawnShapeValue
extends SpawnShapeValue {
    protected static final Vector3 TMP_V1 = new Vector3();
    public ScaledNumericValue spawnWidthValue = new ScaledNumericValue();
    public ScaledNumericValue spawnHeightValue = new ScaledNumericValue();
    public ScaledNumericValue spawnDepthValue = new ScaledNumericValue();
    protected float spawnWidth;
    protected float spawnWidthDiff;
    protected float spawnHeight;
    protected float spawnHeightDiff;
    protected float spawnDepth;
    protected float spawnDepthDiff;
    boolean edges = false;

    public PrimitiveSpawnShapeValue() {
    }

    public PrimitiveSpawnShapeValue(PrimitiveSpawnShapeValue value) {
        super(value);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        this.spawnWidthValue.setActive(true);
        this.spawnHeightValue.setActive(true);
        this.spawnDepthValue.setActive(true);
    }

    public boolean isEdges() {
        return this.edges;
    }

    public void setEdges(boolean edges) {
        this.edges = edges;
    }

    public ScaledNumericValue getSpawnWidth() {
        return this.spawnWidthValue;
    }

    public ScaledNumericValue getSpawnHeight() {
        return this.spawnHeightValue;
    }

    public ScaledNumericValue getSpawnDepth() {
        return this.spawnDepthValue;
    }

    public void setDimensions(float width, float height, float depth) {
        this.spawnWidthValue.setHigh(width);
        this.spawnHeightValue.setHigh(height);
        this.spawnDepthValue.setHigh(depth);
    }

    @Override
    public void start() {
        this.spawnWidth = this.spawnWidthValue.newLowValue();
        this.spawnWidthDiff = this.spawnWidthValue.newHighValue();
        if (!this.spawnWidthValue.isRelative()) {
            this.spawnWidthDiff -= this.spawnWidth;
        }
        this.spawnHeight = this.spawnHeightValue.newLowValue();
        this.spawnHeightDiff = this.spawnHeightValue.newHighValue();
        if (!this.spawnHeightValue.isRelative()) {
            this.spawnHeightDiff -= this.spawnHeight;
        }
        this.spawnDepth = this.spawnDepthValue.newLowValue();
        this.spawnDepthDiff = this.spawnDepthValue.newHighValue();
        if (!this.spawnDepthValue.isRelative()) {
            this.spawnDepthDiff -= this.spawnDepth;
        }
    }

    @Override
    public void load(ParticleValue value) {
        super.load(value);
        PrimitiveSpawnShapeValue shape = (PrimitiveSpawnShapeValue)value;
        this.edges = shape.edges;
        this.spawnWidthValue.load(shape.spawnWidthValue);
        this.spawnHeightValue.load(shape.spawnHeightValue);
        this.spawnDepthValue.load(shape.spawnDepthValue);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("spawnWidthValue", this.spawnWidthValue);
        json.writeValue("spawnHeightValue", this.spawnHeightValue);
        json.writeValue("spawnDepthValue", this.spawnDepthValue);
        json.writeValue("edges", this.edges);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.spawnWidthValue = json.readValue("spawnWidthValue", ScaledNumericValue.class, jsonData);
        this.spawnHeightValue = json.readValue("spawnHeightValue", ScaledNumericValue.class, jsonData);
        this.spawnDepthValue = json.readValue("spawnDepthValue", ScaledNumericValue.class, jsonData);
        this.edges = json.readValue("edges", Boolean.TYPE, jsonData);
    }

    public static enum SpawnSide {
        both,
        top,
        bottom;

    }
}

