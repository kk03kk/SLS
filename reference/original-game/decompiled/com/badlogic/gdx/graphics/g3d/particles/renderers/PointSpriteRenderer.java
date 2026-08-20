/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.renderers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteControllerRenderData;

public class PointSpriteRenderer
extends ParticleControllerRenderer<PointSpriteControllerRenderData, PointSpriteParticleBatch> {
    public PointSpriteRenderer() {
        super(new PointSpriteControllerRenderData());
    }

    public PointSpriteRenderer(PointSpriteParticleBatch batch) {
        this();
        this.setBatch(batch);
    }

    @Override
    public void allocateChannels() {
        ((PointSpriteControllerRenderData)this.renderData).positionChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Position);
        ((PointSpriteControllerRenderData)this.renderData).regionChannel = this.controller.particles.addChannel(ParticleChannels.TextureRegion, ParticleChannels.TextureRegionInitializer.get());
        ((PointSpriteControllerRenderData)this.renderData).colorChannel = this.controller.particles.addChannel(ParticleChannels.Color, ParticleChannels.ColorInitializer.get());
        ((PointSpriteControllerRenderData)this.renderData).scaleChannel = this.controller.particles.addChannel(ParticleChannels.Scale, ParticleChannels.ScaleInitializer.get());
        ((PointSpriteControllerRenderData)this.renderData).rotationChannel = this.controller.particles.addChannel(ParticleChannels.Rotation2D, ParticleChannels.Rotation2dInitializer.get());
    }

    @Override
    public boolean isCompatible(ParticleBatch<?> batch) {
        return batch instanceof PointSpriteParticleBatch;
    }

    @Override
    public ParticleControllerComponent copy() {
        return new PointSpriteRenderer((PointSpriteParticleBatch)this.batch);
    }
}

