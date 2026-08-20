/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector3;

public class FrustumShapeBuilder
extends BaseShapeBuilder {
    public static void build(MeshPartBuilder builder, Camera camera) {
        FrustumShapeBuilder.build(builder, camera, tmpColor0.set(1.0f, 0.66f, 0.0f, 1.0f), tmpColor1.set(1.0f, 0.0f, 0.0f, 1.0f), tmpColor2.set(0.0f, 0.66f, 1.0f, 1.0f), tmpColor3.set(1.0f, 1.0f, 1.0f, 1.0f), tmpColor4.set(0.2f, 0.2f, 0.2f, 1.0f));
    }

    public static void build(MeshPartBuilder builder, Camera camera, Color frustumColor, Color coneColor, Color upColor, Color targetColor, Color crossColor) {
        Vector3[] planePoints = camera.frustum.planePoints;
        FrustumShapeBuilder.build(builder, camera.frustum, frustumColor, crossColor);
        builder.line(planePoints[0], coneColor, camera.position, coneColor);
        builder.line(planePoints[1], coneColor, camera.position, coneColor);
        builder.line(planePoints[2], coneColor, camera.position, coneColor);
        builder.line(planePoints[3], coneColor, camera.position, coneColor);
        builder.line(camera.position, targetColor, FrustumShapeBuilder.centerPoint(planePoints[4], planePoints[5], planePoints[6]), targetColor);
        float halfNearSize = tmpV0.set(planePoints[1]).sub(planePoints[0]).scl(0.5f).len();
        Vector3 centerNear = FrustumShapeBuilder.centerPoint(planePoints[0], planePoints[1], planePoints[2]);
        tmpV0.set(camera.up).scl(halfNearSize * 2.0f);
        centerNear.add(tmpV0);
        builder.line(centerNear, upColor, planePoints[2], upColor);
        builder.line(planePoints[2], upColor, planePoints[3], upColor);
        builder.line(planePoints[3], upColor, centerNear, upColor);
    }

    public static void build(MeshPartBuilder builder, Frustum frustum, Color frustumColor, Color crossColor) {
        Vector3[] planePoints = frustum.planePoints;
        builder.line(planePoints[0], frustumColor, planePoints[1], frustumColor);
        builder.line(planePoints[1], frustumColor, planePoints[2], frustumColor);
        builder.line(planePoints[2], frustumColor, planePoints[3], frustumColor);
        builder.line(planePoints[3], frustumColor, planePoints[0], frustumColor);
        builder.line(planePoints[4], frustumColor, planePoints[5], frustumColor);
        builder.line(planePoints[5], frustumColor, planePoints[6], frustumColor);
        builder.line(planePoints[6], frustumColor, planePoints[7], frustumColor);
        builder.line(planePoints[7], frustumColor, planePoints[4], frustumColor);
        builder.line(planePoints[0], frustumColor, planePoints[4], frustumColor);
        builder.line(planePoints[1], frustumColor, planePoints[5], frustumColor);
        builder.line(planePoints[2], frustumColor, planePoints[6], frustumColor);
        builder.line(planePoints[3], frustumColor, planePoints[7], frustumColor);
        builder.line(FrustumShapeBuilder.middlePoint(planePoints[1], planePoints[0]), crossColor, FrustumShapeBuilder.middlePoint(planePoints[3], planePoints[2]), crossColor);
        builder.line(FrustumShapeBuilder.middlePoint(planePoints[2], planePoints[1]), crossColor, FrustumShapeBuilder.middlePoint(planePoints[3], planePoints[0]), crossColor);
        builder.line(FrustumShapeBuilder.middlePoint(planePoints[5], planePoints[4]), crossColor, FrustumShapeBuilder.middlePoint(planePoints[7], planePoints[6]), crossColor);
        builder.line(FrustumShapeBuilder.middlePoint(planePoints[6], planePoints[5]), crossColor, FrustumShapeBuilder.middlePoint(planePoints[7], planePoints[4]), crossColor);
    }

    private static Vector3 middlePoint(Vector3 point0, Vector3 point1) {
        tmpV0.set(point1).sub(point0).scl(0.5f);
        return tmpV1.set(point0).add(tmpV0);
    }

    private static Vector3 centerPoint(Vector3 point0, Vector3 point1, Vector3 point2) {
        tmpV0.set(point1).sub(point0).scl(0.5f);
        tmpV1.set(point0).add(tmpV0);
        tmpV0.set(point2).sub(point1).scl(0.5f);
        return tmpV1.add(tmpV0);
    }
}

