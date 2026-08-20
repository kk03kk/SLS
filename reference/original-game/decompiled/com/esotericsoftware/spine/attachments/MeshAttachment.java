/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.VertexAttachment;

public class MeshAttachment
extends VertexAttachment {
    private TextureRegion region;
    private String path;
    private float[] regionUVs;
    private float[] worldVertices;
    private short[] triangles;
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private int hullLength;
    private MeshAttachment parentMesh;
    private boolean inheritDeform;
    private short[] edges;
    private float width;
    private float height;

    public MeshAttachment(String name) {
        super(name);
    }

    public void setRegion(TextureRegion region) {
        if (region == null) {
            throw new IllegalArgumentException("region cannot be null.");
        }
        this.region = region;
    }

    public TextureRegion getRegion() {
        if (this.region == null) {
            throw new IllegalStateException("Region has not been set: " + this);
        }
        return this.region;
    }

    public void updateUVs() {
        float width;
        float height;
        float u;
        float v;
        float[] regionUVs = this.regionUVs;
        int verticesLength = regionUVs.length;
        int worldVerticesLength = (verticesLength >> 1) * 5;
        if (this.worldVertices == null || this.worldVertices.length != worldVerticesLength) {
            this.worldVertices = new float[worldVerticesLength];
        }
        if (this.region == null) {
            v = 0.0f;
            u = 0.0f;
            height = 1.0f;
            width = 1.0f;
        } else {
            u = this.region.getU();
            v = this.region.getV();
            width = this.region.getU2() - u;
            height = this.region.getV2() - v;
        }
        if (this.region instanceof TextureAtlas.AtlasRegion && ((TextureAtlas.AtlasRegion)this.region).rotate) {
            int i = 0;
            int w = 3;
            while (i < verticesLength) {
                this.worldVertices[w] = u + regionUVs[i + 1] * width;
                this.worldVertices[w + 1] = v + height - regionUVs[i] * height;
                i += 2;
                w += 5;
            }
        } else {
            int i = 0;
            int w = 3;
            while (i < verticesLength) {
                this.worldVertices[w] = u + regionUVs[i] * width;
                this.worldVertices[w + 1] = v + regionUVs[i + 1] * height;
                i += 2;
                w += 5;
            }
        }
    }

    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        Skeleton skeleton = slot.getSkeleton();
        Color skeletonColor = skeleton.getColor();
        Color slotColor = slot.getColor();
        Color meshColor = this.color;
        float alpha = skeletonColor.a * slotColor.a * meshColor.a * 255.0f;
        float multiplier = premultipliedAlpha ? alpha : 255.0f;
        float color = NumberUtils.intToFloatColor((int)alpha << 24 | (int)(skeletonColor.b * slotColor.b * meshColor.b * multiplier) << 16 | (int)(skeletonColor.g * slotColor.g * meshColor.g * multiplier) << 8 | (int)(skeletonColor.r * slotColor.r * meshColor.r * multiplier));
        float x = skeleton.getX();
        float y = skeleton.getY();
        FloatArray deformArray = slot.getAttachmentVertices();
        float[] vertices = this.vertices;
        float[] worldVertices = this.worldVertices;
        int[] bones = this.bones;
        if (bones == null) {
            int verticesLength = vertices.length;
            if (deformArray.size > 0) {
                vertices = deformArray.items;
            }
            Bone bone = slot.getBone();
            x += bone.getWorldX();
            y += bone.getWorldY();
            float a = bone.getA();
            float b = bone.getB();
            float c = bone.getC();
            float d = bone.getD();
            int v = 0;
            int w = 0;
            while (v < verticesLength) {
                float vx = vertices[v];
                float vy = vertices[v + 1];
                worldVertices[w] = vx * a + vy * b + x;
                worldVertices[w + 1] = vx * c + vy * d + y;
                worldVertices[w + 2] = color;
                v += 2;
                w += 5;
            }
            return worldVertices;
        }
        T[] skeletonBones = skeleton.getBones().items;
        if (deformArray.size == 0) {
            int w = 0;
            int v = 0;
            int b = 0;
            int n = bones.length;
            while (v < n) {
                float wx = x;
                float wy = y;
                int nn = bones[v++] + v;
                while (v < nn) {
                    Bone bone = (Bone)skeletonBones[bones[v]];
                    float vx = vertices[b];
                    float vy = vertices[b + 1];
                    float weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                    ++v;
                    b += 3;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
                worldVertices[w + 2] = color;
                w += 5;
            }
        } else {
            float[] deform = deformArray.items;
            int w = 0;
            int v = 0;
            int b = 0;
            int f = 0;
            int n = bones.length;
            while (v < n) {
                float wx = x;
                float wy = y;
                int nn = bones[v++] + v;
                while (v < nn) {
                    Bone bone = (Bone)skeletonBones[bones[v]];
                    float vx = vertices[b] + deform[f];
                    float vy = vertices[b + 1] + deform[f + 1];
                    float weight = vertices[b + 2];
                    wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                    wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                    ++v;
                    b += 3;
                    f += 2;
                }
                worldVertices[w] = wx;
                worldVertices[w + 1] = wy;
                worldVertices[w + 2] = color;
                w += 5;
            }
        }
        return worldVertices;
    }

    @Override
    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this == sourceAttachment || this.inheritDeform && this.parentMesh == sourceAttachment;
    }

    public float[] getWorldVertices() {
        return this.worldVertices;
    }

    public short[] getTriangles() {
        return this.triangles;
    }

    public void setTriangles(short[] triangles) {
        this.triangles = triangles;
    }

    public float[] getRegionUVs() {
        return this.regionUVs;
    }

    public void setRegionUVs(float[] regionUVs) {
        this.regionUVs = regionUVs;
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

    public int getHullLength() {
        return this.hullLength;
    }

    public void setHullLength(int hullLength) {
        this.hullLength = hullLength;
    }

    public void setEdges(short[] edges) {
        this.edges = edges;
    }

    public short[] getEdges() {
        return this.edges;
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

    public MeshAttachment getParentMesh() {
        return this.parentMesh;
    }

    public void setParentMesh(MeshAttachment parentMesh) {
        this.parentMesh = parentMesh;
        if (parentMesh != null) {
            this.bones = parentMesh.bones;
            this.vertices = parentMesh.vertices;
            this.regionUVs = parentMesh.regionUVs;
            this.triangles = parentMesh.triangles;
            this.hullLength = parentMesh.hullLength;
            this.edges = parentMesh.edges;
            this.width = parentMesh.width;
            this.height = parentMesh.height;
        }
    }

    public boolean getInheritDeform() {
        return this.inheritDeform;
    }

    public void setInheritDeform(boolean inheritDeform) {
        this.inheritDeform = inheritDeform;
    }
}

