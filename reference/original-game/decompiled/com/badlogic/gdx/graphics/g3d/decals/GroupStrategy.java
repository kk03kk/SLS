/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.decals;

import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

public interface GroupStrategy {
    public ShaderProgram getGroupShader(int var1);

    public int decideGroup(Decal var1);

    public void beforeGroup(int var1, Array<Decal> var2);

    public void afterGroup(int var1);

    public void beforeGroups();

    public void afterGroups();
}

