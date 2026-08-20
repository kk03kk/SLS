/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;

public class VertexAttachment
extends Attachment {
    int[] bones;
    float[] vertices;
    int worldVerticesLength;

    public VertexAttachment(String name) {
        super(name);
    }

    protected void computeWorldVertices(Slot slot, float[] worldVertices) {
        this.computeWorldVertices(slot, 0, this.worldVerticesLength, worldVertices, 0);
    }

    protected void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset) {
        count += offset;
        Skeleton skeleton = slot.getSkeleton();
        float x = skeleton.getX();
        float y = skeleton.getY();
        FloatArray deformArray = slot.getAttachmentVertices();
        float[] vertices = this.vertices;
        int[] bones = this.bones;
        if (bones == null) {
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
            int v = start;
            for (int w = offset; w < count; w += 2) {
                float vx = vertices[v];
                float vy = vertices[v + 1];
                worldVertices[w] = vx * a + vy * b + x;
                worldVertices[w + 1] = vx * c + vy * d + y;
                v += 2;
            }
            return;
        }
        int v = 0;
        int skip = 0;
        for (int i = 0; i < start; i += 2) {
            int n = bones[v];
            v += n + 1;
            skip += n;
        }
        T[] skeletonBones = skeleton.getBones().items;
        if (deformArray.size == 0) {
            int b = skip * 3;
            for (int w = offset; w < count; w += 2) {
                float wx = x;
                float wy = y;
                int n = bones[v++];
                n += v;
                while (v < n) {
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
            }
        } else {
            float[] deform = deformArray.items;
            int b = skip * 3;
            int f = skip << 1;
            for (int w = offset; w < count; w += 2) {
                float wx = x;
                float wy = y;
                int n = bones[v++];
                n += v;
                while (v < n) {
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
            }
        }
    }

    public boolean applyDeform(VertexAttachment sourceAttachment) {
        return this == sourceAttachment;
    }

    public int[] getBones() {
        return this.bones;
    }

    public void setBones(int[] bones) {
        this.bones = bones;
    }

    public float[] getVertices() {
        return this.vertices;
    }

    public void setVertices(float[] vertices) {
        this.vertices = vertices;
    }

    public int getWorldVerticesLength() {
        return this.worldVerticesLength;
    }

    public void setWorldVerticesLength(int worldVerticesLength) {
        this.worldVerticesLength = worldVerticesLength;
    }
}

