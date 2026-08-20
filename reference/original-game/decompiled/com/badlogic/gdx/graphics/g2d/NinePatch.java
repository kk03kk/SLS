/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class NinePatch {
    public static final int TOP_LEFT = 0;
    public static final int TOP_CENTER = 1;
    public static final int TOP_RIGHT = 2;
    public static final int MIDDLE_LEFT = 3;
    public static final int MIDDLE_CENTER = 4;
    public static final int MIDDLE_RIGHT = 5;
    public static final int BOTTOM_LEFT = 6;
    public static final int BOTTOM_CENTER = 7;
    public static final int BOTTOM_RIGHT = 8;
    private static final Color tmpDrawColor = new Color();
    private Texture texture;
    private int bottomLeft = -1;
    private int bottomCenter = -1;
    private int bottomRight = -1;
    private int middleLeft = -1;
    private int middleCenter = -1;
    private int middleRight = -1;
    private int topLeft = -1;
    private int topCenter = -1;
    private int topRight = -1;
    private float leftWidth;
    private float rightWidth;
    private float middleWidth;
    private float middleHeight;
    private float topHeight;
    private float bottomHeight;
    private float[] vertices = new float[180];
    private int idx;
    private final Color color = new Color(Color.WHITE);
    private float padLeft = -1.0f;
    private float padRight = -1.0f;
    private float padTop = -1.0f;
    private float padBottom = -1.0f;

    public NinePatch(Texture texture, int left, int right, int top, int bottom) {
        this(new TextureRegion(texture), left, right, top, bottom);
    }

    public NinePatch(TextureRegion region, int left, int right, int top, int bottom) {
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null.");
        }
        int middleWidth = region.getRegionWidth() - left - right;
        int middleHeight = region.getRegionHeight() - top - bottom;
        TextureRegion[] patches = new TextureRegion[9];
        if (top > 0) {
            if (left > 0) {
                patches[0] = new TextureRegion(region, 0, 0, left, top);
            }
            if (middleWidth > 0) {
                patches[1] = new TextureRegion(region, left, 0, middleWidth, top);
            }
            if (right > 0) {
                patches[2] = new TextureRegion(region, left + middleWidth, 0, right, top);
            }
        }
        if (middleHeight > 0) {
            if (left > 0) {
                patches[3] = new TextureRegion(region, 0, top, left, middleHeight);
            }
            if (middleWidth > 0) {
                patches[4] = new TextureRegion(region, left, top, middleWidth, middleHeight);
            }
            if (right > 0) {
                patches[5] = new TextureRegion(region, left + middleWidth, top, right, middleHeight);
            }
        }
        if (bottom > 0) {
            if (left > 0) {
                patches[6] = new TextureRegion(region, 0, top + middleHeight, left, bottom);
            }
            if (middleWidth > 0) {
                patches[7] = new TextureRegion(region, left, top + middleHeight, middleWidth, bottom);
            }
            if (right > 0) {
                patches[8] = new TextureRegion(region, left + middleWidth, top + middleHeight, right, bottom);
            }
        }
        if (left == 0 && middleWidth == 0) {
            patches[1] = patches[2];
            patches[4] = patches[5];
            patches[7] = patches[8];
            patches[2] = null;
            patches[5] = null;
            patches[8] = null;
        }
        if (top == 0 && middleHeight == 0) {
            patches[3] = patches[6];
            patches[4] = patches[7];
            patches[5] = patches[8];
            patches[6] = null;
            patches[7] = null;
            patches[8] = null;
        }
        this.load(patches);
    }

    public NinePatch(Texture texture, Color color) {
        this(texture);
        this.setColor(color);
    }

    public NinePatch(Texture texture) {
        this(new TextureRegion(texture));
    }

    public NinePatch(TextureRegion region, Color color) {
        this(region);
        this.setColor(color);
    }

    public NinePatch(TextureRegion region) {
        this.load(new TextureRegion[]{null, null, null, null, region, null, null, null, null});
    }

    public NinePatch(TextureRegion ... patches) {
        if (patches == null || patches.length != 9) {
            throw new IllegalArgumentException("NinePatch needs nine TextureRegions");
        }
        this.load(patches);
        float leftWidth = this.getLeftWidth();
        if (patches[0] != null && (float)patches[0].getRegionWidth() != leftWidth || patches[3] != null && (float)patches[3].getRegionWidth() != leftWidth || patches[6] != null && (float)patches[6].getRegionWidth() != leftWidth) {
            throw new GdxRuntimeException("Left side patches must have the same width");
        }
        float rightWidth = this.getRightWidth();
        if (patches[2] != null && (float)patches[2].getRegionWidth() != rightWidth || patches[5] != null && (float)patches[5].getRegionWidth() != rightWidth || patches[8] != null && (float)patches[8].getRegionWidth() != rightWidth) {
            throw new GdxRuntimeException("Right side patches must have the same width");
        }
        float bottomHeight = this.getBottomHeight();
        if (patches[6] != null && (float)patches[6].getRegionHeight() != bottomHeight || patches[7] != null && (float)patches[7].getRegionHeight() != bottomHeight || patches[8] != null && (float)patches[8].getRegionHeight() != bottomHeight) {
            throw new GdxRuntimeException("Bottom side patches must have the same height");
        }
        float topHeight = this.getTopHeight();
        if (patches[0] != null && (float)patches[0].getRegionHeight() != topHeight || patches[1] != null && (float)patches[1].getRegionHeight() != topHeight || patches[2] != null && (float)patches[2].getRegionHeight() != topHeight) {
            throw new GdxRuntimeException("Top side patches must have the same height");
        }
    }

    public NinePatch(NinePatch ninePatch) {
        this(ninePatch, ninePatch.color);
    }

    public NinePatch(NinePatch ninePatch, Color color) {
        this.texture = ninePatch.texture;
        this.bottomLeft = ninePatch.bottomLeft;
        this.bottomCenter = ninePatch.bottomCenter;
        this.bottomRight = ninePatch.bottomRight;
        this.middleLeft = ninePatch.middleLeft;
        this.middleCenter = ninePatch.middleCenter;
        this.middleRight = ninePatch.middleRight;
        this.topLeft = ninePatch.topLeft;
        this.topCenter = ninePatch.topCenter;
        this.topRight = ninePatch.topRight;
        this.leftWidth = ninePatch.leftWidth;
        this.rightWidth = ninePatch.rightWidth;
        this.middleWidth = ninePatch.middleWidth;
        this.middleHeight = ninePatch.middleHeight;
        this.topHeight = ninePatch.topHeight;
        this.bottomHeight = ninePatch.bottomHeight;
        this.padLeft = ninePatch.padLeft;
        this.padTop = ninePatch.padTop;
        this.padBottom = ninePatch.padBottom;
        this.padRight = ninePatch.padRight;
        this.vertices = new float[ninePatch.vertices.length];
        System.arraycopy(ninePatch.vertices, 0, this.vertices, 0, ninePatch.vertices.length);
        this.idx = ninePatch.idx;
        this.color.set(color);
    }

    private void load(TextureRegion[] patches) {
        float color = Color.WHITE.toFloatBits();
        if (patches[6] != null) {
            this.bottomLeft = this.add(patches[6], color, false, false);
            this.leftWidth = patches[6].getRegionWidth();
            this.bottomHeight = patches[6].getRegionHeight();
        }
        if (patches[7] != null) {
            this.bottomCenter = this.add(patches[7], color, true, false);
            this.middleWidth = Math.max(this.middleWidth, (float)patches[7].getRegionWidth());
            this.bottomHeight = Math.max(this.bottomHeight, (float)patches[7].getRegionHeight());
        }
        if (patches[8] != null) {
            this.bottomRight = this.add(patches[8], color, false, false);
            this.rightWidth = Math.max(this.rightWidth, (float)patches[8].getRegionWidth());
            this.bottomHeight = Math.max(this.bottomHeight, (float)patches[8].getRegionHeight());
        }
        if (patches[3] != null) {
            this.middleLeft = this.add(patches[3], color, false, true);
            this.leftWidth = Math.max(this.leftWidth, (float)patches[3].getRegionWidth());
            this.middleHeight = Math.max(this.middleHeight, (float)patches[3].getRegionHeight());
        }
        if (patches[4] != null) {
            this.middleCenter = this.add(patches[4], color, true, true);
            this.middleWidth = Math.max(this.middleWidth, (float)patches[4].getRegionWidth());
            this.middleHeight = Math.max(this.middleHeight, (float)patches[4].getRegionHeight());
        }
        if (patches[5] != null) {
            this.middleRight = this.add(patches[5], color, false, true);
            this.rightWidth = Math.max(this.rightWidth, (float)patches[5].getRegionWidth());
            this.middleHeight = Math.max(this.middleHeight, (float)patches[5].getRegionHeight());
        }
        if (patches[0] != null) {
            this.topLeft = this.add(patches[0], color, false, false);
            this.leftWidth = Math.max(this.leftWidth, (float)patches[0].getRegionWidth());
            this.topHeight = Math.max(this.topHeight, (float)patches[0].getRegionHeight());
        }
        if (patches[1] != null) {
            this.topCenter = this.add(patches[1], color, true, false);
            this.middleWidth = Math.max(this.middleWidth, (float)patches[1].getRegionWidth());
            this.topHeight = Math.max(this.topHeight, (float)patches[1].getRegionHeight());
        }
        if (patches[2] != null) {
            this.topRight = this.add(patches[2], color, false, false);
            this.rightWidth = Math.max(this.rightWidth, (float)patches[2].getRegionWidth());
            this.topHeight = Math.max(this.topHeight, (float)patches[2].getRegionHeight());
        }
        if (this.idx < this.vertices.length) {
            float[] newVertices = new float[this.idx];
            System.arraycopy(this.vertices, 0, newVertices, 0, this.idx);
            this.vertices = newVertices;
        }
    }

    private int add(TextureRegion region, float color, boolean isStretchW, boolean isStretchH) {
        if (this.texture == null) {
            this.texture = region.getTexture();
        } else if (this.texture != region.getTexture()) {
            throw new IllegalArgumentException("All regions must be from the same texture.");
        }
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        if (isStretchW) {
            float halfTexelWidth = 0.5f / (float)this.texture.getWidth();
            u += halfTexelWidth;
            u2 -= halfTexelWidth;
        }
        if (isStretchH) {
            float halfTexelHeight = 0.5f / (float)this.texture.getHeight();
            v -= halfTexelHeight;
            v2 += halfTexelHeight;
        }
        float[] vertices = this.vertices;
        vertices[this.idx + 2] = color;
        vertices[this.idx + 3] = u;
        vertices[this.idx + 4] = v;
        vertices[this.idx + 7] = color;
        vertices[this.idx + 8] = u;
        vertices[this.idx + 9] = v2;
        vertices[this.idx + 12] = color;
        vertices[this.idx + 13] = u2;
        vertices[this.idx + 14] = v2;
        vertices[this.idx + 17] = color;
        vertices[this.idx + 18] = u2;
        vertices[this.idx + 19] = v;
        this.idx += 20;
        return this.idx - 20;
    }

    private void set(int idx, float x, float y, float width, float height, float color) {
        float fx2 = x + width;
        float fy2 = y + height;
        float[] vertices = this.vertices;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
    }

    private void prepareVertices(Batch batch, float x, float y, float width, float height) {
        float centerColumnX = x + this.leftWidth;
        float rightColumnX = x + width - this.rightWidth;
        float middleRowY = y + this.bottomHeight;
        float topRowY = y + height - this.topHeight;
        float c = tmpDrawColor.set(this.color).mul(batch.getColor()).toFloatBits();
        if (this.bottomLeft != -1) {
            this.set(this.bottomLeft, x, y, centerColumnX - x, middleRowY - y, c);
        }
        if (this.bottomCenter != -1) {
            this.set(this.bottomCenter, centerColumnX, y, rightColumnX - centerColumnX, middleRowY - y, c);
        }
        if (this.bottomRight != -1) {
            this.set(this.bottomRight, rightColumnX, y, x + width - rightColumnX, middleRowY - y, c);
        }
        if (this.middleLeft != -1) {
            this.set(this.middleLeft, x, middleRowY, centerColumnX - x, topRowY - middleRowY, c);
        }
        if (this.middleCenter != -1) {
            this.set(this.middleCenter, centerColumnX, middleRowY, rightColumnX - centerColumnX, topRowY - middleRowY, c);
        }
        if (this.middleRight != -1) {
            this.set(this.middleRight, rightColumnX, middleRowY, x + width - rightColumnX, topRowY - middleRowY, c);
        }
        if (this.topLeft != -1) {
            this.set(this.topLeft, x, topRowY, centerColumnX - x, y + height - topRowY, c);
        }
        if (this.topCenter != -1) {
            this.set(this.topCenter, centerColumnX, topRowY, rightColumnX - centerColumnX, y + height - topRowY, c);
        }
        if (this.topRight != -1) {
            this.set(this.topRight, rightColumnX, topRowY, x + width - rightColumnX, y + height - topRowY, c);
        }
    }

    public void draw(Batch batch, float x, float y, float width, float height) {
        this.prepareVertices(batch, x, y, width, height);
        batch.draw(this.texture, this.vertices, 0, this.idx);
    }

    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        this.prepareVertices(batch, x, y, width, height);
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        int n = this.idx;
        float[] vertices = this.vertices;
        if (rotation != 0.0f) {
            for (int i = 0; i < n; i += 5) {
                float vx = (vertices[i] - worldOriginX) * scaleX;
                float vy = (vertices[i + 1] - worldOriginY) * scaleY;
                float cos = MathUtils.cosDeg(rotation);
                float sin = MathUtils.sinDeg(rotation);
                vertices[i] = cos * vx - sin * vy + worldOriginX;
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY;
            }
        } else if (scaleX != 1.0f || scaleY != 1.0f) {
            for (int i = 0; i < n; i += 5) {
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX;
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY;
            }
        }
        batch.draw(this.texture, vertices, 0, n);
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public Color getColor() {
        return this.color;
    }

    public float getLeftWidth() {
        return this.leftWidth;
    }

    public void setLeftWidth(float leftWidth) {
        this.leftWidth = leftWidth;
    }

    public float getRightWidth() {
        return this.rightWidth;
    }

    public void setRightWidth(float rightWidth) {
        this.rightWidth = rightWidth;
    }

    public float getTopHeight() {
        return this.topHeight;
    }

    public void setTopHeight(float topHeight) {
        this.topHeight = topHeight;
    }

    public float getBottomHeight() {
        return this.bottomHeight;
    }

    public void setBottomHeight(float bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    public float getMiddleWidth() {
        return this.middleWidth;
    }

    public void setMiddleWidth(float middleWidth) {
        this.middleWidth = middleWidth;
    }

    public float getMiddleHeight() {
        return this.middleHeight;
    }

    public void setMiddleHeight(float middleHeight) {
        this.middleHeight = middleHeight;
    }

    public float getTotalWidth() {
        return this.leftWidth + this.middleWidth + this.rightWidth;
    }

    public float getTotalHeight() {
        return this.topHeight + this.middleHeight + this.bottomHeight;
    }

    public void setPadding(float left, float right, float top, float bottom) {
        this.padLeft = left;
        this.padRight = right;
        this.padTop = top;
        this.padBottom = bottom;
    }

    public float getPadLeft() {
        if (this.padLeft == -1.0f) {
            return this.getLeftWidth();
        }
        return this.padLeft;
    }

    public void setPadLeft(float left) {
        this.padLeft = left;
    }

    public float getPadRight() {
        if (this.padRight == -1.0f) {
            return this.getRightWidth();
        }
        return this.padRight;
    }

    public void setPadRight(float right) {
        this.padRight = right;
    }

    public float getPadTop() {
        if (this.padTop == -1.0f) {
            return this.getTopHeight();
        }
        return this.padTop;
    }

    public void setPadTop(float top) {
        this.padTop = top;
    }

    public float getPadBottom() {
        if (this.padBottom == -1.0f) {
            return this.getBottomHeight();
        }
        return this.padBottom;
    }

    public void setPadBottom(float bottom) {
        this.padBottom = bottom;
    }

    public void scale(float scaleX, float scaleY) {
        this.leftWidth *= scaleX;
        this.rightWidth *= scaleX;
        this.topHeight *= scaleY;
        this.bottomHeight *= scaleY;
        this.middleWidth *= scaleX;
        this.middleHeight *= scaleY;
        if (this.padLeft != -1.0f) {
            this.padLeft *= scaleX;
        }
        if (this.padRight != -1.0f) {
            this.padRight *= scaleX;
        }
        if (this.padTop != -1.0f) {
            this.padTop *= scaleY;
        }
        if (this.padBottom != -1.0f) {
            this.padBottom *= scaleY;
        }
    }

    public Texture getTexture() {
        return this.texture;
    }
}

