/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;

public class ConeShapeBuilder
extends BaseShapeBuilder {
    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions) {
        ConeShapeBuilder.build(builder, width, height, depth, divisions, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions, float angleFrom, float angleTo) {
        ConeShapeBuilder.build(builder, width, height, depth, divisions, angleFrom, angleTo, true);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions, float angleFrom, float angleTo, boolean close) {
        builder.ensureVertices(divisions + 2);
        builder.ensureTriangleIndices(divisions);
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        float hd = depth * 0.5f;
        float ao = (float)Math.PI / 180 * angleFrom;
        float step = (float)Math.PI / 180 * (angleTo - angleFrom) / (float)divisions;
        float us = 1.0f / (float)divisions;
        float u = 0.0f;
        float angle = 0.0f;
        MeshPartBuilder.VertexInfo curr1 = vertTmp3.set(null, null, null, null);
        curr1.hasNormal = true;
        curr1.hasPosition = true;
        curr1.hasUV = true;
        MeshPartBuilder.VertexInfo curr2 = vertTmp4.set(null, null, null, null).setPos(0.0f, hh, 0.0f).setNor(0.0f, 1.0f, 0.0f).setUV(0.5f, 0.0f);
        short base = builder.vertex(curr2);
        short i2 = 0;
        for (int i = 0; i <= divisions; ++i) {
            angle = ao + step * (float)i;
            u = 1.0f - us * (float)i;
            curr1.position.set(MathUtils.cos(angle) * hw, 0.0f, MathUtils.sin(angle) * hd);
            curr1.normal.set(curr1.position).nor();
            curr1.position.y = -hh;
            curr1.uv.set(u, 1.0f);
            short i1 = builder.vertex(curr1);
            if (i != 0) {
                builder.triangle(base, i1, i2);
            }
            i2 = i1;
        }
        if (close) {
            EllipseShapeBuilder.build(builder, width, depth, 0.0f, 0.0f, divisions, 0.0f, -hh, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 180.0f - angleTo, 180.0f - angleFrom);
        }
    }
}

