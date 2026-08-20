/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;

public class RegionAttachment
extends Attachment {
    public static final int BLX = 0;
    public static final int BLY = 1;
    public static final int ULX = 2;
    public static final int ULY = 3;
    public static final int URX = 4;
    public static final int URY = 5;
    public static final int BRX = 6;
    public static final int BRY = 7;
    private TextureRegion region;
    private String path;
    private float x;
    private float y;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation;
    private float width;
    private float height;
    private final float[] vertices = new float[20];
    private final float[] offset = new float[8];
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    public RegionAttachment(String name) {
        super(name);
    }

    public void updateOffset() {
        float width = this.getWidth();
        float height = this.getHeight();
        float localX2 = width / 2.0f;
        float localY2 = height / 2.0f;
        float localX = -localX2;
        float localY = -localY2;
        if (this.region instanceof TextureAtlas.AtlasRegion) {
            TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion)this.region;
            if (region.rotate) {
                localX += region.offsetX / (float)region.originalWidth * width;
                localY += region.offsetY / (float)region.originalHeight * height;
                localX2 -= ((float)region.originalWidth - region.offsetX - (float)region.packedHeight) / (float)region.originalWidth * width;
                localY2 -= ((float)region.originalHeight - region.offsetY - (float)region.packedWidth) / (float)region.originalHeight * height;
            } else {
                localX += region.offsetX / (float)region.originalWidth * width;
                localY += region.offsetY / (float)region.originalHeight * height;
                localX2 -= ((float)region.originalWidth - region.offsetX - (float)region.packedWidth) / (float)region.originalWidth * width;
                localY2 -= ((float)region.originalHeight - region.offsetY - (float)region.packedHeight) / (float)region.originalHeight * height;
            }
        }
        float scaleX = this.getScaleX();
        float scaleY = this.getScaleY();
        localX *= scaleX;
        localY *= scaleY;
        localX2 *= scaleX;
        localY2 *= scaleY;
        float rotation = this.getRotation();
        float cos = MathUtils.cosDeg(rotation);
        float sin = MathUtils.sinDeg(rotation);
        float x = this.getX();
        float y = this.getY();
        float localXCos = localX * cos + x;
        float localXSin = localX * sin;
        float localYCos = localY * cos + y;
        float localYSin = localY * sin;
        float localX2Cos = localX2 * cos + x;
        float localX2Sin = localX2 * sin;
        float localY2Cos = localY2 * cos + y;
        float localY2Sin = localY2 * sin;
        float[] offset = this.offset;
        offset[0] = localXCos - localYSin;
        offset[1] = localYCos + localXSin;
        offset[2] = localXCos - localY2Sin;
        offset[3] = localY2Cos + localXSin;
        offset[4] = localX2Cos - localY2Sin;
        offset[5] = localY2Cos + localX2Sin;
        offset[6] = localX2Cos - localYSin;
        offset[7] = localYCos + localX2Sin;
    }

    public void setRegion(TextureRegion region) {
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null.");
        }
        this.region = region;
        float[] vertices = this.vertices;
        if (region instanceof TextureAtlas.AtlasRegion && ((TextureAtlas.AtlasRegion)region).rotate) {
            vertices[13] = region.getU();
            vertices[14] = region.getV2();
            vertices[18] = region.getU();
            vertices[19] = region.getV();
            vertices[3] = region.getU2();
            vertices[4] = region.getV();
            vertices[8] = region.getU2();
            vertices[9] = region.getV2();
        } else {
            vertices[8] = region.getU();
            vertices[9] = region.getV2();
            vertices[13] = region.getU();
            vertices[14] = region.getV();
            vertices[18] = region.getU2();
            vertices[19] = region.getV();
            vertices[3] = region.getU2();
            vertices[4] = region.getV2();
        }
    }

    public TextureRegion getRegion() {
        if (this.region == null) {
            throw new IllegalStateException("Region has not been set: " + this);
        }
        return this.region;
    }

    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        Skeleton skeleton = slot.getSkeleton();
        Color skeletonColor = skeleton.getColor();
        Color slotColor = slot.getColor();
        Color regionColor = this.color;
        float alpha = skeletonColor.a * slotColor.a * regionColor.a * 255.0f;
        float multiplier = premultipliedAlpha ? alpha : 255.0f;
        float color = NumberUtils.intToFloatColor((int)alpha << 24 | (int)(skeletonColor.b * slotColor.b * regionColor.b * multiplier) << 16 | (int)(skeletonColor.g * slotColor.g * regionColor.g * multiplier) << 8 | (int)(skeletonColor.r * slotColor.r * regionColor.r * multiplier));
        float[] vertices = this.vertices;
        float[] offset = this.offset;
        Bone bone = slot.getBone();
        float x = skeleton.getX() + bone.getWorldX();
        float y = skeleton.getY() + bone.getWorldY();
        float a = bone.getA();
        float b = bone.getB();
        float c = bone.getC();
        float d = bone.getD();
        float offsetX = offset[6];
        float offsetY = offset[7];
        vertices[0] = offsetX * a + offsetY * b + x;
        vertices[1] = offsetX * c + offsetY * d + y;
        vertices[2] = color;
        offsetX = offset[0];
        offsetY = offset[1];
        vertices[5] = offsetX * a + offsetY * b + x;
        vertices[6] = offsetX * c + offsetY * d + y;
        vertices[7] = color;
        offsetX = offset[2];
        offsetY = offset[3];
        vertices[10] = offsetX * a + offsetY * b + x;
        vertices[11] = offsetX * c + offsetY * d + y;
        vertices[12] = color;
        offsetX = offset[4];
        offsetY = offset[5];
        vertices[15] = offsetX * a + offsetY * b + x;
        vertices[16] = offsetX * c + offsetY * d + y;
        vertices[17] = color;
        return vertices;
    }

    public float[] getWorldVertices() {
        return this.vertices;
    }

    public float[] getOffset() {
        return this.offset;
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

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getWidth() {
        return this.width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Color getColor() {
        return this.color;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

