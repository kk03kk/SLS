/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class EllipseShapeBuilder
extends BaseShapeBuilder {
    public static void build(MeshPartBuilder builder, float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build(builder, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal) {
        EllipseShapeBuilder.build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z, binormal.x, binormal.y, binormal.z);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ) {
        EllipseShapeBuilder.build(builder, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, radius * 2.0f, radius * 2.0f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, Vector3 center, Vector3 normal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z, binormal.x, binormal.y, binormal.z, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, radius * 2.0f, radius * 2.0f, 0.0f, 0.0f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build(builder, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build(builder, width, height, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal) {
        EllipseShapeBuilder.build(builder, width, height, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z, binormal.x, binormal.y, binormal.z);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ) {
        EllipseShapeBuilder.build(builder, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, width, height, 0.0f, 0.0f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, Vector3 center, Vector3 normal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, width, height, 0.0f, 0.0f, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, width, height, 0.0f, 0.0f, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z, binormal.x, binormal.y, binormal.z, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(builder, width, height, 0.0f, 0.0f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        tmpV1.set(normalX, normalY, normalZ).crs(0.0f, 0.0f, 1.0f);
        tmpV2.set(normalX, normalY, normalZ).crs(0.0f, 1.0f, 0.0f);
        if (tmpV2.len2() > tmpV1.len2()) {
            tmpV1.set(tmpV2);
        }
        tmpV2.set(tmpV1.nor()).crs(normalX, normalY, normalZ).nor();
        EllipseShapeBuilder.build(builder, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, EllipseShapeBuilder.tmpV1.x, EllipseShapeBuilder.tmpV1.y, EllipseShapeBuilder.tmpV1.z, EllipseShapeBuilder.tmpV2.x, EllipseShapeBuilder.tmpV2.y, EllipseShapeBuilder.tmpV2.z, angleFrom, angleTo);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build(builder, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float innerWidth, float innerHeight, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build(builder, width, height, innerWidth, innerHeight, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, 0.0f, 360.0f);
    }

    public static void build(MeshPartBuilder builder, float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        if (innerWidth <= 0.0f || innerHeight <= 0.0f) {
            builder.ensureVertices(divisions + 2);
            builder.ensureTriangleIndices(divisions);
        } else if (innerWidth == width && innerHeight == height) {
            builder.ensureVertices(divisions + 1);
            builder.ensureIndices(divisions + 1);
            if (builder.getPrimitiveType() != 1) {
                throw new GdxRuntimeException("Incorrect primitive type : expect GL_LINES because innerWidth == width && innerHeight == height");
            }
        } else {
            builder.ensureVertices((divisions + 1) * 2);
            builder.ensureRectangleIndices(divisions + 1);
        }
        float ao = (float)Math.PI / 180 * angleFrom;
        float step = (float)Math.PI / 180 * (angleTo - angleFrom) / (float)divisions;
        Vector3 sxEx = tmpV1.set(tangentX, tangentY, tangentZ).scl(width * 0.5f);
        Vector3 syEx = tmpV2.set(binormalX, binormalY, binormalZ).scl(height * 0.5f);
        Vector3 sxIn = tmpV3.set(tangentX, tangentY, tangentZ).scl(innerWidth * 0.5f);
        Vector3 syIn = tmpV4.set(binormalX, binormalY, binormalZ).scl(innerHeight * 0.5f);
        MeshPartBuilder.VertexInfo currIn = vertTmp3.set(null, null, null, null);
        currIn.hasNormal = true;
        currIn.hasPosition = true;
        currIn.hasUV = true;
        currIn.uv.set(0.5f, 0.5f);
        currIn.position.set(centerX, centerY, centerZ);
        currIn.normal.set(normalX, normalY, normalZ);
        MeshPartBuilder.VertexInfo currEx = vertTmp4.set(null, null, null, null);
        currEx.hasNormal = true;
        currEx.hasPosition = true;
        currEx.hasUV = true;
        currEx.uv.set(0.5f, 0.5f);
        currEx.position.set(centerX, centerY, centerZ);
        currEx.normal.set(normalX, normalY, normalZ);
        short center = builder.vertex(currEx);
        float angle = 0.0f;
        float us = 0.5f * (innerWidth / width);
        float vs = 0.5f * (innerHeight / height);
        short i2 = 0;
        short i3 = 0;
        short i4 = 0;
        for (int i = 0; i <= divisions; ++i) {
            angle = ao + step * (float)i;
            float x = MathUtils.cos(angle);
            float y = MathUtils.sin(angle);
            currEx.position.set(centerX, centerY, centerZ).add(sxEx.x * x + syEx.x * y, sxEx.y * x + syEx.y * y, sxEx.z * x + syEx.z * y);
            currEx.uv.set(0.5f + 0.5f * x, 0.5f + 0.5f * y);
            short i1 = builder.vertex(currEx);
            if (innerWidth <= 0.0f || innerHeight <= 0.0f) {
                if (i != 0) {
                    builder.triangle(i1, i2, center);
                }
                i2 = i1;
                continue;
            }
            if (innerWidth == width && innerHeight == height) {
                if (i != 0) {
                    builder.line(i1, i2);
                }
                i2 = i1;
                continue;
            }
            currIn.position.set(centerX, centerY, centerZ).add(sxIn.x * x + syIn.x * y, sxIn.y * x + syIn.y * y, sxIn.z * x + syIn.z * y);
            currIn.uv.set(0.5f + us * x, 0.5f + vs * y);
            i2 = i1;
            i1 = builder.vertex(currIn);
            if (i != 0) {
                builder.rect(i1, i2, i4, i3);
            }
            i4 = i2;
            i3 = i1;
        }
    }
}

