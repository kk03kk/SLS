/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public final class ParticleSystem
implements RenderableProvider {
    private static ParticleSystem instance;
    private Array<ParticleBatch<?>> batches = new Array();
    private Array<ParticleEffect> effects = new Array();

    public static ParticleSystem get() {
        if (instance == null) {
            instance = new ParticleSystem();
        }
        return instance;
    }

    public void add(ParticleBatch<?> batch) {
        this.batches.add(batch);
    }

    public void add(ParticleEffect effect) {
        this.effects.add(effect);
    }

    public void remove(ParticleEffect effect) {
        this.effects.removeValue(effect, true);
    }

    public void removeAll() {
        this.effects.clear();
    }

    public void update() {
        for (ParticleEffect effect : this.effects) {
            effect.update();
        }
    }

    public void updateAndDraw() {
        for (ParticleEffect effect : this.effects) {
            effect.update();
            effect.draw();
        }
    }

    public void begin() {
        for (ParticleBatch<?> batch : this.batches) {
            batch.begin();
        }
    }

    public void draw() {
        for (ParticleEffect effect : this.effects) {
            effect.draw();
        }
    }

    public void end() {
        for (ParticleBatch<?> batch : this.batches) {
            batch.end();
        }
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        for (ParticleBatch<?> batch : this.batches) {
            batch.getRenderables(renderables, pool);
        }
    }

    public Array<ParticleBatch<?>> getBatches() {
        return this.batches;
    }
}

