/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public class ShapeRenderer
implements Disposable {
    private final ImmediateModeRenderer renderer;
    private boolean matrixDirty = false;
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();
    private final Vector2 tmp = new Vector2();
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private ShapeType shapeType;
    private boolean autoShapeType;
    private float defaultRectLineWidth = 0.75f;

    public ShapeRenderer() {
        this(5000);
    }

    public ShapeRenderer(int maxVertices) {
        this(maxVertices, null);
    }

    public ShapeRenderer(int maxVertices, ShaderProgram defaultShader) {
        this.renderer = defaultShader == null ? new ImmediateModeRenderer20(maxVertices, false, true, 0) : new ImmediateModeRenderer20(maxVertices, false, true, 0, defaultShader);
        this.projectionMatrix.setToOrtho2D(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.matrixDirty = true;
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
    }

    public Color getColor() {
        return this.color;
    }

    public void updateMatrices() {
        this.matrixDirty = true;
    }

    public void setProjectionMatrix(Matrix4 matrix) {
        this.projectionMatrix.set(matrix);
        this.matrixDirty = true;
    }

    public Matrix4 getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public void setTransformMatrix(Matrix4 matrix) {
        this.transformMatrix.set(matrix);
        this.matrixDirty = true;
    }

    public Matrix4 getTransformMatrix() {
        return this.transformMatrix;
    }

    public void identity() {
        this.transformMatrix.idt();
        this.matrixDirty = true;
    }

    public void translate(float x, float y, float z) {
        this.transformMatrix.translate(x, y, z);
        this.matrixDirty = true;
    }

    public void rotate(float axisX, float axisY, float axisZ, float degrees) {
        this.transformMatrix.rotate(axisX, axisY, axisZ, degrees);
        this.matrixDirty = true;
    }

    public void scale(float scaleX, float scaleY, float scaleZ) {
        this.transformMatrix.scale(scaleX, scaleY, scaleZ);
        this.matrixDirty = true;
    }

    public void setAutoShapeType(boolean autoShapeType) {
        this.autoShapeType = autoShapeType;
    }

    public void begin() {
        if (!this.autoShapeType) {
            throw new IllegalStateException("autoShapeType must be true to use this method.");
        }
        this.begin(ShapeType.Line);
    }

    public void begin(ShapeType type) {
        if (this.shapeType != null) {
            throw new IllegalStateException("Call end() before beginning a new shape batch.");
        }
        this.shapeType = type;
        if (this.matrixDirty) {
            this.combinedMatrix.set(this.projectionMatrix);
            Matrix4.mul(this.combinedMatrix.val, this.transformMatrix.val);
            this.matrixDirty = false;
        }
        this.renderer.begin(this.combinedMatrix, this.shapeType.getGlType());
    }

    public void set(ShapeType type) {
        if (this.shapeType == type) {
            return;
        }
        if (this.shapeType == null) {
            throw new IllegalStateException("begin must be called first.");
        }
        if (!this.autoShapeType) {
            throw new IllegalStateException("autoShapeType must be enabled.");
        }
        this.end();
        this.begin(type);
    }

    public void point(float x, float y, float z) {
        if (this.shapeType == ShapeType.Line) {
            float size = this.defaultRectLineWidth * 0.5f;
            this.line(x - size, y - size, z, x + size, y + size, z);
            return;
        }
        if (this.shapeType == ShapeType.Filled) {
            float size = this.defaultRectLineWidth * 0.5f;
            this.box(x - size, y - size, z - size, this.defaultRectLineWidth, this.defaultRectLineWidth, this.defaultRectLineWidth);
            return;
        }
        this.check(ShapeType.Point, null, 1);
        this.renderer.color(this.color);
        this.renderer.vertex(x, y, z);
    }

    public final void line(float x, float y, float z, float x2, float y2, float z2) {
        this.line(x, y, z, x2, y2, z2, this.color, this.color);
    }

    public final void line(Vector3 v0, Vector3 v1) {
        this.line(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, this.color, this.color);
    }

    public final void line(float x, float y, float x2, float y2) {
        this.line(x, y, 0.0f, x2, y2, 0.0f, this.color, this.color);
    }

    public final void line(Vector2 v0, Vector2 v1) {
        this.line(v0.x, v0.y, 0.0f, v1.x, v1.y, 0.0f, this.color, this.color);
    }

    public final void line(float x, float y, float x2, float y2, Color c1, Color c2) {
        this.line(x, y, 0.0f, x2, y2, 0.0f, c1, c2);
    }

    public void line(float x, float y, float z, float x2, float y2, float z2, Color c1, Color c2) {
        if (this.shapeType == ShapeType.Filled) {
            this.rectLine(x, y, x2, y2, this.defaultRectLineWidth, c1, c2);
            return;
        }
        this.check(ShapeType.Line, null, 2);
        this.renderer.color(c1.r, c1.g, c1.b, c1.a);
        this.renderer.vertex(x, y, z);
        this.renderer.color(c2.r, c2.g, c2.b, c2.a);
        this.renderer.vertex(x2, y2, z2);
    }

    public void curve(float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2, int segments) {
        this.check(ShapeType.Line, null, segments * 2 + 2);
        float colorBits = this.color.toFloatBits();
        float subdiv_step = 1.0f / (float)segments;
        float subdiv_step2 = subdiv_step * subdiv_step;
        float subdiv_step3 = subdiv_step * subdiv_step * subdiv_step;
        float pre1 = 3.0f * subdiv_step;
        float pre2 = 3.0f * subdiv_step2;
        float pre4 = 6.0f * subdiv_step2;
        float pre5 = 6.0f * subdiv_step3;
        float tmp1x = x1 - cx1 * 2.0f + cx2;
        float tmp1y = y1 - cy1 * 2.0f + cy2;
        float tmp2x = (cx1 - cx2) * 3.0f - x1 + x2;
        float tmp2y = (cy1 - cy2) * 3.0f - y1 + y2;
        float fx = x1;
        float fy = y1;
        float dfx = (cx1 - x1) * pre1 + tmp1x * pre2 + tmp2x * subdiv_step3;
        float dfy = (cy1 - y1) * pre1 + tmp1y * pre2 + tmp2y * subdiv_step3;
        float ddfx = tmp1x * pre4 + tmp2x * pre5;
        float ddfy = tmp1y * pre4 + tmp2y * pre5;
        float dddfx = tmp2x * pre5;
        float dddfy = tmp2y * pre5;
        while (segments-- > 0) {
            this.renderer.color(colorBits);
            this.renderer.vertex(fx, fy, 0.0f);
            fx += dfx;
            fy += dfy;
            dfx += ddfx;
            dfy += ddfy;
            ddfx += dddfx;
            ddfy += dddfy;
            this.renderer.color(colorBits);
            this.renderer.vertex(fx, fy, 0.0f);
        }
        this.renderer.color(colorBits);
        this.renderer.vertex(fx, fy, 0.0f);
        this.renderer.color(colorBits);
        this.renderer.vertex(x2, y2, 0.0f);
    }

    public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        this.check(ShapeType.Line, ShapeType.Filled, 6);
        float colorBits = this.color.toFloatBits();
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(colorBits);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1, y1, 0.0f);
        } else {
            this.renderer.color(colorBits);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x3, y3, 0.0f);
        }
    }

    public void triangle(float x1, float y1, float x2, float y2, float x3, float y3, Color col1, Color col2, Color col3) {
        this.check(ShapeType.Line, ShapeType.Filled, 6);
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
        } else {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
        }
    }

    public void rect(float x, float y, float width, float height) {
        this.check(ShapeType.Line, ShapeType.Filled, 8);
        float colorBits = this.color.toFloatBits();
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
        } else {
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
        }
    }

    public void rect(float x, float y, float width, float height, Color col1, Color col2, Color col3, Color col4) {
        this.check(ShapeType.Line, ShapeType.Filled, 8);
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x, y, 0.0f);
        } else {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x + width, y, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x + width, y + height, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x, y + height, 0.0f);
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x, y, 0.0f);
        }
    }

    public void rect(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float degrees) {
        this.rect(x, y, originX, originY, width, height, scaleX, scaleY, degrees, this.color, this.color, this.color, this.color);
    }

    public void rect(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float degrees, Color col1, Color col2, Color col3, Color col4) {
        this.check(ShapeType.Line, ShapeType.Filled, 8);
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
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
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float x1 = cos * fx - sin * fy + worldOriginX;
        float y1 = sin * fx + cos * fy + worldOriginY;
        float x2 = cos * fx2 - sin * fy + worldOriginX;
        float y2 = sin * fx2 + cos * fy + worldOriginY;
        float x3 = cos * fx2 - sin * fy2 + worldOriginX;
        float y3 = sin * fx2 + cos * fy2 + worldOriginY;
        float x4 = x1 + (x3 - x2);
        float y4 = y3 - (y2 - y1);
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x4, y4, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x4, y4, 0.0f);
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
        } else {
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(col2.r, col2.g, col2.b, col2.a);
            this.renderer.vertex(x2, y2, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col3.r, col3.g, col3.b, col3.a);
            this.renderer.vertex(x3, y3, 0.0f);
            this.renderer.color(col4.r, col4.g, col4.b, col4.a);
            this.renderer.vertex(x4, y4, 0.0f);
            this.renderer.color(col1.r, col1.g, col1.b, col1.a);
            this.renderer.vertex(x1, y1, 0.0f);
        }
    }

    public void rectLine(float x1, float y1, float x2, float y2, float width) {
        this.check(ShapeType.Line, ShapeType.Filled, 8);
        float colorBits = this.color.toFloatBits();
        Vector2 t = this.tmp.set(y2 - y1, x1 - x2).nor();
        float tx = t.x * (width *= 0.5f);
        float ty = t.y * width;
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
        } else {
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
        }
    }

    public void rectLine(float x1, float y1, float x2, float y2, float width, Color c1, Color c2) {
        this.check(ShapeType.Line, ShapeType.Filled, 8);
        float col1Bits = c1.toFloatBits();
        float col2Bits = c2.toFloatBits();
        Vector2 t = this.tmp.set(y2 - y1, x1 - x2).nor();
        float tx = t.x * (width *= 0.5f);
        float ty = t.y * width;
        if (this.shapeType == ShapeType.Line) {
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
        } else {
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 + tx, y1 + ty, 0.0f);
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 - tx, y2 - ty, 0.0f);
            this.renderer.color(col2Bits);
            this.renderer.vertex(x2 + tx, y2 + ty, 0.0f);
            this.renderer.color(col1Bits);
            this.renderer.vertex(x1 - tx, y1 - ty, 0.0f);
        }
    }

    public void rectLine(Vector2 p1, Vector2 p2, float width) {
        this.rectLine(p1.x, p1.y, p2.x, p2.y, width);
    }

    public void box(float x, float y, float z, float width, float height, float depth) {
        depth = -depth;
        float colorBits = this.color.toFloatBits();
        if (this.shapeType == ShapeType.Line) {
            this.check(ShapeType.Line, ShapeType.Filled, 24);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
        } else {
            this.check(ShapeType.Line, ShapeType.Filled, 36);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y + height, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + depth);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + width, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
        }
    }

    public void x(float x, float y, float size) {
        this.line(x - size, y - size, x + size, y + size);
        this.line(x - size, y + size, x + size, y - size);
    }

    public void x(Vector2 p, float size) {
        this.x(p.x, p.y, size);
    }

    public void arc(float x, float y, float radius, float start, float degrees) {
        this.arc(x, y, radius, start, degrees, Math.max(1, (int)(6.0f * (float)Math.cbrt(radius) * (degrees / 360.0f))));
    }

    public void arc(float x, float y, float radius, float start, float degrees, int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("segments must be > 0.");
        }
        float colorBits = this.color.toFloatBits();
        float theta = (float)Math.PI * 2 * (degrees / 360.0f) / (float)segments;
        float cos = MathUtils.cos(theta);
        float sin = MathUtils.sin(theta);
        float cx = radius * MathUtils.cos(start * ((float)Math.PI / 180));
        float cy = radius * MathUtils.sin(start * ((float)Math.PI / 180));
        if (this.shapeType == ShapeType.Line) {
            this.check(ShapeType.Line, ShapeType.Filled, segments * 2 + 2);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, 0.0f);
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, 0.0f);
        } else {
            this.check(ShapeType.Line, ShapeType.Filled, segments * 3 + 3);
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x, y, 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, 0.0f);
        }
        float temp = cx;
        cx = 0.0f;
        cy = 0.0f;
        this.renderer.color(colorBits);
        this.renderer.vertex(x + cx, y + cy, 0.0f);
    }

    public void circle(float x, float y, float radius) {
        this.circle(x, y, radius, Math.max(1, (int)(6.0f * (float)Math.cbrt(radius))));
    }

    public void circle(float x, float y, float radius, int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("segments must be > 0.");
        }
        float colorBits = this.color.toFloatBits();
        float angle = (float)Math.PI * 2 / (float)segments;
        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);
        float cx = radius;
        float cy = 0.0f;
        if (this.shapeType == ShapeType.Line) {
            this.check(ShapeType.Line, ShapeType.Filled, segments * 2 + 2);
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, 0.0f);
        } else {
            this.check(ShapeType.Line, ShapeType.Filled, segments * 3 + 3);
            --segments;
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x, y, 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, 0.0f);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, 0.0f);
        }
        float temp = cx;
        cx = radius;
        cy = 0.0f;
        this.renderer.color(colorBits);
        this.renderer.vertex(x + cx, y + cy, 0.0f);
    }

    public void ellipse(float x, float y, float width, float height) {
        this.ellipse(x, y, width, height, Math.max(1, (int)(12.0f * (float)Math.cbrt(Math.max(width * 0.5f, height * 0.5f)))));
    }

    public void ellipse(float x, float y, float width, float height, int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("segments must be > 0.");
        }
        this.check(ShapeType.Line, ShapeType.Filled, segments * 3);
        float colorBits = this.color.toFloatBits();
        float angle = (float)Math.PI * 2 / (float)segments;
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        if (this.shapeType == ShapeType.Line) {
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + width * 0.5f * MathUtils.cos((float)i * angle), cy + height * 0.5f * MathUtils.sin((float)i * angle), 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + width * 0.5f * MathUtils.cos((float)(i + 1) * angle), cy + height * 0.5f * MathUtils.sin((float)(i + 1) * angle), 0.0f);
            }
        } else {
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + width * 0.5f * MathUtils.cos((float)i * angle), cy + height * 0.5f * MathUtils.sin((float)i * angle), 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx, cy, 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + width * 0.5f * MathUtils.cos((float)(i + 1) * angle), cy + height * 0.5f * MathUtils.sin((float)(i + 1) * angle), 0.0f);
            }
        }
    }

    public void ellipse(float x, float y, float width, float height, float rotation) {
        this.ellipse(x, y, width, height, rotation, Math.max(1, (int)(12.0f * (float)Math.cbrt(Math.max(width * 0.5f, height * 0.5f)))));
    }

    public void ellipse(float x, float y, float width, float height, float rotation, int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("segments must be > 0.");
        }
        this.check(ShapeType.Line, ShapeType.Filled, segments * 3);
        float colorBits = this.color.toFloatBits();
        float angle = (float)Math.PI * 2 / (float)segments;
        rotation = (float)Math.PI * rotation / 180.0f;
        float sin = MathUtils.sin(rotation);
        float cos = MathUtils.cos(rotation);
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        float x1 = width * 0.5f;
        float y1 = 0.0f;
        if (this.shapeType == ShapeType.Line) {
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0.0f);
                x1 = width * 0.5f * MathUtils.cos((float)(i + 1) * angle);
                y1 = height * 0.5f * MathUtils.sin((float)(i + 1) * angle);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0.0f);
            }
        } else {
            for (int i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0.0f);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx, cy, 0.0f);
                x1 = width * 0.5f * MathUtils.cos((float)(i + 1) * angle);
                y1 = height * 0.5f * MathUtils.sin((float)(i + 1) * angle);
                this.renderer.color(colorBits);
                this.renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0.0f);
            }
        }
    }

    public void cone(float x, float y, float z, float radius, float height) {
        this.cone(x, y, z, radius, height, Math.max(1, (int)(4.0f * (float)Math.sqrt(radius))));
    }

    public void cone(float x, float y, float z, float radius, float height, int segments) {
        float temp;
        int i;
        if (segments <= 0) {
            throw new IllegalArgumentException("segments must be > 0.");
        }
        this.check(ShapeType.Line, ShapeType.Filled, segments * 4 + 2);
        float colorBits = this.color.toFloatBits();
        float angle = (float)Math.PI * 2 / (float)segments;
        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);
        float cx = radius;
        float cy = 0.0f;
        if (this.shapeType == ShapeType.Line) {
            for (i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
                this.renderer.color(colorBits);
                this.renderer.vertex(x, y, z + height);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
                temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, z);
        } else {
            --segments;
            for (i = 0; i < segments; ++i) {
                this.renderer.color(colorBits);
                this.renderer.vertex(x, y, z);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
                temp = cx;
                float temp2 = cy;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + temp, y + temp2, z);
                this.renderer.color(colorBits);
                this.renderer.vertex(x + cx, y + cy, z);
                this.renderer.color(colorBits);
                this.renderer.vertex(x, y, z + height);
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, z);
        }
        float temp2 = cx;
        float temp22 = cy;
        cx = radius;
        cy = 0.0f;
        this.renderer.color(colorBits);
        this.renderer.vertex(x + cx, y + cy, z);
        if (this.shapeType != ShapeType.Line) {
            this.renderer.color(colorBits);
            this.renderer.vertex(x + temp2, y + temp22, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x + cx, y + cy, z);
            this.renderer.color(colorBits);
            this.renderer.vertex(x, y, z + height);
        }
    }

    public void polygon(float[] vertices, int offset, int count) {
        if (count < 6) {
            throw new IllegalArgumentException("Polygons must contain at least 3 points.");
        }
        if (count % 2 != 0) {
            throw new IllegalArgumentException("Polygons must have an even number of vertices.");
        }
        this.check(ShapeType.Line, null, count);
        float colorBits = this.color.toFloatBits();
        float firstX = vertices[0];
        float firstY = vertices[1];
        int n = offset + count;
        for (int i = offset; i < n; i += 2) {
            float y2;
            float x2;
            float x1 = vertices[i];
            float y1 = vertices[i + 1];
            if (i + 2 >= count) {
                x2 = firstX;
                y2 = firstY;
            } else {
                x2 = vertices[i + 2];
                y2 = vertices[i + 3];
            }
            this.renderer.color(colorBits);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2, y2, 0.0f);
        }
    }

    public void polygon(float[] vertices) {
        this.polygon(vertices, 0, vertices.length);
    }

    public void polyline(float[] vertices, int offset, int count) {
        if (count < 4) {
            throw new IllegalArgumentException("Polylines must contain at least 2 points.");
        }
        if (count % 2 != 0) {
            throw new IllegalArgumentException("Polylines must have an even number of vertices.");
        }
        this.check(ShapeType.Line, null, count);
        float colorBits = this.color.toFloatBits();
        int n = offset + count - 2;
        for (int i = offset; i < n; i += 2) {
            float x1 = vertices[i];
            float y1 = vertices[i + 1];
            float x2 = vertices[i + 2];
            float y2 = vertices[i + 3];
            this.renderer.color(colorBits);
            this.renderer.vertex(x1, y1, 0.0f);
            this.renderer.color(colorBits);
            this.renderer.vertex(x2, y2, 0.0f);
        }
    }

    public void polyline(float[] vertices) {
        this.polyline(vertices, 0, vertices.length);
    }

    private void check(ShapeType preferred, ShapeType other, int newVertices) {
        if (this.shapeType == null) {
            throw new IllegalStateException("begin must be called first.");
        }
        if (this.shapeType != preferred && this.shapeType != other) {
            if (!this.autoShapeType) {
                if (other == null) {
                    throw new IllegalStateException("Must call begin(ShapeType." + (Object)((Object)preferred) + ").");
                }
                throw new IllegalStateException("Must call begin(ShapeType." + (Object)((Object)preferred) + ") or begin(ShapeType." + (Object)((Object)other) + ").");
            }
            this.end();
            this.begin(preferred);
        } else if (this.matrixDirty) {
            ShapeType type = this.shapeType;
            this.end();
            this.begin(type);
        } else if (this.renderer.getMaxVertices() - this.renderer.getNumVertices() < newVertices) {
            ShapeType type = this.shapeType;
            this.end();
            this.begin(type);
        }
    }

    public void end() {
        this.renderer.end();
        this.shapeType = null;
    }

    public void flush() {
        ShapeType type = this.shapeType;
        this.end();
        this.begin(type);
    }

    public ShapeType getCurrentType() {
        return this.shapeType;
    }

    public ImmediateModeRenderer getRenderer() {
        return this.renderer;
    }

    public boolean isDrawing() {
        return this.shapeType != null;
    }

    @Override
    public void dispose() {
        this.renderer.dispose();
    }

    public static enum ShapeType {
        Point(0),
        Line(1),
        Filled(4);

        private final int glType;

        private ShapeType(int glType) {
            this.glType = glType;
        }

        public int getGlType() {
            return this.glType;
        }
    }
}

