/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.renderers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData;

public abstract class ParticleControllerRenderer<D extends ParticleControllerRenderData, T extends ParticleBatch<D>>
extends ParticleControllerComponent {
    protected T batch;
    protected D renderData;

    protected ParticleControllerRenderer() {
    }

    protected ParticleControllerRenderer(D renderData) {
        this.renderData = renderData;
    }

    @Override
    public void update() {
        this.batch.draw(this.renderData);
    }

    public boolean setBatch(ParticleBatch<?> batch) {
        if (this.isCompatible(batch)) {
            this.batch = batch;
            return true;
        }
        return false;
    }

    public abstract boolean isCompatible(ParticleBatch<?> var1);

    @Override
    public void set(ParticleController particleController) {
        super.set(particleController);
        if (this.renderData != null) {
            ((ParticleControllerRenderData)this.renderData).controller = this.controller;
        }
    }
}

