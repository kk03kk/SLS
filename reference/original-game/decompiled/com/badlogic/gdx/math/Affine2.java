/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.Serializable;

public final class Affine2
implements Serializable {
    private static final long serialVersionUID = 1524569123485049187L;
    public float m00 = 1.0f;
    public float m01 = 0.0f;
    public float m02 = 0.0f;
    public float m10 = 0.0f;
    public float m11 = 1.0f;
    public float m12 = 0.0f;

    public Affine2() {
    }

    public Affine2(Affine2 other) {
        this.set(other);
    }

    public Affine2 idt() {
        this.m00 = 1.0f;
        this.m01 = 0.0f;
        this.m02 = 0.0f;
        this.m10 = 0.0f;
        this.m11 = 1.0f;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 set(Affine2 other) {
        this.m00 = other.m00;
        this.m01 = other.m01;
        this.m02 = other.m02;
        this.m10 = other.m10;
        this.m11 = other.m11;
        this.m12 = other.m12;
        return this;
    }

    public Affine2 set(Matrix3 matrix) {
        float[] other = matrix.val;
        this.m00 = other[0];
        this.m01 = other[3];
        this.m02 = other[6];
        this.m10 = other[1];
        this.m11 = other[4];
        this.m12 = other[7];
        return this;
    }

    public Affine2 set(Matrix4 matrix) {
        float[] other = matrix.val;
        this.m00 = other[0];
        this.m01 = other[4];
        this.m02 = other[12];
        this.m10 = other[1];
        this.m11 = other[5];
        this.m12 = other[13];
        return this;
    }

    public Affine2 setToTranslation(float x, float y) {
        this.m00 = 1.0f;
        this.m01 = 0.0f;
        this.m02 = x;
        this.m10 = 0.0f;
        this.m11 = 1.0f;
        this.m12 = y;
        return this;
    }

    public Affine2 setToTranslation(Vector2 trn) {
        return this.setToTranslation(trn.x, trn.y);
    }

    public Affine2 setToScaling(float scaleX, float scaleY) {
        this.m00 = scaleX;
        this.m01 = 0.0f;
        this.m02 = 0.0f;
        this.m10 = 0.0f;
        this.m11 = scaleY;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 setToScaling(Vector2 scale) {
        return this.setToScaling(scale.x, scale.y);
    }

    public Affine2 setToRotation(float degrees) {
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        this.m00 = cos;
        this.m01 = -sin;
        this.m02 = 0.0f;
        this.m10 = sin;
        this.m11 = cos;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 setToRotationRad(float radians) {
        float cos = MathUtils.cos(radians);
        float sin = MathUtils.sin(radians);
        this.m00 = cos;
        this.m01 = -sin;
        this.m02 = 0.0f;
        this.m10 = sin;
        this.m11 = cos;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 setToRotation(float cos, float sin) {
        this.m00 = cos;
        this.m01 = -sin;
        this.m02 = 0.0f;
        this.m10 = sin;
        this.m11 = cos;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 setToShearing(float shearX, float shearY) {
        this.m00 = 1.0f;
        this.m01 = shearX;
        this.m02 = 0.0f;
        this.m10 = shearY;
        this.m11 = 1.0f;
        this.m12 = 0.0f;
        return this;
    }

    public Affine2 setToShearing(Vector2 shear) {
        return this.setToShearing(shear.x, shear.y);
    }

    public Affine2 setToTrnRotScl(float x, float y, float degrees, float scaleX, float scaleY) {
        this.m02 = x;
        this.m12 = y;
        if (degrees == 0.0f) {
            this.m00 = scaleX;
            this.m01 = 0.0f;
            this.m10 = 0.0f;
            this.m11 = scaleY;
        } else {
            float sin = MathUtils.sinDeg(degrees);
            float cos = MathUtils.cosDeg(degrees);
            this.m00 = cos * scaleX;
            this.m01 = -sin * scaleY;
            this.m10 = sin * scaleX;
            this.m11 = cos * scaleY;
        }
        return this;
    }

    public Affine2 setToTrnRotScl(Vector2 trn, float degrees, Vector2 scale) {
        return this.setToTrnRotScl(trn.x, trn.y, degrees, scale.x, scale.y);
    }

    public Affine2 setToTrnRotRadScl(float x, float y, float radians, float scaleX, float scaleY) {
        this.m02 = x;
        this.m12 = y;
        if (radians == 0.0f) {
            this.m00 = scaleX;
            this.m01 = 0.0f;
            this.m10 = 0.0f;
            this.m11 = scaleY;
        } else {
            float sin = MathUtils.sin(radians);
            float cos = MathUtils.cos(radians);
            this.m00 = cos * scaleX;
            this.m01 = -sin * scaleY;
            this.m10 = sin * scaleX;
            this.m11 = cos * scaleY;
        }
        return this;
    }

    public Affine2 setToTrnRotRadScl(Vector2 trn, float radians, Vector2 scale) {
        return this.setToTrnRotRadScl(trn.x, trn.y, radians, scale.x, scale.y);
    }

    public Affine2 setToTrnScl(float x, float y, float scaleX, float scaleY) {
        this.m00 = scaleX;
        this.m01 = 0.0f;
        this.m02 = x;
        this.m10 = 0.0f;
        this.m11 = scaleY;
        this.m12 = y;
        return this;
    }

    public Affine2 setToTrnScl(Vector2 trn, Vector2 scale) {
        return this.setToTrnScl(trn.x, trn.y, scale.x, scale.y);
    }

    public Affine2 setToProduct(Affine2 l, Affine2 r) {
        this.m00 = l.m00 * r.m00 + l.m01 * r.m10;
        this.m01 = l.m00 * r.m01 + l.m01 * r.m11;
        this.m02 = l.m00 * r.m02 + l.m01 * r.m12 + l.m02;
        this.m10 = l.m10 * r.m00 + l.m11 * r.m10;
        this.m11 = l.m10 * r.m01 + l.m11 * r.m11;
        this.m12 = l.m10 * r.m02 + l.m11 * r.m12 + l.m12;
        return this;
    }

    public Affine2 inv() {
        float det = this.det();
        if (det == 0.0f) {
            throw new GdxRuntimeException("Can't invert a singular affine matrix");
        }
        float invDet = 1.0f / det;
        float tmp00 = this.m11;
        float tmp01 = -this.m01;
        float tmp02 = this.m01 * this.m12 - this.m11 * this.m02;
        float tmp10 = -this.m10;
        float tmp11 = this.m00;
        float tmp12 = this.m10 * this.m02 - this.m00 * this.m12;
        this.m00 = invDet * tmp00;
        this.m01 = invDet * tmp01;
        this.m02 = invDet * tmp02;
        this.m10 = invDet * tmp10;
        this.m11 = invDet * tmp11;
        this.m12 = invDet * tmp12;
        return this;
    }

    public Affine2 mul(Affine2 other) {
        float tmp00 = this.m00 * other.m00 + this.m01 * other.m10;
        float tmp01 = this.m00 * other.m01 + this.m01 * other.m11;
        float tmp02 = this.m00 * other.m02 + this.m01 * other.m12 + this.m02;
        float tmp10 = this.m10 * other.m00 + this.m11 * other.m10;
        float tmp11 = this.m10 * other.m01 + this.m11 * other.m11;
        float tmp12 = this.m10 * other.m02 + this.m11 * other.m12 + this.m12;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m02 = tmp02;
        this.m10 = tmp10;
        this.m11 = tmp11;
        this.m12 = tmp12;
        return this;
    }

    public Affine2 preMul(Affine2 other) {
        float tmp00 = other.m00 * this.m00 + other.m01 * this.m10;
        float tmp01 = other.m00 * this.m01 + other.m01 * this.m11;
        float tmp02 = other.m00 * this.m02 + other.m01 * this.m12 + other.m02;
        float tmp10 = other.m10 * this.m00 + other.m11 * this.m10;
        float tmp11 = other.m10 * this.m01 + other.m11 * this.m11;
        float tmp12 = other.m10 * this.m02 + other.m11 * this.m12 + other.m12;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m02 = tmp02;
        this.m10 = tmp10;
        this.m11 = tmp11;
        this.m12 = tmp12;
        return this;
    }

    public Affine2 translate(float x, float y) {
        this.m02 += this.m00 * x + this.m01 * y;
        this.m12 += this.m10 * x + this.m11 * y;
        return this;
    }

    public Affine2 translate(Vector2 trn) {
        return this.translate(trn.x, trn.y);
    }

    public Affine2 preTranslate(float x, float y) {
        this.m02 += x;
        this.m12 += y;
        return this;
    }

    public Affine2 preTranslate(Vector2 trn) {
        return this.preTranslate(trn.x, trn.y);
    }

    public Affine2 scale(float scaleX, float scaleY) {
        this.m00 *= scaleX;
        this.m01 *= scaleY;
        this.m10 *= scaleX;
        this.m11 *= scaleY;
        return this;
    }

    public Affine2 scale(Vector2 scale) {
        return this.scale(scale.x, scale.y);
    }

    public Affine2 preScale(float scaleX, float scaleY) {
        this.m00 *= scaleX;
        this.m01 *= scaleX;
        this.m02 *= scaleX;
        this.m10 *= scaleY;
        this.m11 *= scaleY;
        this.m12 *= scaleY;
        return this;
    }

    public Affine2 preScale(Vector2 scale) {
        return this.preScale(scale.x, scale.y);
    }

    public Affine2 rotate(float degrees) {
        if (degrees == 0.0f) {
            return this;
        }
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        float tmp00 = this.m00 * cos + this.m01 * sin;
        float tmp01 = this.m00 * -sin + this.m01 * cos;
        float tmp10 = this.m10 * cos + this.m11 * sin;
        float tmp11 = this.m10 * -sin + this.m11 * cos;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m10 = tmp10;
        this.m11 = tmp11;
        return this;
    }

    public Affine2 rotateRad(float radians) {
        if (radians == 0.0f) {
            return this;
        }
        float cos = MathUtils.cos(radians);
        float sin = MathUtils.sin(radians);
        float tmp00 = this.m00 * cos + this.m01 * sin;
        float tmp01 = this.m00 * -sin + this.m01 * cos;
        float tmp10 = this.m10 * cos + this.m11 * sin;
        float tmp11 = this.m10 * -sin + this.m11 * cos;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m10 = tmp10;
        this.m11 = tmp11;
        return this;
    }

    public Affine2 preRotate(float degrees) {
        if (degrees == 0.0f) {
            return this;
        }
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        float tmp00 = cos * this.m00 - sin * this.m10;
        float tmp01 = cos * this.m01 - sin * this.m11;
        float tmp02 = cos * this.m02 - sin * this.m12;
        float tmp10 = sin * this.m00 + cos * this.m10;
        float tmp11 = sin * this.m01 + cos * this.m11;
        float tmp12 = sin * this.m02 + cos * this.m12;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m02 = tmp02;
        this.m10 = tmp10;
        this.m11 = tmp11;
        this.m12 = tmp12;
        return this;
    }

    public Affine2 preRotateRad(float radians) {
        if (radians == 0.0f) {
            return this;
        }
        float cos = MathUtils.cos(radians);
        float sin = MathUtils.sin(radians);
        float tmp00 = cos * this.m00 - sin * this.m10;
        float tmp01 = cos * this.m01 - sin * this.m11;
        float tmp02 = cos * this.m02 - sin * this.m12;
        float tmp10 = sin * this.m00 + cos * this.m10;
        float tmp11 = sin * this.m01 + cos * this.m11;
        float tmp12 = sin * this.m02 + cos * this.m12;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m02 = tmp02;
        this.m10 = tmp10;
        this.m11 = tmp11;
        this.m12 = tmp12;
        return this;
    }

    public Affine2 shear(float shearX, float shearY) {
        float tmp0 = this.m00 + shearY * this.m01;
        float tmp1 = this.m01 + shearX * this.m00;
        this.m00 = tmp0;
        this.m01 = tmp1;
        tmp0 = this.m10 + shearY * this.m11;
        tmp1 = this.m11 + shearX * this.m10;
        this.m10 = tmp0;
        this.m11 = tmp1;
        return this;
    }

    public Affine2 shear(Vector2 shear) {
        return this.shear(shear.x, shear.y);
    }

    public Affine2 preShear(float shearX, float shearY) {
        float tmp00 = this.m00 + shearX * this.m10;
        float tmp01 = this.m01 + shearX * this.m11;
        float tmp02 = this.m02 + shearX * this.m12;
        float tmp10 = this.m10 + shearY * this.m00;
        float tmp11 = this.m11 + shearY * this.m01;
        float tmp12 = this.m12 + shearY * this.m02;
        this.m00 = tmp00;
        this.m01 = tmp01;
        this.m02 = tmp02;
        this.m10 = tmp10;
        this.m11 = tmp11;
        this.m12 = tmp12;
        return this;
    }

    public Affine2 preShear(Vector2 shear) {
        return this.preShear(shear.x, shear.y);
    }

    public float det() {
        return this.m00 * this.m11 - this.m01 * this.m10;
    }

    public Vector2 getTranslation(Vector2 position) {
        position.x = this.m02;
        position.y = this.m12;
        return position;
    }

    public boolean isTranslation() {
        return this.m00 == 1.0f && this.m11 == 1.0f && this.m01 == 0.0f && this.m10 == 0.0f;
    }

    public boolean isIdt() {
        return this.m00 == 1.0f && this.m02 == 0.0f && this.m12 == 0.0f && this.m11 == 1.0f && this.m01 == 0.0f && this.m10 == 0.0f;
    }

    public void applyTo(Vector2 point) {
        float x = point.x;
        float y = point.y;
        point.x = this.m00 * x + this.m01 * y + this.m02;
        point.y = this.m10 * x + this.m11 * y + this.m12;
    }

    public String toString() {
        return "[" + this.m00 + "|" + this.m01 + "|" + this.m02 + "]\n[" + this.m10 + "|" + this.m11 + "|" + this.m12 + "]\n[0.0|0.0|0.1]";
    }
}

