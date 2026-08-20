/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.renderers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderer;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ParticleControllerControllerRenderer
extends ParticleControllerRenderer {
    ParallelArray.ObjectChannel<ParticleController> controllerChannel;

    @Override
    public void init() {
        this.controllerChannel = (ParallelArray.ObjectChannel)this.controller.particles.getChannel(ParticleChannels.ParticleController);
        if (this.controllerChannel == null) {
            throw new GdxRuntimeException("ParticleController channel not found, specify an influencer which will allocate it please.");
        }
    }

    @Override
    public void update() {
        int c = this.controller.particles.size;
        for (int i = 0; i < c; ++i) {
            ((ParticleController[])this.controllerChannel.data)[i].draw();
        }
    }

    @Override
    public ParticleControllerComponent copy() {
        return new ParticleControllerControllerRenderer();
    }

    public boolean isCompatible(ParticleBatch batch) {
        return false;
    }
}

