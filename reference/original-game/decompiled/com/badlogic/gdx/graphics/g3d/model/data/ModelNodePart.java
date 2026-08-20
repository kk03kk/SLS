/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model.data;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ArrayMap;

public class ModelNodePart {
    public String materialId;
    public String meshPartId;
    public ArrayMap<String, Matrix4> bones;
    public int[][] uvMapping;
}

