/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class NodeAnimation {
    public Node node;
    public Array<NodeKeyframe<Vector3>> translation = null;
    public Array<NodeKeyframe<Quaternion>> rotation = null;
    public Array<NodeKeyframe<Vector3>> scaling = null;
}

