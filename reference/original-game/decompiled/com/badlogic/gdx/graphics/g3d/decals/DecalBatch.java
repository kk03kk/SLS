/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.decals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SortedIntList;

public class DecalBatch
implements Disposable {
    private static final int DEFAULT_SIZE = 1000;
    private float[] vertices;
    private Mesh mesh;
    private final SortedIntList<Array<Decal>> groupList = new SortedIntList();
    private GroupStrategy groupStrategy;
    private final Pool<Array<Decal>> groupPool = new Pool<Array<Decal>>(16){

        @Override
        protected Array<Decal> newObject() {
            return new Array<Decal>(false, 100);
        }
    };
    private final Array<Array<Decal>> usedGroups = new Array(16);

    public DecalBatch(GroupStrategy groupStrategy) {
        this(1000, groupStrategy);
    }

    public DecalBatch(int size, GroupStrategy groupStrategy) {
        this.initialize(size);
        this.setGroupStrategy(groupStrategy);
    }

    public void setGroupStrategy(GroupStrategy groupStrategy) {
        this.groupStrategy = groupStrategy;
    }

    public void initialize(int size) {
        this.vertices = new float[size * 24];
        Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexArray;
        if (Gdx.gl30 != null) {
            vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
        }
        this.mesh = new Mesh(vertexDataType, false, size * 4, size * 6, new VertexAttribute(1, 3, "a_position"), new VertexAttribute(4, 4, "a_color"), new VertexAttribute(16, 2, "a_texCoord0"));
        short[] indices = new short[size * 6];
        int v = 0;
        int i = 0;
        while (i < indices.length) {
            indices[i] = (short)v;
            indices[i + 1] = (short)(v + 2);
            indices[i + 2] = (short)(v + 1);
            indices[i + 3] = (short)(v + 1);
            indices[i + 4] = (short)(v + 2);
            indices[i + 5] = (short)(v + 3);
            i += 6;
            v += 4;
        }
        this.mesh.setIndices(indices);
    }

    public int getSize() {
        return this.vertices.length / 24;
    }

    public void add(Decal decal) {
        int groupIndex = this.groupStrategy.decideGroup(decal);
        Array<Decal> targetGroup = this.groupList.get(groupIndex);
        if (targetGroup == null) {
            targetGroup = this.groupPool.obtain();
            targetGroup.clear();
            this.usedGroups.add(targetGroup);
            this.groupList.insert(groupIndex, targetGroup);
        }
        targetGroup.add(decal);
    }

    public void flush() {
        this.render();
        this.clear();
    }

    protected void render() {
        this.groupStrategy.beforeGroups();
        for (SortedIntList.Node<Array<Decal>> node : this.groupList) {
            this.groupStrategy.beforeGroup(node.index, (Array)node.value);
            ShaderProgram shader = this.groupStrategy.getGroupShader(node.index);
            this.render(shader, (Array)node.value);
            this.groupStrategy.afterGroup(node.index);
        }
        this.groupStrategy.afterGroups();
    }

    private void render(ShaderProgram shader, Array<Decal> decals) {
        DecalMaterial lastMaterial = null;
        int idx = 0;
        for (Decal decal : decals) {
            if (lastMaterial == null || !lastMaterial.equals(decal.getMaterial())) {
                if (idx > 0) {
                    this.flush(shader, idx);
                    idx = 0;
                }
                decal.material.set();
                lastMaterial = decal.material;
            }
            decal.update();
            System.arraycopy(decal.vertices, 0, this.vertices, idx, decal.vertices.length);
            if ((idx += decal.vertices.length) != this.vertices.length) continue;
            this.flush(shader, idx);
            idx = 0;
        }
        if (idx > 0) {
            this.flush(shader, idx);
        }
    }

    protected void flush(ShaderProgram shader, int verticesPosition) {
        this.mesh.setVertices(this.vertices, 0, verticesPosition);
        this.mesh.render(shader, 4, 0, verticesPosition / 4);
    }

    protected void clear() {
        this.groupList.clear();
        this.groupPool.freeAll(this.usedGroups);
        this.usedGroups.clear();
    }

    @Override
    public void dispose() {
        this.clear();
        this.vertices = null;
        this.mesh.dispose();
    }
}

