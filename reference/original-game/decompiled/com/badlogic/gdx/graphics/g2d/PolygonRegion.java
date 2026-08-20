/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PolygonRegion {
    final float[] textureCoords;
    final float[] vertices;
    final short[] triangles;
    final TextureRegion region;

    public PolygonRegion(TextureRegion region, float[] vertices, short[] triangles) {
        this.region = region;
        this.vertices = vertices;
        this.triangles = triangles;
        this.textureCoords = new float[vertices.length];
        float[] textureCoords = this.textureCoords;
        float u = region.u;
        float v = region.v;
        float uvWidth = region.u2 - u;
        float uvHeight = region.v2 - v;
        int width = region.regionWidth;
        int height = region.regionHeight;
        int n = vertices.length;
        for (int i = 0; i < n; ++i) {
            textureCoords[i] = u + uvWidth * (vertices[i] / (float)width);
            textureCoords[++i] = v + uvHeight * (1.0f - vertices[i] / (float)height);
        }
    }

    public float[] getVertices() {
        return this.vertices;
    }

    public short[] getTriangles() {
        return this.triangles;
    }

    public float[] getTextureCoords() {
        return this.textureCoords;
    }

    public TextureRegion getRegion() {
        return this.region;
    }
}

