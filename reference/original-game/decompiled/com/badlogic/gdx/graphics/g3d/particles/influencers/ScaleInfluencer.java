/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.influencers.SimpleInfluencer;

public class ScaleInfluencer
extends SimpleInfluencer {
    public ScaleInfluencer() {
        this.valueChannelDescriptor = ParticleChannels.Scale;
    }

    @Override
    public void activateParticles(int startIndex, int count) {
        if (this.value.isRelative()) {
            int i = startIndex * this.valueChannel.strideSize;
            int a = startIndex * this.interpolationChannel.strideSize;
            int c = i + count * this.valueChannel.strideSize;
            while (i < c) {
                float start = this.value.newLowValue() * this.controller.scale.x;
                float diff = this.value.newHighValue() * this.controller.scale.x;
                this.interpolationChannel.data[a + 0] = start;
                this.interpolationChannel.data[a + 1] = diff;
                this.valueChannel.data[i] = start + diff * this.value.getScale(0.0f);
                i += this.valueChannel.strideSize;
                a += this.interpolationChannel.strideSize;
            }
        } else {
            int i = startIndex * this.valueChannel.strideSize;
            int a = startIndex * this.interpolationChannel.strideSize;
            int c = i + count * this.valueChannel.strideSize;
            while (i < c) {
                float start = this.value.newLowValue() * this.controller.scale.x;
                float diff = this.value.newHighValue() * this.controller.scale.x - start;
                this.interpolationChannel.data[a + 0] = start;
                this.interpolationChannel.data[a + 1] = diff;
                this.valueChannel.data[i] = start + diff * this.value.getScale(0.0f);
                i += this.valueChannel.strideSize;
                a += this.interpolationChannel.strideSize;
            }
        }
    }

    public ScaleInfluencer(ScaleInfluencer scaleInfluencer) {
        super(scaleInfluencer);
    }

    @Override
    public ParticleControllerComponent copy() {
        return new ScaleInfluencer(this);
    }
}

