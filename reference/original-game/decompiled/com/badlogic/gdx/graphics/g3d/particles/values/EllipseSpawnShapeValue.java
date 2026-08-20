/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public final class EllipseSpawnShapeValue
extends PrimitiveSpawnShapeValue {
    PrimitiveSpawnShapeValue.SpawnSide side = PrimitiveSpawnShapeValue.SpawnSide.both;

    public EllipseSpawnShapeValue(EllipseSpawnShapeValue value) {
        super(value);
        this.load(value);
    }

    public EllipseSpawnShapeValue() {
    }

    @Override
    public void spawnAux(Vector3 vector, float percent) {
        float radiusZ;
        float radiusY;
        float radiusX;
        float width = this.spawnWidth + this.spawnWidthDiff * this.spawnWidthValue.getScale(percent);
        float height = this.spawnHeight + this.spawnHeightDiff * this.spawnHeightValue.getScale(percent);
        float depth = this.spawnDepth + this.spawnDepthDiff * this.spawnDepthValue.getScale(percent);
        float minT = 0.0f;
        float maxT = (float)Math.PI * 2;
        if (this.side == PrimitiveSpawnShapeValue.SpawnSide.top) {
            maxT = (float)Math.PI;
        } else if (this.side == PrimitiveSpawnShapeValue.SpawnSide.bottom) {
            maxT = (float)(-Math.PI);
        }
        float t = MathUtils.random(minT, maxT);
        if (this.edges) {
            if (width == 0.0f) {
                vector.set(0.0f, height / 2.0f * MathUtils.sin(t), depth / 2.0f * MathUtils.cos(t));
                return;
            }
            if (height == 0.0f) {
                vector.set(width / 2.0f * MathUtils.cos(t), 0.0f, depth / 2.0f * MathUtils.sin(t));
                return;
            }
            if (depth == 0.0f) {
                vector.set(width / 2.0f * MathUtils.cos(t), height / 2.0f * MathUtils.sin(t), 0.0f);
                return;
            }
            radiusX = width / 2.0f;
            radiusY = height / 2.0f;
            radiusZ = depth / 2.0f;
        } else {
            radiusX = MathUtils.random(width / 2.0f);
            radiusY = MathUtils.random(height / 2.0f);
            radiusZ = MathUtils.random(depth / 2.0f);
        }
        float z = MathUtils.random(-1.0f, 1.0f);
        float r = (float)Math.sqrt(1.0f - z * z);
        vector.set(radiusX * r * MathUtils.cos(t), radiusY * r * MathUtils.sin(t), radiusZ * z);
    }

    public PrimitiveSpawnShapeValue.SpawnSide getSide() {
        return this.side;
    }

    public void setSide(PrimitiveSpawnShapeValue.SpawnSide side) {
        this.side = side;
    }

    @Override
    public void load(ParticleValue value) {
        super.load(value);
        EllipseSpawnShapeValue shape = (EllipseSpawnShapeValue)value;
        this.side = shape.side;
    }

    @Override
    public SpawnShapeValue copy() {
        return new EllipseSpawnShapeValue(this);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("side", (Object)this.side);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.side = json.readValue("side", PrimitiveSpawnShapeValue.SpawnSide.class, jsonData);
    }
}

