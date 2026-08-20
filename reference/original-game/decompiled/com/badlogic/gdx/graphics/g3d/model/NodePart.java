/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ArrayMap;

public class NodePart {
    public MeshPart meshPart;
    public Material material;
    public ArrayMap<Node, Matrix4> invBoneBindTransforms;
    public Matrix4[] bones;
    public boolean enabled = true;

    public NodePart() {
    }

    public NodePart(MeshPart meshPart, Material material) {
        this.meshPart = meshPart;
        this.material = material;
    }

    public Renderable setRenderable(Renderable out) {
        out.material = this.material;
        out.meshPart.set(this.meshPart);
        out.bones = this.bones;
        return out;
    }

    public NodePart copy() {
        return new NodePart().set(this);
    }

    protected NodePart set(NodePart other) {
        this.meshPart = new MeshPart(other.meshPart);
        this.material = other.material;
        this.enabled = other.enabled;
        if (other.invBoneBindTransforms == null) {
            this.invBoneBindTransforms = null;
            this.bones = null;
        } else {
            if (this.invBoneBindTransforms == null) {
                this.invBoneBindTransforms = new ArrayMap(true, other.invBoneBindTransforms.size, Node.class, Matrix4.class);
            } else {
                this.invBoneBindTransforms.clear();
            }
            this.invBoneBindTransforms.putAll(other.invBoneBindTransforms);
            if (this.bones == null || this.bones.length != this.invBoneBindTransforms.size) {
                this.bones = new Matrix4[this.invBoneBindTransforms.size];
            }
            for (int i = 0; i < this.bones.length; ++i) {
                if (this.bones[i] != null) continue;
                this.bones[i] = new Matrix4();
            }
        }
        return this;
    }
}

