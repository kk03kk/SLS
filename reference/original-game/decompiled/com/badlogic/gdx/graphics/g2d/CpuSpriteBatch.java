/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class CpuSpriteBatch
extends SpriteBatch {
    private final Matrix4 virtualMatrix = new Matrix4();
    private final Affine2 adjustAffine = new Affine2();
    private boolean adjustNeeded;
    private boolean haveIdentityRealMatrix = true;
    private final Affine2 tmpAffine = new Affine2();

    public CpuSpriteBatch() {
        this(1000);
    }

    public CpuSpriteBatch(int size) {
        this(size, null);
    }

    public CpuSpriteBatch(int size, ShaderProgram defaultShader) {
        super(size, defaultShader);
    }

    public void flushAndSyncTransformMatrix() {
        this.flush();
        if (this.adjustNeeded) {
            this.haveIdentityRealMatrix = CpuSpriteBatch.checkIdt(this.virtualMatrix);
            if (!this.haveIdentityRealMatrix && this.virtualMatrix.det() == 0.0f) {
                throw new GdxRuntimeException("Transform matrix is singular, can't sync");
            }
            this.adjustNeeded = false;
            super.setTransformMatrix(this.virtualMatrix);
        }
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return this.adjustNeeded ? this.virtualMatrix : super.getTransformMatrix();
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        Matrix4 realMatrix = super.getTransformMatrix();
        if (CpuSpriteBatch.checkEqual(realMatrix, transform)) {
            this.adjustNeeded = false;
        } else if (this.isDrawing()) {
            this.virtualMatrix.setAsAffine(transform);
            this.adjustNeeded = true;
            if (this.haveIdentityRealMatrix) {
                this.adjustAffine.set(transform);
            } else {
                this.tmpAffine.set(transform);
                this.adjustAffine.set(realMatrix).inv().mul(this.tmpAffine);
            }
        } else {
            realMatrix.setAsAffine(transform);
            this.haveIdentityRealMatrix = CpuSpriteBatch.checkIdt(realMatrix);
        }
    }

    public void setTransformMatrix(Affine2 transform) {
        Matrix4 realMatrix = super.getTransformMatrix();
        if (CpuSpriteBatch.checkEqual(realMatrix, transform)) {
            this.adjustNeeded = false;
        } else {
            this.virtualMatrix.setAsAffine(transform);
            if (this.isDrawing()) {
                this.adjustNeeded = true;
                if (this.haveIdentityRealMatrix) {
                    this.adjustAffine.set(transform);
                } else {
                    this.adjustAffine.set(realMatrix).inv().mul(transform);
                }
            } else {
                realMatrix.setAsAffine(transform);
                this.haveIdentityRealMatrix = CpuSpriteBatch.checkIdt(realMatrix);
            }
        }
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
        } else {
            this.drawAdjusted(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
        }
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
        } else {
            this.drawAdjusted(texture, x, y, 0.0f, 0.0f, width, height, 1.0f, 1.0f, 0.0f, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
        }
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y, srcX, srcY, srcWidth, srcHeight);
        } else {
            this.drawAdjusted(texture, x, y, 0.0f, 0.0f, srcWidth, srcHeight, 1.0f, 1.0f, 0.0f, srcX, srcY, srcWidth, srcHeight, false, false);
        }
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y, width, height, u, v, u2, v2);
        } else {
            this.drawAdjustedUV(texture, x, y, 0.0f, 0.0f, width, height, 1.0f, 1.0f, 0.0f, u, v, u2, v2, false, false);
        }
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y);
        } else {
            this.drawAdjusted(texture, x, y, 0.0f, 0.0f, texture.getWidth(), texture.getHeight(), 1.0f, 1.0f, 0.0f, 0, 1, 1, 0, false, false);
        }
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        if (!this.adjustNeeded) {
            super.draw(texture, x, y, width, height);
        } else {
            this.drawAdjusted(texture, x, y, 0.0f, 0.0f, width, height, 1.0f, 1.0f, 0.0f, 0, 1, 1, 0, false, false);
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        if (!this.adjustNeeded) {
            super.draw(region, x, y);
        } else {
            this.drawAdjusted(region, x, y, 0.0f, 0.0f, region.getRegionWidth(), region.getRegionHeight(), 1.0f, 1.0f, 0.0f);
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        if (!this.adjustNeeded) {
            super.draw(region, x, y, width, height);
        } else {
            this.drawAdjusted(region, x, y, 0.0f, 0.0f, width, height, 1.0f, 1.0f, 0.0f);
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        if (!this.adjustNeeded) {
            super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        } else {
            this.drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        if (!this.adjustNeeded) {
            super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise);
        } else {
            this.drawAdjusted(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise);
        }
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        if (count % 20 != 0) {
            throw new GdxRuntimeException("invalid vertex count");
        }
        if (!this.adjustNeeded) {
            super.draw(texture, spriteVertices, offset, count);
        } else {
            this.drawAdjusted(texture, spriteVertices, offset, count);
        }
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        if (!this.adjustNeeded) {
            super.draw(region, width, height, transform);
        } else {
            this.drawAdjusted(region, width, height, transform);
        }
    }

    private void drawAdjusted(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        this.drawAdjustedUV(region.texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, region.u, region.v2, region.u2, region.v, false, false);
    }

    private void drawAdjusted(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        float invTexWidth = 1.0f / (float)texture.getWidth();
        float invTexHeight = 1.0f / (float)texture.getHeight();
        float u = (float)srcX * invTexWidth;
        float v = (float)(srcY + srcHeight) * invTexHeight;
        float u2 = (float)(srcX + srcWidth) * invTexWidth;
        float v2 = (float)srcY * invTexHeight;
        this.drawAdjustedUV(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, u, v, u2, v2, flipX, flipY);
    }

    private void drawAdjustedUV(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, float u, float v, float u2, float v2, boolean flipX, boolean flipY) {
        float tmp;
        float y4;
        float x4;
        float y3;
        float x3;
        float y2;
        float x2;
        float y1;
        float x1;
        if (!this.drawing) {
            throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");
        }
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == this.vertices.length) {
            super.flush();
        }
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }
        float p1x = fx;
        float p1y = fy;
        float p2x = fx;
        float p2y = fy2;
        float p3x = fx2;
        float p3y = fy2;
        float p4x = fx2;
        float p4y = fy;
        if (rotation != 0.0f) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }
        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;
        if (flipX) {
            tmp = u;
            u = u2;
            u2 = tmp;
        }
        if (flipY) {
            tmp = v;
            v = v2;
            v2 = tmp;
        }
        Affine2 t = this.adjustAffine;
        this.vertices[this.idx + 0] = t.m00 * x1 + t.m01 * y1 + t.m02;
        this.vertices[this.idx + 1] = t.m10 * x1 + t.m11 * y1 + t.m12;
        this.vertices[this.idx + 2] = this.color;
        this.vertices[this.idx + 3] = u;
        this.vertices[this.idx + 4] = v;
        this.vertices[this.idx + 5] = t.m00 * x2 + t.m01 * y2 + t.m02;
        this.vertices[this.idx + 6] = t.m10 * x2 + t.m11 * y2 + t.m12;
        this.vertices[this.idx + 7] = this.color;
        this.vertices[this.idx + 8] = u;
        this.vertices[this.idx + 9] = v2;
        this.vertices[this.idx + 10] = t.m00 * x3 + t.m01 * y3 + t.m02;
        this.vertices[this.idx + 11] = t.m10 * x3 + t.m11 * y3 + t.m12;
        this.vertices[this.idx + 12] = this.color;
        this.vertices[this.idx + 13] = u2;
        this.vertices[this.idx + 14] = v2;
        this.vertices[this.idx + 15] = t.m00 * x4 + t.m01 * y4 + t.m02;
        this.vertices[this.idx + 16] = t.m10 * x4 + t.m11 * y4 + t.m12;
        this.vertices[this.idx + 17] = this.color;
        this.vertices[this.idx + 18] = u2;
        this.vertices[this.idx + 19] = v;
        this.idx += 20;
    }

    private void drawAdjusted(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        float v4;
        float u4;
        float v3;
        float u3;
        float v2;
        float u2;
        float v1;
        float u1;
        float y4;
        float x4;
        float y3;
        float x3;
        float y2;
        float x2;
        float y1;
        float x1;
        if (!this.drawing) {
            throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");
        }
        if (region.texture != this.lastTexture) {
            this.switchTexture(region.texture);
        } else if (this.idx == this.vertices.length) {
            super.flush();
        }
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }
        float p1x = fx;
        float p1y = fy;
        float p2x = fx;
        float p2y = fy2;
        float p3x = fx2;
        float p3y = fy2;
        float p4x = fx2;
        float p4y = fy;
        if (rotation != 0.0f) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }
        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;
        if (clockwise) {
            u1 = region.u2;
            v1 = region.v2;
            u2 = region.u;
            v2 = region.v2;
            u3 = region.u;
            v3 = region.v;
            u4 = region.u2;
            v4 = region.v;
        } else {
            u1 = region.u;
            v1 = region.v;
            u2 = region.u2;
            v2 = region.v;
            u3 = region.u2;
            v3 = region.v2;
            u4 = region.u;
            v4 = region.v2;
        }
        Affine2 t = this.adjustAffine;
        this.vertices[this.idx + 0] = t.m00 * x1 + t.m01 * y1 + t.m02;
        this.vertices[this.idx + 1] = t.m10 * x1 + t.m11 * y1 + t.m12;
        this.vertices[this.idx + 2] = this.color;
        this.vertices[this.idx + 3] = u1;
        this.vertices[this.idx + 4] = v1;
        this.vertices[this.idx + 5] = t.m00 * x2 + t.m01 * y2 + t.m02;
        this.vertices[this.idx + 6] = t.m10 * x2 + t.m11 * y2 + t.m12;
        this.vertices[this.idx + 7] = this.color;
        this.vertices[this.idx + 8] = u2;
        this.vertices[this.idx + 9] = v2;
        this.vertices[this.idx + 10] = t.m00 * x3 + t.m01 * y3 + t.m02;
        this.vertices[this.idx + 11] = t.m10 * x3 + t.m11 * y3 + t.m12;
        this.vertices[this.idx + 12] = this.color;
        this.vertices[this.idx + 13] = u3;
        this.vertices[this.idx + 14] = v3;
        this.vertices[this.idx + 15] = t.m00 * x4 + t.m01 * y4 + t.m02;
        this.vertices[this.idx + 16] = t.m10 * x4 + t.m11 * y4 + t.m12;
        this.vertices[this.idx + 17] = this.color;
        this.vertices[this.idx + 18] = u4;
        this.vertices[this.idx + 19] = v4;
        this.idx += 20;
    }

    private void drawAdjusted(TextureRegion region, float width, float height, Affine2 transform) {
        if (!this.drawing) {
            throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");
        }
        if (region.texture != this.lastTexture) {
            this.switchTexture(region.texture);
        } else if (this.idx == this.vertices.length) {
            super.flush();
        }
        Affine2 t = transform;
        float x1 = t.m02;
        float y1 = t.m12;
        float x2 = t.m01 * height + t.m02;
        float y2 = t.m11 * height + t.m12;
        float x3 = t.m00 * width + t.m01 * height + t.m02;
        float y3 = t.m10 * width + t.m11 * height + t.m12;
        float x4 = t.m00 * width + t.m02;
        float y4 = t.m10 * width + t.m12;
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        t = this.adjustAffine;
        this.vertices[this.idx + 0] = t.m00 * x1 + t.m01 * y1 + t.m02;
        this.vertices[this.idx + 1] = t.m10 * x1 + t.m11 * y1 + t.m12;
        this.vertices[this.idx + 2] = this.color;
        this.vertices[this.idx + 3] = u;
        this.vertices[this.idx + 4] = v;
        this.vertices[this.idx + 5] = t.m00 * x2 + t.m01 * y2 + t.m02;
        this.vertices[this.idx + 6] = t.m10 * x2 + t.m11 * y2 + t.m12;
        this.vertices[this.idx + 7] = this.color;
        this.vertices[this.idx + 8] = u;
        this.vertices[this.idx + 9] = v2;
        this.vertices[this.idx + 10] = t.m00 * x3 + t.m01 * y3 + t.m02;
        this.vertices[this.idx + 11] = t.m10 * x3 + t.m11 * y3 + t.m12;
        this.vertices[this.idx + 12] = this.color;
        this.vertices[this.idx + 13] = u2;
        this.vertices[this.idx + 14] = v2;
        this.vertices[this.idx + 15] = t.m00 * x4 + t.m01 * y4 + t.m02;
        this.vertices[this.idx + 16] = t.m10 * x4 + t.m11 * y4 + t.m12;
        this.vertices[this.idx + 17] = this.color;
        this.vertices[this.idx + 18] = u2;
        this.vertices[this.idx + 19] = v;
        this.idx += 20;
    }

    private void drawAdjusted(Texture texture, float[] spriteVertices, int offset, int count) {
        if (!this.drawing) {
            throw new IllegalStateException("CpuSpriteBatch.begin must be called before draw.");
        }
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        }
        Affine2 t = this.adjustAffine;
        int copyCount = Math.min(this.vertices.length - this.idx, count);
        do {
            count -= copyCount;
            while (copyCount > 0) {
                float x = spriteVertices[offset];
                float y = spriteVertices[offset + 1];
                this.vertices[this.idx] = t.m00 * x + t.m01 * y + t.m02;
                this.vertices[this.idx + 1] = t.m10 * x + t.m11 * y + t.m12;
                this.vertices[this.idx + 2] = spriteVertices[offset + 2];
                this.vertices[this.idx + 3] = spriteVertices[offset + 3];
                this.vertices[this.idx + 4] = spriteVertices[offset + 4];
                this.idx += 5;
                offset += 5;
                copyCount -= 5;
            }
            if (count <= 0) continue;
            super.flush();
            copyCount = Math.min(this.vertices.length, count);
        } while (count > 0);
    }

    private static boolean checkEqual(Matrix4 a, Matrix4 b) {
        if (a == b) {
            return true;
        }
        return a.val[0] == b.val[0] && a.val[1] == b.val[1] && a.val[4] == b.val[4] && a.val[5] == b.val[5] && a.val[12] == b.val[12] && a.val[13] == b.val[13];
    }

    private static boolean checkEqual(Matrix4 matrix, Affine2 affine) {
        float[] val = matrix.getValues();
        return val[0] == affine.m00 && val[1] == affine.m10 && val[4] == affine.m01 && val[5] == affine.m11 && val[12] == affine.m02 && val[13] == affine.m12;
    }

    private static boolean checkIdt(Matrix4 matrix) {
        float[] val = matrix.getValues();
        return val[0] == 1.0f && val[1] == 0.0f && val[4] == 0.0f && val[5] == 1.0f && val[12] == 0.0f && val[13] == 0.0f;
    }
}

