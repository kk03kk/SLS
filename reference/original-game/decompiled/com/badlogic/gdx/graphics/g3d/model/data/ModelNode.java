/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model.data;

import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class ModelNode {
    public String id;
    public Vector3 translation;
    public Quaternion rotation;
    public Vector3 scale;
    public String meshId;
    public ModelNodePart[] parts;
    public ModelNode[] children;
}

