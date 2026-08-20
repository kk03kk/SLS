/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Updatable;

public class Bone
implements Updatable {
    final BoneData data;
    final Skeleton skeleton;
    final Bone parent;
    final Array<Bone> children = new Array();
    float x;
    float y;
    float rotation;
    float scaleX;
    float scaleY;
    float shearX;
    float shearY;
    float appliedRotation;
    float a;
    float b;
    float worldX;
    float c;
    float d;
    float worldY;
    float worldSignX;
    float worldSignY;
    boolean sorted;

    public Bone(BoneData data, Skeleton skeleton, Bone parent) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = data;
        this.skeleton = skeleton;
        this.parent = parent;
        this.setToSetupPose();
    }

    public Bone(Bone bone, Skeleton skeleton, Bone parent) {
        if (bone == null) {
            throw new IllegalArgumentException("bone cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.skeleton = skeleton;
        this.parent = parent;
        this.data = bone.data;
        this.x = bone.x;
        this.y = bone.y;
        this.rotation = bone.rotation;
        this.scaleX = bone.scaleX;
        this.scaleY = bone.scaleY;
        this.shearX = bone.shearX;
        this.shearY = bone.shearY;
    }

    @Override
    public void update() {
        this.updateWorldTransform(this.x, this.y, this.rotation, this.scaleX, this.scaleY, this.shearX, this.shearY);
    }

    public void updateWorldTransform() {
        this.updateWorldTransform(this.x, this.y, this.rotation, this.scaleX, this.scaleY, this.shearX, this.shearY);
    }

    public void updateWorldTransform(float x, float y, float rotation, float scaleX, float scaleY, float shearX, float shearY) {
        this.appliedRotation = rotation;
        float rotationY = rotation + 90.0f + shearY;
        float la = MathUtils.cosDeg(rotation + shearX) * scaleX;
        float lb = MathUtils.cosDeg(rotationY) * scaleY;
        float lc = MathUtils.sinDeg(rotation + shearX) * scaleX;
        float ld = MathUtils.sinDeg(rotationY) * scaleY;
        Bone parent = this.parent;
        if (parent == null) {
            Skeleton skeleton = this.skeleton;
            if (skeleton.flipX) {
                x = -x;
                la = -la;
                lb = -lb;
            }
            if (skeleton.flipY) {
                y = -y;
                lc = -lc;
                ld = -ld;
            }
            this.a = la;
            this.b = lb;
            this.c = lc;
            this.d = ld;
            this.worldX = x;
            this.worldY = y;
            this.worldSignX = Math.signum(scaleX);
            this.worldSignY = Math.signum(scaleY);
            return;
        }
        float pa = parent.a;
        float pb = parent.b;
        float pc = parent.c;
        float pd = parent.d;
        this.worldX = pa * x + pb * y + parent.worldX;
        this.worldY = pc * x + pd * y + parent.worldY;
        this.worldSignX = parent.worldSignX * Math.signum(scaleX);
        this.worldSignY = parent.worldSignY * Math.signum(scaleY);
        if (this.data.inheritRotation && this.data.inheritScale) {
            this.a = pa * la + pb * lc;
            this.b = pa * lb + pb * ld;
            this.c = pc * la + pd * lc;
            this.d = pc * lb + pd * ld;
        } else {
            if (this.data.inheritRotation) {
                pa = 1.0f;
                pb = 0.0f;
                pc = 0.0f;
                pd = 1.0f;
                do {
                    float cos = MathUtils.cosDeg(parent.appliedRotation);
                    float sin = MathUtils.sinDeg(parent.appliedRotation);
                    float temp = pa * cos + pb * sin;
                    pb = pb * cos - pa * sin;
                    pa = temp;
                    temp = pc * cos + pd * sin;
                    pd = pd * cos - pc * sin;
                    pc = temp;
                } while (parent.data.inheritRotation && (parent = parent.parent) != null);
                this.a = pa * la + pb * lc;
                this.b = pa * lb + pb * ld;
                this.c = pc * la + pd * lc;
                this.d = pc * lb + pd * ld;
            } else if (this.data.inheritScale) {
                pa = 1.0f;
                pb = 0.0f;
                pc = 0.0f;
                pd = 1.0f;
                do {
                    float cos = MathUtils.cosDeg(parent.appliedRotation);
                    float sin = MathUtils.sinDeg(parent.appliedRotation);
                    float psx = parent.scaleX;
                    float psy = parent.scaleY;
                    float za = cos * psx;
                    float zb = sin * psy;
                    float zc = sin * psx;
                    float zd = cos * psy;
                    float temp = pa * za + pb * zc;
                    pb = pb * zd - pa * zb;
                    pa = temp;
                    temp = pc * za + pd * zc;
                    pd = pd * zd - pc * zb;
                    pc = temp;
                    if (psx >= 0.0f) {
                        sin = -sin;
                    }
                    temp = pa * cos + pb * sin;
                    pb = pb * cos - pa * sin;
                    pa = temp;
                    temp = pc * cos + pd * sin;
                    pd = pd * cos - pc * sin;
                    pc = temp;
                } while (parent.data.inheritScale && (parent = parent.parent) != null);
                this.a = pa * la + pb * lc;
                this.b = pa * lb + pb * ld;
                this.c = pc * la + pd * lc;
                this.d = pc * lb + pd * ld;
            } else {
                this.a = la;
                this.b = lb;
                this.c = lc;
                this.d = ld;
            }
            if (this.skeleton.flipX) {
                this.a = -this.a;
                this.b = -this.b;
            }
            if (this.skeleton.flipY) {
                this.c = -this.c;
                this.d = -this.d;
            }
        }
    }

    public void setToSetupPose() {
        BoneData data = this.data;
        this.x = data.x;
        this.y = data.y;
        this.rotation = data.rotation;
        this.scaleX = data.scaleX;
        this.scaleY = data.scaleY;
        this.shearX = data.shearX;
        this.shearY = data.shearY;
    }

    public BoneData getData() {
        return this.data;
    }

    public Skeleton getSkeleton() {
        return this.skeleton;
    }

    public Bone getParent() {
        return this.parent;
    }

    public Array<Bone> getChildren() {
        return this.children;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
    }

    public float getShearX() {
        return this.shearX;
    }

    public void setShearX(float shearX) {
        this.shearX = shearX;
    }

    public float getShearY() {
        return this.shearY;
    }

    public void setShearY(float shearY) {
        this.shearY = shearY;
    }

    public float getA() {
        return this.a;
    }

    public float getB() {
        return this.b;
    }

    public float getC() {
        return this.c;
    }

    public float getD() {
        return this.d;
    }

    public float getWorldX() {
        return this.worldX;
    }

    public float getWorldY() {
        return this.worldY;
    }

    public float getWorldSignX() {
        return this.worldSignX;
    }

    public float getWorldSignY() {
        return this.worldSignY;
    }

    public float getWorldRotationX() {
        return MathUtils.atan2(this.c, this.a) * 57.295776f;
    }

    public float getWorldRotationY() {
        return MathUtils.atan2(this.d, this.b) * 57.295776f;
    }

    public float getWorldScaleX() {
        return (float)Math.sqrt(this.a * this.a + this.b * this.b) * this.worldSignX;
    }

    public float getWorldScaleY() {
        return (float)Math.sqrt(this.c * this.c + this.d * this.d) * this.worldSignY;
    }

    public float worldToLocalRotationX() {
        Bone parent = this.parent;
        if (parent == null) {
            return this.rotation;
        }
        float pa = parent.a;
        float pb = parent.b;
        float pc = parent.c;
        float pd = parent.d;
        float a = this.a;
        float c = this.c;
        return MathUtils.atan2(pa * c - pc * a, pd * a - pb * c) * 57.295776f;
    }

    public float worldToLocalRotationY() {
        Bone parent = this.parent;
        if (parent == null) {
            return this.rotation;
        }
        float pa = parent.a;
        float pb = parent.b;
        float pc = parent.c;
        float pd = parent.d;
        float b = this.b;
        float d = this.d;
        return MathUtils.atan2(pa * d - pc * b, pd * b - pb * d) * 57.295776f;
    }

    public void rotateWorld(float degrees) {
        float a = this.a;
        float b = this.b;
        float c = this.c;
        float d = this.d;
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        this.a = cos * a - sin * c;
        this.b = cos * b - sin * d;
        this.c = sin * a + cos * c;
        this.d = sin * b + cos * d;
    }

    public void updateLocalTransform() {
        Bone parent = this.parent;
        if (parent == null) {
            this.x = this.worldX;
            this.y = this.worldY;
            this.rotation = MathUtils.atan2(this.c, this.a) * 57.295776f;
            this.scaleX = (float)Math.sqrt(this.a * this.a + this.c * this.c);
            this.scaleY = (float)Math.sqrt(this.b * this.b + this.d * this.d);
            float det = this.a * this.d - this.b * this.c;
            this.shearX = 0.0f;
            this.shearY = MathUtils.atan2(this.a * this.b + this.c * this.d, det) * 57.295776f;
            return;
        }
        float pa = parent.a;
        float pb = parent.b;
        float pc = parent.c;
        float pd = parent.d;
        float pid = 1.0f / (pa * pd - pb * pc);
        float dx = this.worldX - parent.worldX;
        float dy = this.worldY - parent.worldY;
        this.x = dx * pd * pid - dy * pb * pid;
        this.y = dy * pa * pid - dx * pc * pid;
        float ia = pid * pd;
        float id = pid * pa;
        float ib = pid * pb;
        float ic = pid * pc;
        float ra = ia * this.a - ib * this.c;
        float rb = ia * this.b - ib * this.d;
        float rc = id * this.c - ic * this.a;
        float rd = id * this.d - ic * this.b;
        this.shearX = 0.0f;
        this.scaleX = (float)Math.sqrt(ra * ra + rc * rc);
        if (this.scaleX > 1.0E-4f) {
            float det = ra * rd - rb * rc;
            this.scaleY = det / this.scaleX;
            this.shearY = MathUtils.atan2(ra * rb + rc * rd, det) * 57.295776f;
            this.rotation = MathUtils.atan2(rc, ra) * 57.295776f;
        } else {
            this.scaleX = 0.0f;
            this.scaleY = (float)Math.sqrt(rb * rb + rd * rd);
            this.shearY = 0.0f;
            this.rotation = 90.0f - MathUtils.atan2(rd, rb) * 57.295776f;
        }
        this.appliedRotation = this.rotation;
    }

    public Matrix3 getWorldTransform(Matrix3 worldTransform) {
        if (worldTransform == null) {
            throw new IllegalArgumentException("worldTransform cannot be null.");
        }
        float[] val = worldTransform.val;
        val[0] = this.a;
        val[3] = this.b;
        val[1] = this.c;
        val[4] = this.d;
        val[6] = this.worldX;
        val[7] = this.worldY;
        val[2] = 0.0f;
        val[5] = 0.0f;
        val[8] = 1.0f;
        return worldTransform;
    }

    public Vector2 worldToLocal(Vector2 world) {
        float a = this.a;
        float b = this.b;
        float c = this.c;
        float d = this.d;
        float invDet = 1.0f / (a * d - b * c);
        float x = world.x - this.worldX;
        float y = world.y - this.worldY;
        world.x = x * d * invDet - y * b * invDet;
        world.y = y * a * invDet - x * c * invDet;
        return world;
    }

    public Vector2 localToWorld(Vector2 local) {
        float x = local.x;
        float y = local.y;
        local.x = x * this.a + y * this.b + this.worldX;
        local.y = x * this.c + y * this.d + this.worldY;
        return local;
    }

    public String toString() {
        return this.data.name;
    }
}

