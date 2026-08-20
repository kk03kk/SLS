/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ShortArray;

public class SphereShapeBuilder
extends BaseShapeBuilder {
    private static final ShortArray tmpIndices = new ShortArray();

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisionsU, int divisionsV) {
        SphereShapeBuilder.build(builder, width, height, depth, divisionsU, divisionsV, 0.0f, 360.0f, 0.0f, 180.0f);
    }

    @Deprecated
    public static void build(MeshPartBuilder builder, Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV) {
        SphereShapeBuilder.build(builder, transform, width, height, depth, divisionsU, divisionsV, 0.0f, 360.0f, 0.0f, 180.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        SphereShapeBuilder.build(builder, matTmp1.idt(), width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo);
    }

    @Deprecated
    public static void build(MeshPartBuilder builder, Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        float hd = depth * 0.5f;
        float auo = (float)Math.PI / 180 * angleUFrom;
        float stepU = (float)Math.PI / 180 * (angleUTo - angleUFrom) / (float)divisionsU;
        float avo = (float)Math.PI / 180 * angleVFrom;
        float stepV = (float)Math.PI / 180 * (angleVTo - angleVFrom) / (float)divisionsV;
        float us = 1.0f / (float)divisionsU;
        float vs = 1.0f / (float)divisionsV;
        float u = 0.0f;
        float v = 0.0f;
        float angleU = 0.0f;
        float angleV = 0.0f;
        MeshPartBuilder.VertexInfo curr1 = vertTmp3.set(null, null, null, null);
        curr1.hasNormal = true;
        curr1.hasPosition = true;
        curr1.hasUV = true;
        int s = divisionsU + 3;
        tmpIndices.clear();
        tmpIndices.ensureCapacity(divisionsU * 2);
        SphereShapeBuilder.tmpIndices.size = s;
        int tempOffset = 0;
        builder.ensureVertices((divisionsV + 1) * (divisionsU + 1));
        builder.ensureRectangleIndices(divisionsU);
        for (int iv = 0; iv <= divisionsV; ++iv) {
            angleV = avo + stepV * (float)iv;
            v = vs * (float)iv;
            float t = MathUtils.sin(angleV);
            float h = MathUtils.cos(angleV) * hh;
            for (int iu = 0; iu <= divisionsU; ++iu) {
                angleU = auo + stepU * (float)iu;
                u = 1.0f - us * (float)iu;
                curr1.position.set(MathUtils.cos(angleU) * hw * t, h, MathUtils.sin(angleU) * hd * t).mul(transform);
                curr1.normal.set(curr1.position).nor();
                curr1.uv.set(u, v);
                tmpIndices.set(tempOffset, builder.vertex(curr1));
                int o = tempOffset + s;
                if (iv > 0 && iu > 0) {
                    builder.rect(tmpIndices.get(tempOffset), tmpIndices.get((o - 1) % s), tmpIndices.get((o - (divisionsU + 2)) % s), tmpIndices.get((o - (divisionsU + 1)) % s));
                }
                tempOffset = (tempOffset + 1) % SphereShapeBuilder.tmpIndices.size;
            }
        }
    }
}

