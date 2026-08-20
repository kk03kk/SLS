/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.environment;

import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.Matrix4;

public interface ShadowMap {
    public Matrix4 getProjViewTrans();

    public TextureDescriptor getDepthMap();
}

