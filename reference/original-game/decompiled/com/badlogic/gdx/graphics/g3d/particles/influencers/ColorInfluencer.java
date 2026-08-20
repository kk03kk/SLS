/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.values.GradientColorValue;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class ColorInfluencer
extends Influencer {
    ParallelArray.FloatChannel colorChannel;

    @Override
    public void allocateChannels() {
        this.colorChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Color);
    }

    public static class Single
    extends ColorInfluencer {
        ParallelArray.FloatChannel alphaInterpolationChannel;
        ParallelArray.FloatChannel lifeChannel;
        public ScaledNumericValue alphaValue;
        public GradientColorValue colorValue = new GradientColorValue();

        public Single() {
            this.alphaValue = new ScaledNumericValue();
            this.alphaValue.setHigh(1.0f);
        }

        public Single(Single billboardColorInfluencer) {
            this();
            this.set(billboardColorInfluencer);
        }

        public void set(Single colorInfluencer) {
            this.colorValue.load(colorInfluencer.colorValue);
            this.alphaValue.load(colorInfluencer.alphaValue);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            ParticleChannels.Interpolation.id = this.controller.particleChannels.newId();
            this.alphaInterpolationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Interpolation);
            this.lifeChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Life);
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int i = startIndex * this.colorChannel.strideSize;
            int a = startIndex * this.alphaInterpolationChannel.strideSize;
            int l = startIndex * this.lifeChannel.strideSize + 2;
            int c = i + count * this.colorChannel.strideSize;
            while (i < c) {
                float alphaStart = this.alphaValue.newLowValue();
                float alphaDiff = this.alphaValue.newHighValue() - alphaStart;
                this.colorValue.getColor(0.0f, this.colorChannel.data, i);
                this.colorChannel.data[i + 3] = alphaStart + alphaDiff * this.alphaValue.getScale(this.lifeChannel.data[l]);
                this.alphaInterpolationChannel.data[a + 0] = alphaStart;
                this.alphaInterpolationChannel.data[a + 1] = alphaDiff;
                i += this.colorChannel.strideSize;
                a += this.alphaInterpolationChannel.strideSize;
                l += this.lifeChannel.strideSize;
            }
        }

        @Override
        public void update() {
            int i = 0;
            int a = 0;
            int l = 2;
            int c = i + this.controller.particles.size * this.colorChannel.strideSize;
            while (i < c) {
                float lifePercent = this.lifeChannel.data[l];
                this.colorValue.getColor(lifePercent, this.colorChannel.data, i);
                this.colorChannel.data[i + 3] = this.alphaInterpolationChannel.data[a + 0] + this.alphaInterpolationChannel.data[a + 1] * this.alphaValue.getScale(lifePercent);
                i += this.colorChannel.strideSize;
                a += this.alphaInterpolationChannel.strideSize;
                l += this.lifeChannel.strideSize;
            }
        }

        @Override
        public Single copy() {
            return new Single(this);
        }

        @Override
        public void write(Json json) {
            json.writeValue("alpha", this.alphaValue);
            json.writeValue("color", this.colorValue);
        }

        @Override
        public void read(Json json, JsonValue jsonData) {
            this.alphaValue = json.readValue("alpha", ScaledNumericValue.class, jsonData);
            this.colorValue = json.readValue("color", GradientColorValue.class, jsonData);
        }
    }

    public static class Random
    extends ColorInfluencer {
        ParallelArray.FloatChannel colorChannel;

        @Override
        public void allocateChannels() {
            this.colorChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Color);
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int i;
            int c = i + count * this.colorChannel.strideSize;
            for (i = startIndex * this.colorChannel.strideSize; i < c; i += this.colorChannel.strideSize) {
                this.colorChannel.data[i + 0] = MathUtils.random();
                this.colorChannel.data[i + 1] = MathUtils.random();
                this.colorChannel.data[i + 2] = MathUtils.random();
                this.colorChannel.data[i + 3] = MathUtils.random();
            }
        }

        @Override
        public Random copy() {
            return new Random();
        }
    }
}

