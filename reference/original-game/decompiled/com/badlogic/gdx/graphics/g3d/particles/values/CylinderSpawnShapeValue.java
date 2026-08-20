/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public final class CylinderSpawnShapeValue
extends PrimitiveSpawnShapeValue {
    public CylinderSpawnShapeValue(CylinderSpawnShapeValue cylinderSpawnShapeValue) {
        super(cylinderSpawnShapeValue);
        this.load(cylinderSpawnShapeValue);
    }

    public CylinderSpawnShapeValue() {
    }

    @Override
    public void spawnAux(Vector3 vector, float percent) {
        boolean isRadiusZZero;
        float radiusZ;
        float radiusX;
        float width = this.spawnWidth + this.spawnWidthDiff * this.spawnWidthValue.getScale(percent);
        float height = this.spawnHeight + this.spawnHeightDiff * this.spawnHeightValue.getScale(percent);
        float depth = this.spawnDepth + this.spawnDepthDiff * this.spawnDepthValue.getScale(percent);
        float hf = height / 2.0f;
        float ty = MathUtils.random(height) - hf;
        if (this.edges) {
            radiusX = width / 2.0f;
            radiusZ = depth / 2.0f;
        } else {
            radiusX = MathUtils.random(width) / 2.0f;
            radiusZ = MathUtils.random(depth) / 2.0f;
        }
        float spawnTheta = 0.0f;
        boolean isRadiusXZero = radiusX == 0.0f;
        boolean bl = isRadiusZZero = radiusZ == 0.0f;
        if (!isRadiusXZero && !isRadiusZZero) {
            spawnTheta = MathUtils.random(360.0f);
        } else if (isRadiusXZero) {
            spawnTheta = MathUtils.random(1) == 0 ? -90.0f : 90.0f;
        } else if (isRadiusZZero) {
            spawnTheta = MathUtils.random(1) == 0 ? 0.0f : 180.0f;
        }
        vector.set(radiusX * MathUtils.cosDeg(spawnTheta), ty, radiusZ * MathUtils.sinDeg(spawnTheta));
    }

    @Override
    public SpawnShapeValue copy() {
        return new CylinderSpawnShapeValue(this);
    }
}

