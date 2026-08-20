/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.Vector3;

public final class PointSpawnShapeValue
extends PrimitiveSpawnShapeValue {
    public PointSpawnShapeValue(PointSpawnShapeValue value) {
        super(value);
        this.load(value);
    }

    public PointSpawnShapeValue() {
    }

    @Override
    public void spawnAux(Vector3 vector, float percent) {
        vector.x = this.spawnWidth + this.spawnWidthDiff * this.spawnWidthValue.getScale(percent);
        vector.y = this.spawnHeight + this.spawnHeightDiff * this.spawnHeightValue.getScale(percent);
        vector.z = this.spawnDepth + this.spawnDepthDiff * this.spawnDepthValue.getScale(percent);
    }

    @Override
    public SpawnShapeValue copy() {
        return new PointSpawnShapeValue(this);
    }
}

