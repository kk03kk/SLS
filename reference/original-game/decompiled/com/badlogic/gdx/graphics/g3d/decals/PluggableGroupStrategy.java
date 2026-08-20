/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.decals;

import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.GroupPlug;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public abstract class PluggableGroupStrategy
implements GroupStrategy {
    private IntMap<GroupPlug> plugs = new IntMap();

    @Override
    public void beforeGroup(int group, Array<Decal> contents) {
        this.plugs.get(group).beforeGroup(contents);
    }

    @Override
    public void afterGroup(int group) {
        this.plugs.get(group).afterGroup();
    }

    public void plugIn(GroupPlug plug, int group) {
        this.plugs.put(group, plug);
    }

    public GroupPlug unPlug(int group) {
        return this.plugs.remove(group);
    }
}

