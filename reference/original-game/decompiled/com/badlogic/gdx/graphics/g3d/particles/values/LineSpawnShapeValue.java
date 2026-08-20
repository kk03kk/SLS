/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public final class LineSpawnShapeValue
extends PrimitiveSpawnShapeValue {
    public LineSpawnShapeValue(LineSpawnShapeValue value) {
        super(value);
        this.load(value);
    }

    public LineSpawnShapeValue() {
    }

    @Override
    public void spawnAux(Vector3 vector, float percent) {
        float width = this.spawnWidth + this.spawnWidthDiff * this.spawnWidthValue.getScale(percent);
        float height = this.spawnHeight + this.spawnHeightDiff * this.spawnHeightValue.getScale(percent);
        float depth = this.spawnDepth + this.spawnDepthDiff * this.spawnDepthValue.getScale(percent);
        float a = MathUtils.random();
        vector.x = a * width;
        vector.y = a * height;
        vector.z = a * depth;
    }

    @Override
    public SpawnShapeValue copy() {
        return new LineSpawnShapeValue(this);
    }
}

