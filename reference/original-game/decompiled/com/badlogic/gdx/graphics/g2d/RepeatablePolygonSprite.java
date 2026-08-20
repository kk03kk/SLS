/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ShortArray;

public class RepeatablePolygonSprite {
    private TextureRegion region;
    private float density;
    private boolean dirty = true;
    private Array<float[]> parts = new Array();
    private Array<float[]> vertices = new Array();
    private Array<short[]> indices = new Array();
    private int cols;
    private int rows;
    private float gridWidth;
    private float gridHeight;
    public float x = 0.0f;
    public float y = 0.0f;
    private Color color = Color.WHITE;
    private Vector2 offset = new Vector2();

    public void setPolygon(TextureRegion region, float[] vertices) {
        this.setPolygon(region, vertices, -1.0f);
    }

    public void setPolygon(TextureRegion region, float[] vertices, float density) {
        this.region = region;
        vertices = this.offset(vertices);
        Polygon polygon = new Polygon(vertices);
        Polygon tmpPoly = new Polygon();
        Polygon intersectionPoly = new Polygon();
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        Rectangle boundRect = polygon.getBoundingRectangle();
        if (density == -1.0f) {
            density = boundRect.getWidth() / (float)region.getRegionWidth();
        }
        float regionAspectRatio = (float)region.getRegionHeight() / (float)region.getRegionWidth();
        this.cols = (int)Math.ceil(density);
        this.gridWidth = boundRect.getWidth() / density;
        this.gridHeight = regionAspectRatio * this.gridWidth;
        this.rows = (int)Math.ceil(boundRect.getHeight() / this.gridHeight);
        for (int col = 0; col < this.cols; ++col) {
            for (int row = 0; row < this.rows; ++row) {
                float[] verts = new float[8];
                int idx = 0;
                verts[idx++] = (float)col * this.gridWidth;
                verts[idx++] = (float)row * this.gridHeight;
                verts[idx++] = (float)col * this.gridWidth;
                verts[idx++] = (float)(row + 1) * this.gridHeight;
                verts[idx++] = (float)(col + 1) * this.gridWidth;
                verts[idx++] = (float)(row + 1) * this.gridHeight;
                verts[idx++] = (float)(col + 1) * this.gridWidth;
                verts[idx] = (float)row * this.gridHeight;
                tmpPoly.setVertices(verts);
                Intersector.intersectPolygons(polygon, tmpPoly, intersectionPoly);
                verts = intersectionPoly.getVertices();
                if (verts.length > 0) {
                    this.parts.add(this.snapToGrid(verts));
                    ShortArray arr = triangulator.computeTriangles(verts);
                    this.indices.add(arr.toArray());
                    continue;
                }
                this.parts.add(null);
            }
        }
        this.buildVertices();
    }

    private float[] snapToGrid(float[] vertices) {
        for (int i = 0; i < vertices.length; i += 2) {
            float numX = vertices[i] / this.gridWidth % 1.0f;
            float numY = vertices[i + 1] / this.gridHeight % 1.0f;
            if (numX > 0.99f || numX < 0.01f) {
                vertices[i] = this.gridWidth * (float)Math.round(vertices[i] / this.gridWidth);
            }
            if (!(numY > 0.99f) && !(numY < 0.01f)) continue;
            vertices[i + 1] = this.gridHeight * (float)Math.round(vertices[i + 1] / this.gridHeight);
        }
        return vertices;
    }

    private float[] offset(float[] vertices) {
        int i;
        this.offset.set(vertices[0], vertices[1]);
        for (i = 0; i < vertices.length - 1; i += 2) {
            if (this.offset.x > vertices[i]) {
                this.offset.x = vertices[i];
            }
            if (!(this.offset.y > vertices[i + 1])) continue;
            this.offset.y = vertices[i + 1];
        }
        for (i = 0; i < vertices.length; i += 2) {
            int n = i;
            vertices[n] = vertices[n] - this.offset.x;
            int n2 = i + 1;
            vertices[n2] = vertices[n2] - this.offset.y;
        }
        return vertices;
    }

    private void buildVertices() {
        this.vertices.clear();
        for (int i = 0; i < this.parts.size; ++i) {
            float[] verts = this.parts.get(i);
            if (verts == null) continue;
            float[] fullVerts = new float[5 * verts.length / 2];
            int idx = 0;
            int col = i / this.rows;
            int row = i % this.rows;
            for (int j = 0; j < verts.length; j += 2) {
                fullVerts[idx++] = verts[j] + this.offset.x + this.x;
                fullVerts[idx++] = verts[j + 1] + this.offset.y + this.y;
                fullVerts[idx++] = this.color.toFloatBits();
                float u = verts[j] % this.gridWidth / this.gridWidth;
                float v = verts[j + 1] % this.gridHeight / this.gridHeight;
                if (verts[j] == (float)col * this.gridWidth) {
                    u = 0.0f;
                }
                if (verts[j] == (float)(col + 1) * this.gridWidth) {
                    u = 1.0f;
                }
                if (verts[j + 1] == (float)row * this.gridHeight) {
                    v = 0.0f;
                }
                if (verts[j + 1] == (float)(row + 1) * this.gridHeight) {
                    v = 1.0f;
                }
                u = this.region.getU() + (this.region.getU2() - this.region.getU()) * u;
                v = this.region.getV() + (this.region.getV2() - this.region.getV()) * v;
                fullVerts[idx++] = u;
                fullVerts[idx++] = v;
            }
            this.vertices.add(fullVerts);
        }
        this.dirty = false;
    }

    public void draw(PolygonSpriteBatch batch) {
        if (this.dirty) {
            this.buildVertices();
        }
        for (int i = 0; i < this.vertices.size; ++i) {
            batch.draw(this.region.getTexture(), this.vertices.get(i), 0, this.vertices.get(i).length, this.indices.get(i), 0, this.indices.get(i).length);
        }
    }

    public void setColor(Color color) {
        this.color = color;
        this.dirty = true;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.dirty = true;
    }
}

