/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model.data;

import com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ModelData {
    public String id;
    public final short[] version = new short[2];
    public final Array<ModelMesh> meshes = new Array();
    public final Array<ModelMaterial> materials = new Array();
    public final Array<ModelNode> nodes = new Array();
    public final Array<ModelAnimation> animations = new Array();

    public void addMesh(ModelMesh mesh) {
        for (ModelMesh other : this.meshes) {
            if (!other.id.equals(mesh.id)) continue;
            throw new GdxRuntimeException("Mesh with id '" + other.id + "' already in model");
        }
        this.meshes.add(mesh);
    }
}

