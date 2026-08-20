/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.batches;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData;

public interface ParticleBatch<T extends ParticleControllerRenderData>
extends RenderableProvider,
ResourceData.Configurable {
    public void begin();

    public void draw(T var1);

    public void end();

    public void save(AssetManager var1, ResourceData var2);

    public void load(AssetManager var1, ResourceData var2);
}

