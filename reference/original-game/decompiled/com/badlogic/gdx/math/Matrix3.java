/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.Serializable;

public class Matrix3
implements Serializable {
    private static final long serialVersionUID = 7907569533774959788L;
    public static final int M00 = 0;
    public static final int M01 = 3;
    public static final int M02 = 6;
    public static final int M10 = 1;
    public static final int M11 = 4;
    public static final int M12 = 7;
    public static final int M20 = 2;
    public static final int M21 = 5;
    public static final int M22 = 8;
    public float[] val = new float[9];
    private float[] tmp = new float[9];

    public Matrix3() {
        this.idt();
    }

    public Matrix3(Matrix3 matrix) {
        this.set(matrix);
    }

    public Matrix3(float[] values) {
        this.set(values);
    }

    public Matrix3 idt() {
        float[] val = this.val;
        val[0] = 1.0f;
        val[1] = 0.0f;
        val[2] = 0.0f;
        val[3] = 0.0f;
        val[4] = 1.0f;
        val[5] = 0.0f;
        val[6] = 0.0f;
        val[7] = 0.0f;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 mul(Matrix3 m) {
        float[] val = this.val;
        float v00 = val[0] * m.val[0] + val[3] * m.val[1] + val[6] * m.val[2];
        float v01 = val[0] * m.val[3] + val[3] * m.val[4] + val[6] * m.val[5];
        float v02 = val[0] * m.val[6] + val[3] * m.val[7] + val[6] * m.val[8];
        float v10 = val[1] * m.val[0] + val[4] * m.val[1] + val[7] * m.val[2];
        float v11 = val[1] * m.val[3] + val[4] * m.val[4] + val[7] * m.val[5];
        float v12 = val[1] * m.val[6] + val[4] * m.val[7] + val[7] * m.val[8];
        float v20 = val[2] * m.val[0] + val[5] * m.val[1] + val[8] * m.val[2];
        float v21 = val[2] * m.val[3] + val[5] * m.val[4] + val[8] * m.val[5];
        float v22 = val[2] * m.val[6] + val[5] * m.val[7] + val[8] * m.val[8];
        val[0] = v00;
        val[1] = v10;
        val[2] = v20;
        val[3] = v01;
        val[4] = v11;
        val[5] = v21;
        val[6] = v02;
        val[7] = v12;
        val[8] = v22;
        return this;
    }

    public Matrix3 mulLeft(Matrix3 m) {
        float[] val = this.val;
        float v00 = m.val[0] * val[0] + m.val[3] * val[1] + m.val[6] * val[2];
        float v01 = m.val[0] * val[3] + m.val[3] * val[4] + m.val[6] * val[5];
        float v02 = m.val[0] * val[6] + m.val[3] * val[7] + m.val[6] * val[8];
        float v10 = m.val[1] * val[0] + m.val[4] * val[1] + m.val[7] * val[2];
        float v11 = m.val[1] * val[3] + m.val[4] * val[4] + m.val[7] * val[5];
        float v12 = m.val[1] * val[6] + m.val[4] * val[7] + m.val[7] * val[8];
        float v20 = m.val[2] * val[0] + m.val[5] * val[1] + m.val[8] * val[2];
        float v21 = m.val[2] * val[3] + m.val[5] * val[4] + m.val[8] * val[5];
        float v22 = m.val[2] * val[6] + m.val[5] * val[7] + m.val[8] * val[8];
        val[0] = v00;
        val[1] = v10;
        val[2] = v20;
        val[3] = v01;
        val[4] = v11;
        val[5] = v21;
        val[6] = v02;
        val[7] = v12;
        val[8] = v22;
        return this;
    }

    public Matrix3 setToRotation(float degrees) {
        return this.setToRotationRad((float)Math.PI / 180 * degrees);
    }

    public Matrix3 setToRotationRad(float radians) {
        float cos = (float)Math.cos(radians);
        float sin = (float)Math.sin(radians);
        float[] val = this.val;
        val[0] = cos;
        val[1] = sin;
        val[2] = 0.0f;
        val[3] = -sin;
        val[4] = cos;
        val[5] = 0.0f;
        val[6] = 0.0f;
        val[7] = 0.0f;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 setToRotation(Vector3 axis, float degrees) {
        return this.setToRotation(axis, MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees));
    }

    public Matrix3 setToRotation(Vector3 axis, float cos, float sin) {
        float[] val = this.val;
        float oc = 1.0f - cos;
        val[0] = oc * axis.x * axis.x + cos;
        val[1] = oc * axis.x * axis.y - axis.z * sin;
        val[2] = oc * axis.z * axis.x + axis.y * sin;
        val[3] = oc * axis.x * axis.y + axis.z * sin;
        val[4] = oc * axis.y * axis.y + cos;
        val[5] = oc * axis.y * axis.z - axis.x * sin;
        val[6] = oc * axis.z * axis.x - axis.y * sin;
        val[7] = oc * axis.y * axis.z + axis.x * sin;
        val[8] = oc * axis.z * axis.z + cos;
        return this;
    }

    public Matrix3 setToTranslation(float x, float y) {
        float[] val = this.val;
        val[0] = 1.0f;
        val[1] = 0.0f;
        val[2] = 0.0f;
        val[3] = 0.0f;
        val[4] = 1.0f;
        val[5] = 0.0f;
        val[6] = x;
        val[7] = y;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 setToTranslation(Vector2 translation) {
        float[] val = this.val;
        val[0] = 1.0f;
        val[1] = 0.0f;
        val[2] = 0.0f;
        val[3] = 0.0f;
        val[4] = 1.0f;
        val[5] = 0.0f;
        val[6] = translation.x;
        val[7] = translation.y;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 setToScaling(float scaleX, float scaleY) {
        float[] val = this.val;
        val[0] = scaleX;
        val[1] = 0.0f;
        val[2] = 0.0f;
        val[3] = 0.0f;
        val[4] = scaleY;
        val[5] = 0.0f;
        val[6] = 0.0f;
        val[7] = 0.0f;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 setToScaling(Vector2 scale) {
        float[] val = this.val;
        val[0] = scale.x;
        val[1] = 0.0f;
        val[2] = 0.0f;
        val[3] = 0.0f;
        val[4] = scale.y;
        val[5] = 0.0f;
        val[6] = 0.0f;
        val[7] = 0.0f;
        val[8] = 1.0f;
        return this;
    }

    public String toString() {
        float[] val = this.val;
        return "[" + val[0] + "|" + val[3] + "|" + val[6] + "]\n[" + val[1] + "|" + val[4] + "|" + val[7] + "]\n[" + val[2] + "|" + val[5] + "|" + val[8] + "]";
    }

    public float det() {
        float[] val = this.val;
        return val[0] * val[4] * val[8] + val[3] * val[7] * val[2] + val[6] * val[1] * val[5] - val[0] * val[7] * val[5] - val[3] * val[1] * val[8] - val[6] * val[4] * val[2];
    }

    public Matrix3 inv() {
        float det = this.det();
        if (det == 0.0f) {
            throw new GdxRuntimeException("Can't invert a singular matrix");
        }
        float inv_det = 1.0f / det;
        float[] tmp = this.tmp;
        float[] val = this.val;
        tmp[0] = val[4] * val[8] - val[5] * val[7];
        tmp[1] = val[2] * val[7] - val[1] * val[8];
        tmp[2] = val[1] * val[5] - val[2] * val[4];
        tmp[3] = val[5] * val[6] - val[3] * val[8];
        tmp[4] = val[0] * val[8] - val[2] * val[6];
        tmp[5] = val[2] * val[3] - val[0] * val[5];
        tmp[6] = val[3] * val[7] - val[4] * val[6];
        tmp[7] = val[1] * val[6] - val[0] * val[7];
        tmp[8] = val[0] * val[4] - val[1] * val[3];
        val[0] = inv_det * tmp[0];
        val[1] = inv_det * tmp[1];
        val[2] = inv_det * tmp[2];
        val[3] = inv_det * tmp[3];
        val[4] = inv_det * tmp[4];
        val[5] = inv_det * tmp[5];
        val[6] = inv_det * tmp[6];
        val[7] = inv_det * tmp[7];
        val[8] = inv_det * tmp[8];
        return this;
    }

    public Matrix3 set(Matrix3 mat) {
        System.arraycopy(mat.val, 0, this.val, 0, this.val.length);
        return this;
    }

    public Matrix3 set(Affine2 affine) {
        float[] val = this.val;
        val[0] = affine.m00;
        val[1] = affine.m10;
        val[2] = 0.0f;
        val[3] = affine.m01;
        val[4] = affine.m11;
        val[5] = 0.0f;
        val[6] = affine.m02;
        val[7] = affine.m12;
        val[8] = 1.0f;
        return this;
    }

    public Matrix3 set(Matrix4 mat) {
        float[] val = this.val;
        val[0] = mat.val[0];
        val[1] = mat.val[1];
        val[2] = mat.val[2];
        val[3] = mat.val[4];
        val[4] = mat.val[5];
        val[5] = mat.val[6];
        val[6] = mat.val[8];
        val[7] = mat.val[9];
        val[8] = mat.val[10];
        return this;
    }

    public Matrix3 set(float[] values) {
        System.arraycopy(values, 0, this.val, 0, this.val.length);
        return this;
    }

    public Matrix3 trn(Vector2 vector) {
        this.val[6] = this.val[6] + vector.x;
        this.val[7] = this.val[7] + vector.y;
        return this;
    }

    public Matrix3 trn(float x, float y) {
        this.val[6] = this.val[6] + x;
        this.val[7] = this.val[7] + y;
        return this;
    }

    public Matrix3 trn(Vector3 vector) {
        this.val[6] = this.val[6] + vector.x;
        this.val[7] = this.val[7] + vector.y;
        return this;
    }

    public Matrix3 translate(float x, float y) {
        float[] val = this.val;
        this.tmp[0] = 1.0f;
        this.tmp[1] = 0.0f;
        this.tmp[2] = 0.0f;
        this.tmp[3] = 0.0f;
        this.tmp[4] = 1.0f;
        this.tmp[5] = 0.0f;
        this.tmp[6] = x;
        this.tmp[7] = y;
        this.tmp[8] = 1.0f;
        Matrix3.mul(val, this.tmp);
        return this;
    }

    public Matrix3 translate(Vector2 translation) {
        float[] val = this.val;
        this.tmp[0] = 1.0f;
        this.tmp[1] = 0.0f;
        this.tmp[2] = 0.0f;
        this.tmp[3] = 0.0f;
        this.tmp[4] = 1.0f;
        this.tmp[5] = 0.0f;
        this.tmp[6] = translation.x;
        this.tmp[7] = translation.y;
        this.tmp[8] = 1.0f;
        Matrix3.mul(val, this.tmp);
        return this;
    }

    public Matrix3 rotate(float degrees) {
        return this.rotateRad((float)Math.PI / 180 * degrees);
    }

    public Matrix3 rotateRad(float radians) {
        if (radians == 0.0f) {
            return this;
        }
        float cos = (float)Math.cos(radians);
        float sin = (float)Math.sin(radians);
        float[] tmp = this.tmp;
        tmp[0] = cos;
        tmp[1] = sin;
        tmp[2] = 0.0f;
        tmp[3] = -sin;
        tmp[4] = cos;
        tmp[5] = 0.0f;
        tmp[6] = 0.0f;
        tmp[7] = 0.0f;
        tmp[8] = 1.0f;
        Matrix3.mul(this.val, tmp);
        return this;
    }

    public Matrix3 scale(float scaleX, float scaleY) {
        float[] tmp = this.tmp;
        tmp[0] = scaleX;
        tmp[1] = 0.0f;
        tmp[2] = 0.0f;
        tmp[3] = 0.0f;
        tmp[4] = scaleY;
        tmp[5] = 0.0f;
        tmp[6] = 0.0f;
        tmp[7] = 0.0f;
        tmp[8] = 1.0f;
        Matrix3.mul(this.val, tmp);
        return this;
    }

    public Matrix3 scale(Vector2 scale) {
        float[] tmp = this.tmp;
        tmp[0] = scale.x;
        tmp[1] = 0.0f;
        tmp[2] = 0.0f;
        tmp[3] = 0.0f;
        tmp[4] = scale.y;
        tmp[5] = 0.0f;
        tmp[6] = 0.0f;
        tmp[7] = 0.0f;
        tmp[8] = 1.0f;
        Matrix3.mul(this.val, tmp);
        return this;
    }

    public float[] getValues() {
        return this.val;
    }

    public Vector2 getTranslation(Vector2 position) {
        position.x = this.val[6];
        position.y = this.val[7];
        return position;
    }

    public Vector2 getScale(Vector2 scale) {
        float[] val = this.val;
        scale.x = (float)Math.sqrt(val[0] * val[0] + val[3] * val[3]);
        scale.y = (float)Math.sqrt(val[1] * val[1] + val[4] * val[4]);
        return scale;
    }

    public float getRotation() {
        return 57.295776f * (float)Math.atan2(this.val[1], this.val[0]);
    }

    public float getRotationRad() {
        return (float)Math.atan2(this.val[1], this.val[0]);
    }

    public Matrix3 scl(float scale) {
        this.val[0] = this.val[0] * scale;
        this.val[4] = this.val[4] * scale;
        return this;
    }

    public Matrix3 scl(Vector2 scale) {
        this.val[0] = this.val[0] * scale.x;
        this.val[4] = this.val[4] * scale.y;
        return this;
    }

    public Matrix3 scl(Vector3 scale) {
        this.val[0] = this.val[0] * scale.x;
        this.val[4] = this.val[4] * scale.y;
        return this;
    }

    public Matrix3 transpose() {
        float[] val = this.val;
        float v01 = val[1];
        float v02 = val[2];
        float v10 = val[3];
        float v12 = val[5];
        float v20 = val[6];
        float v21 = val[7];
        val[3] = v01;
        val[6] = v02;
        val[1] = v10;
        val[7] = v12;
        val[2] = v20;
        val[5] = v21;
        return this;
    }

    private static void mul(float[] mata, float[] matb) {
        float v00 = mata[0] * matb[0] + mata[3] * matb[1] + mata[6] * matb[2];
        float v01 = mata[0] * matb[3] + mata[3] * matb[4] + mata[6] * matb[5];
        float v02 = mata[0] * matb[6] + mata[3] * matb[7] + mata[6] * matb[8];
        float v10 = mata[1] * matb[0] + mata[4] * matb[1] + mata[7] * matb[2];
        float v11 = mata[1] * matb[3] + mata[4] * matb[4] + mata[7] * matb[5];
        float v12 = mata[1] * matb[6] + mata[4] * matb[7] + mata[7] * matb[8];
        float v20 = mata[2] * matb[0] + mata[5] * matb[1] + mata[8] * matb[2];
        float v21 = mata[2] * matb[3] + mata[5] * matb[4] + mata[8] * matb[5];
        float v22 = mata[2] * matb[6] + mata[5] * matb[7] + mata[8] * matb[8];
        mata[0] = v00;
        mata[1] = v10;
        mata[2] = v20;
        mata[3] = v01;
        mata[4] = v11;
        mata[5] = v21;
        mata[6] = v02;
        mata[7] = v12;
        mata[8] = v22;
    }
}

