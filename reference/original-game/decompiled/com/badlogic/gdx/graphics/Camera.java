/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public abstract class Camera {
    public final Vector3 position = new Vector3();
    public final Vector3 direction = new Vector3(0.0f, 0.0f, -1.0f);
    public final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
    public final Matrix4 projection = new Matrix4();
    public final Matrix4 view = new Matrix4();
    public final Matrix4 combined = new Matrix4();
    public final Matrix4 invProjectionView = new Matrix4();
    public float near = 1.0f;
    public float far = 100.0f;
    public float viewportWidth = 0.0f;
    public float viewportHeight = 0.0f;
    public final Frustum frustum = new Frustum();
    private final Vector3 tmpVec = new Vector3();
    private final Ray ray = new Ray(new Vector3(), new Vector3());

    public abstract void update();

    public abstract void update(boolean var1);

    public void lookAt(float x, float y, float z) {
        this.tmpVec.set(x, y, z).sub(this.position).nor();
        if (!this.tmpVec.isZero()) {
            float dot = this.tmpVec.dot(this.up);
            if (Math.abs(dot - 1.0f) < 1.0E-9f) {
                this.up.set(this.direction).scl(-1.0f);
            } else if (Math.abs(dot + 1.0f) < 1.0E-9f) {
                this.up.set(this.direction);
            }
            this.direction.set(this.tmpVec);
            this.normalizeUp();
        }
    }

    public void lookAt(Vector3 target) {
        this.lookAt(target.x, target.y, target.z);
    }

    public void normalizeUp() {
        this.tmpVec.set(this.direction).crs(this.up).nor();
        this.up.set(this.tmpVec).crs(this.direction).nor();
    }

    public void rotate(float angle, float axisX, float axisY, float axisZ) {
        this.direction.rotate(angle, axisX, axisY, axisZ);
        this.up.rotate(angle, axisX, axisY, axisZ);
    }

    public void rotate(Vector3 axis, float angle) {
        this.direction.rotate(axis, angle);
        this.up.rotate(axis, angle);
    }

    public void rotate(Matrix4 transform) {
        this.direction.rot(transform);
        this.up.rot(transform);
    }

    public void rotate(Quaternion quat) {
        quat.transform(this.direction);
        quat.transform(this.up);
    }

    public void rotateAround(Vector3 point, Vector3 axis, float angle) {
        this.tmpVec.set(point);
        this.tmpVec.sub(this.position);
        this.translate(this.tmpVec);
        this.rotate(axis, angle);
        this.tmpVec.rotate(axis, angle);
        this.translate(-this.tmpVec.x, -this.tmpVec.y, -this.tmpVec.z);
    }

    public void transform(Matrix4 transform) {
        this.position.mul(transform);
        this.rotate(transform);
    }

    public void translate(float x, float y, float z) {
        this.position.add(x, y, z);
    }

    public void translate(Vector3 vec) {
        this.position.add(vec);
    }

    public Vector3 unproject(Vector3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
        float x = screenCoords.x;
        float y = screenCoords.y;
        y = (float)Gdx.graphics.getHeight() - y - 1.0f;
        screenCoords.x = 2.0f * (x -= viewportX) / viewportWidth - 1.0f;
        screenCoords.y = 2.0f * (y -= viewportY) / viewportHeight - 1.0f;
        screenCoords.z = 2.0f * screenCoords.z - 1.0f;
        screenCoords.prj(this.invProjectionView);
        return screenCoords;
    }

    public Vector3 unproject(Vector3 screenCoords) {
        this.unproject(screenCoords, 0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return screenCoords;
    }

    public Vector3 project(Vector3 worldCoords) {
        this.project(worldCoords, 0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return worldCoords;
    }

    public Vector3 project(Vector3 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
        worldCoords.prj(this.combined);
        worldCoords.x = viewportWidth * (worldCoords.x + 1.0f) / 2.0f + viewportX;
        worldCoords.y = viewportHeight * (worldCoords.y + 1.0f) / 2.0f + viewportY;
        worldCoords.z = (worldCoords.z + 1.0f) / 2.0f;
        return worldCoords;
    }

    public Ray getPickRay(float screenX, float screenY, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
        this.unproject(this.ray.origin.set(screenX, screenY, 0.0f), viewportX, viewportY, viewportWidth, viewportHeight);
        this.unproject(this.ray.direction.set(screenX, screenY, 1.0f), viewportX, viewportY, viewportWidth, viewportHeight);
        this.ray.direction.sub(this.ray.origin).nor();
        return this.ray;
    }

    public Ray getPickRay(float screenX, float screenY) {
        return this.getPickRay(screenX, screenY, 0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
}

