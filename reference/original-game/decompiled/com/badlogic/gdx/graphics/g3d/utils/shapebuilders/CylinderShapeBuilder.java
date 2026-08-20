/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;

public class CylinderShapeBuilder
extends BaseShapeBuilder {
    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions) {
        CylinderShapeBuilder.build(builder, width, height, depth, divisions, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions, float angleFrom, float angleTo) {
        CylinderShapeBuilder.build(builder, width, height, depth, divisions, angleFrom, angleTo, true);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float depth, int divisions, float angleFrom, float angleTo, boolean close) {
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
        MeshPartBuilder.VertexInfo curr2 = vertTmp4.set(null, null, null, null);
        curr2.hasNormal = true;
        curr2.hasPosition = true;
        curr2.hasUV = true;
        short i3 = 0;
        short i4 = 0;
        builder.ensureVertices(2 * (divisions + 1));
        builder.ensureRectangleIndices(divisions);
        for (int i = 0; i <= divisions; ++i) {
            angle = ao + step * (float)i;
            u = 1.0f - us * (float)i;
            curr1.position.set(MathUtils.cos(angle) * hw, 0.0f, MathUtils.sin(angle) * hd);
            curr1.normal.set(curr1.position).nor();
            curr1.position.y = -hh;
            curr1.uv.set(u, 1.0f);
            curr2.position.set(curr1.position);
            curr2.normal.set(curr1.normal);
            curr2.position.y = hh;
            curr2.uv.set(u, 0.0f);
            short i2 = builder.vertex(curr1);
            short i1 = builder.vertex(curr2);
            if (i != 0) {
                builder.rect(i3, i1, i2, i4);
            }
            i4 = i2;
            i3 = i1;
        }
        if (close) {
            EllipseShapeBuilder.build(builder, width, depth, 0.0f, 0.0f, divisions, 0.0f, hh, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, angleFrom, angleTo);
            EllipseShapeBuilder.build(builder, width, depth, 0.0f, 0.0f, divisions, 0.0f, -hh, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 180.0f - angleTo, 180.0f - angleFrom);
        }
    }
}

