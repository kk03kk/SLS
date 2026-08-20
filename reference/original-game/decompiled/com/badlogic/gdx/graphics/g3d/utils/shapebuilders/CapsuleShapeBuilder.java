/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class CapsuleShapeBuilder
extends BaseShapeBuilder {
    public static void build(MeshPartBuilder builder, float radius, float height, int divisions) {
        if (height < 2.0f * radius) {
            throw new GdxRuntimeException("Height must be at least twice the radius");
        }
        float d = 2.0f * radius;
        CylinderShapeBuilder.build(builder, d, height - d, d, divisions, 0.0f, 360.0f, false);
        SphereShapeBuilder.build(builder, matTmp1.setToTranslation(0.0f, 0.5f * (height - d), 0.0f), d, d, d, divisions, divisions, 0.0f, 360.0f, 0.0f, 90.0f);
        SphereShapeBuilder.build(builder, matTmp1.setToTranslation(0.0f, -0.5f * (height - d), 0.0f), d, d, d, divisions, divisions, 0.0f, 360.0f, 90.0f, 180.0f);
    }
}

