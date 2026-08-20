/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class SimpleInfluencer
extends Influencer {
    public ScaledNumericValue value = new ScaledNumericValue();
    ParallelArray.FloatChannel valueChannel;
    ParallelArray.FloatChannel interpolationChannel;
    ParallelArray.FloatChannel lifeChannel;
    ParallelArray.ChannelDescriptor valueChannelDescriptor;

    public SimpleInfluencer() {
        this.value.setHigh(1.0f);
    }

    public SimpleInfluencer(SimpleInfluencer billboardScaleinfluencer) {
        this();
        this.set(billboardScaleinfluencer);
    }

    private void set(SimpleInfluencer scaleInfluencer) {
        this.value.load(scaleInfluencer.value);
        this.valueChannelDescriptor = scaleInfluencer.valueChannelDescriptor;
    }

    @Override
    public void allocateChannels() {
        this.valueChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(this.valueChannelDescriptor);
        ParticleChannels.Interpolation.id = this.controller.particleChannels.newId();
        this.interpolationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Interpolation);
        this.lifeChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Life);
    }

    @Override
    public void activateParticles(int startIndex, int count) {
        if (!this.value.isRelative()) {
            int i = startIndex * this.valueChannel.strideSize;
            int a = startIndex * this.interpolationChannel.strideSize;
            int c = i + count * this.valueChannel.strideSize;
            while (i < c) {
                float start = this.value.newLowValue();
                float diff = this.value.newHighValue() - start;
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
                float start = this.value.newLowValue();
                float diff = this.value.newHighValue();
                this.interpolationChannel.data[a + 0] = start;
                this.interpolationChannel.data[a + 1] = diff;
                this.valueChannel.data[i] = start + diff * this.value.getScale(0.0f);
                i += this.valueChannel.strideSize;
                a += this.interpolationChannel.strideSize;
            }
        }
    }

    @Override
    public void update() {
        int i = 0;
        int a = 0;
        int l = 2;
        int c = i + this.controller.particles.size * this.valueChannel.strideSize;
        while (i < c) {
            this.valueChannel.data[i] = this.interpolationChannel.data[a + 0] + this.interpolationChannel.data[a + 1] * this.value.getScale(this.lifeChannel.data[l]);
            i += this.valueChannel.strideSize;
            a += this.interpolationChannel.strideSize;
            l += this.lifeChannel.strideSize;
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("value", this.value);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.value = json.readValue("value", ScaledNumericValue.class, jsonData);
    }
}

