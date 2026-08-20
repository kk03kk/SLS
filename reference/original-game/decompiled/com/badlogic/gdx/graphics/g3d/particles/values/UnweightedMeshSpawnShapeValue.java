/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public final class UnweightedMeshSpawnShapeValue
extends MeshSpawnShapeValue {
    private float[] vertices;
    private short[] indices;
    private int positionOffset;
    private int vertexSize;
    private int vertexCount;
    private int triangleCount;

    public UnweightedMeshSpawnShapeValue(UnweightedMeshSpawnShapeValue value) {
        super(value);
        this.load(value);
    }

    public UnweightedMeshSpawnShapeValue() {
    }

    @Override
    public void setMesh(Mesh mesh, Model model) {
        super.setMesh(mesh, model);
        this.vertexSize = mesh.getVertexSize() / 4;
        this.positionOffset = mesh.getVertexAttribute((int)1).offset / 4;
        int indicesCount = mesh.getNumIndices();
        if (indicesCount > 0) {
            this.indices = new short[indicesCount];
            mesh.getIndices(this.indices);
            this.triangleCount = this.indices.length / 3;
        } else {
            this.indices = null;
        }
        this.vertexCount = mesh.getNumVertices();
        this.vertices = new float[this.vertexCount * this.vertexSize];
        mesh.getVertices(this.vertices);
    }

    @Override
    public void spawnAux(Vector3 vector, float percent) {
        if (this.indices == null) {
            int triangleIndex = MathUtils.random(this.vertexCount - 3) * this.vertexSize;
            int p1Offset = triangleIndex + this.positionOffset;
            int p2Offset = p1Offset + this.vertexSize;
            int p3Offset = p2Offset + this.vertexSize;
            float x1 = this.vertices[p1Offset];
            float y1 = this.vertices[p1Offset + 1];
            float z1 = this.vertices[p1Offset + 2];
            float x2 = this.vertices[p2Offset];
            float y2 = this.vertices[p2Offset + 1];
            float z2 = this.vertices[p2Offset + 2];
            float x3 = this.vertices[p3Offset];
            float y3 = this.vertices[p3Offset + 1];
            float z3 = this.vertices[p3Offset + 2];
            MeshSpawnShapeValue.Triangle.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector);
        } else {
            int triangleIndex = MathUtils.random(this.triangleCount - 1) * 3;
            int p1Offset = this.indices[triangleIndex] * this.vertexSize + this.positionOffset;
            int p2Offset = this.indices[triangleIndex + 1] * this.vertexSize + this.positionOffset;
            int p3Offset = this.indices[triangleIndex + 2] * this.vertexSize + this.positionOffset;
            float x1 = this.vertices[p1Offset];
            float y1 = this.vertices[p1Offset + 1];
            float z1 = this.vertices[p1Offset + 2];
            float x2 = this.vertices[p2Offset];
            float y2 = this.vertices[p2Offset + 1];
            float z2 = this.vertices[p2Offset + 2];
            float x3 = this.vertices[p3Offset];
            float y3 = this.vertices[p3Offset + 1];
            float z3 = this.vertices[p3Offset + 2];
            MeshSpawnShapeValue.Triangle.pick(x1, y1, z1, x2, y2, z2, x3, y3, z3, vector);
        }
    }

    @Override
    public SpawnShapeValue copy() {
        return new UnweightedMeshSpawnShapeValue(this);
    }
}

