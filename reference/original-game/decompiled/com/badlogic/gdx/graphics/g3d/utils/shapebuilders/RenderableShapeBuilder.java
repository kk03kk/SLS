/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils.shapebuilders;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BaseShapeBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FlushablePool;

public class RenderableShapeBuilder
extends BaseShapeBuilder {
    private static short[] indices;
    private static float[] vertices;
    private static final RenderablePool renderablesPool;
    private static final Array<Renderable> renderables;
    private static final int FLOAT_BYTES = 4;

    public static void buildNormals(MeshPartBuilder builder, RenderableProvider renderableProvider, float vectorSize) {
        RenderableShapeBuilder.buildNormals(builder, renderableProvider, vectorSize, tmpColor0.set(0.0f, 0.0f, 1.0f, 1.0f), tmpColor1.set(1.0f, 0.0f, 0.0f, 1.0f), tmpColor2.set(0.0f, 1.0f, 0.0f, 1.0f));
    }

    public static void buildNormals(MeshPartBuilder builder, RenderableProvider renderableProvider, float vectorSize, Color normalColor, Color tangentColor, Color binormalColor) {
        renderableProvider.getRenderables(renderables, renderablesPool);
        for (Renderable renderable : renderables) {
            RenderableShapeBuilder.buildNormals(builder, renderable, vectorSize, normalColor, tangentColor, binormalColor);
        }
        renderablesPool.flush();
        renderables.clear();
    }

    public static void buildNormals(MeshPartBuilder builder, Renderable renderable, float vectorSize, Color normalColor, Color tangentColor, Color binormalColor) {
        Mesh mesh = renderable.meshPart.mesh;
        int positionOffset = -1;
        if (mesh.getVertexAttribute(1) != null) {
            positionOffset = mesh.getVertexAttribute((int)1).offset / 4;
        }
        int normalOffset = -1;
        if (mesh.getVertexAttribute(8) != null) {
            normalOffset = mesh.getVertexAttribute((int)8).offset / 4;
        }
        int tangentOffset = -1;
        if (mesh.getVertexAttribute(128) != null) {
            tangentOffset = mesh.getVertexAttribute((int)128).offset / 4;
        }
        int binormalOffset = -1;
        if (mesh.getVertexAttribute(256) != null) {
            binormalOffset = mesh.getVertexAttribute((int)256).offset / 4;
        }
        int attributesSize = mesh.getVertexSize() / 4;
        int verticesOffset = 0;
        int verticesQuantity = 0;
        if (mesh.getNumIndices() > 0) {
            RenderableShapeBuilder.ensureIndicesCapacity(mesh.getNumIndices());
            mesh.getIndices(renderable.meshPart.offset, renderable.meshPart.size, indices, 0);
            short minVertice = RenderableShapeBuilder.minVerticeInIndices();
            short maxVertice = RenderableShapeBuilder.maxVerticeInIndices();
            verticesOffset = minVertice;
            verticesQuantity = maxVertice - minVertice;
        } else {
            verticesOffset = renderable.meshPart.offset;
            verticesQuantity = renderable.meshPart.size;
        }
        RenderableShapeBuilder.ensureVerticesCapacity(verticesQuantity * attributesSize);
        mesh.getVertices(verticesOffset * attributesSize, verticesQuantity * attributesSize, vertices, 0);
        for (int i = verticesOffset; i < verticesQuantity; ++i) {
            int id = i * attributesSize;
            tmpV0.set(vertices[id + positionOffset], vertices[id + positionOffset + 1], vertices[id + positionOffset + 2]);
            if (normalOffset != -1) {
                tmpV1.set(vertices[id + normalOffset], vertices[id + normalOffset + 1], vertices[id + normalOffset + 2]);
                tmpV2.set(tmpV0).add(tmpV1.scl(vectorSize));
            }
            if (tangentOffset != -1) {
                tmpV3.set(vertices[id + tangentOffset], vertices[id + tangentOffset + 1], vertices[id + tangentOffset + 2]);
                tmpV4.set(tmpV0).add(tmpV3.scl(vectorSize));
            }
            if (binormalOffset != -1) {
                tmpV5.set(vertices[id + binormalOffset], vertices[id + binormalOffset + 1], vertices[id + binormalOffset + 2]);
                tmpV6.set(tmpV0).add(tmpV5.scl(vectorSize));
            }
            tmpV0.mul(renderable.worldTransform);
            tmpV2.mul(renderable.worldTransform);
            tmpV4.mul(renderable.worldTransform);
            tmpV6.mul(renderable.worldTransform);
            if (normalOffset != -1) {
                builder.setColor(normalColor);
                builder.line(tmpV0, tmpV2);
            }
            if (tangentOffset != -1) {
                builder.setColor(tangentColor);
                builder.line(tmpV0, tmpV4);
            }
            if (binormalOffset == -1) continue;
            builder.setColor(binormalColor);
            builder.line(tmpV0, tmpV6);
        }
    }

    private static void ensureVerticesCapacity(int capacity) {
        if (vertices == null || vertices.length < capacity) {
            vertices = new float[capacity];
        }
    }

    private static void ensureIndicesCapacity(int capacity) {
        if (indices == null || indices.length < capacity) {
            indices = new short[capacity];
        }
    }

    private static short minVerticeInIndices() {
        short min = Short.MAX_VALUE;
        for (int i = 0; i < indices.length; ++i) {
            if (indices[i] >= min) continue;
            min = indices[i];
        }
        return min;
    }

    private static short maxVerticeInIndices() {
        short max = Short.MIN_VALUE;
        for (int i = 0; i < indices.length; ++i) {
            if (indices[i] <= max) continue;
            max = indices[i];
        }
        return max;
    }

    static {
        renderablesPool = new RenderablePool();
        renderables = new Array();
    }

    private static class RenderablePool
    extends FlushablePool<Renderable> {
        @Override
        protected Renderable newObject() {
            return new Renderable();
        }

        @Override
        public Renderable obtain() {
            Renderable renderable = (Renderable)super.obtain();
            renderable.environment = null;
            renderable.material = null;
            renderable.meshPart.set("", null, 0, 0, 0);
            renderable.shader = null;
            return renderable;
        }
    }
}

