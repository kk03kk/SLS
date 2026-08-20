/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.emitters;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.emitters.Emitter;
import com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class RegularEmitter
extends Emitter
implements Json.Serializable {
    public RangedNumericValue delayValue = new RangedNumericValue();
    public RangedNumericValue durationValue = new RangedNumericValue();
    public ScaledNumericValue lifeOffsetValue = new ScaledNumericValue();
    public ScaledNumericValue lifeValue = new ScaledNumericValue();
    public ScaledNumericValue emissionValue = new ScaledNumericValue();
    protected int emission;
    protected int emissionDiff;
    protected int emissionDelta;
    protected int lifeOffset;
    protected int lifeOffsetDiff;
    protected int life;
    protected int lifeDiff;
    protected float duration;
    protected float delay;
    protected float durationTimer;
    protected float delayTimer;
    private boolean continuous;
    private EmissionMode emissionMode;
    private ParallelArray.FloatChannel lifeChannel;

    public RegularEmitter() {
        this.durationValue.setActive(true);
        this.emissionValue.setActive(true);
        this.lifeValue.setActive(true);
        this.continuous = true;
        this.emissionMode = EmissionMode.Enabled;
    }

    public RegularEmitter(RegularEmitter regularEmitter) {
        this();
        this.set(regularEmitter);
    }

    @Override
    public void allocateChannels() {
        this.lifeChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Life);
    }

    @Override
    public void start() {
        this.delay = this.delayValue.active ? this.delayValue.newLowValue() : 0.0f;
        this.delayTimer = 0.0f;
        this.durationTimer = 0.0f;
        this.duration = this.durationValue.newLowValue();
        this.percent = this.durationTimer / this.duration;
        this.emission = (int)this.emissionValue.newLowValue();
        this.emissionDiff = (int)this.emissionValue.newHighValue();
        if (!this.emissionValue.isRelative()) {
            this.emissionDiff -= this.emission;
        }
        this.life = (int)this.lifeValue.newLowValue();
        this.lifeDiff = (int)this.lifeValue.newHighValue();
        if (!this.lifeValue.isRelative()) {
            this.lifeDiff -= this.life;
        }
        this.lifeOffset = this.lifeOffsetValue.active ? (int)this.lifeOffsetValue.newLowValue() : 0;
        this.lifeOffsetDiff = (int)this.lifeOffsetValue.newHighValue();
        if (!this.lifeOffsetValue.isRelative()) {
            this.lifeOffsetDiff -= this.lifeOffset;
        }
    }

    @Override
    public void init() {
        super.init();
        this.emissionDelta = 0;
        this.durationTimer = this.duration;
    }

    @Override
    public void activateParticles(int startIndex, int count) {
        int i;
        int currentTotaLife;
        int currentLife = currentTotaLife = this.life + (int)((float)this.lifeDiff * this.lifeValue.getScale(this.percent));
        int offsetTime = (int)((float)this.lifeOffset + (float)this.lifeOffsetDiff * this.lifeOffsetValue.getScale(this.percent));
        if (offsetTime > 0) {
            if (offsetTime >= currentLife) {
                offsetTime = currentLife - 1;
            }
            currentLife -= offsetTime;
        }
        float lifePercent = 1.0f - (float)currentLife / (float)currentTotaLife;
        int c = i + count * this.lifeChannel.strideSize;
        for (i = startIndex * this.lifeChannel.strideSize; i < c; i += this.lifeChannel.strideSize) {
            this.lifeChannel.data[i + 0] = currentLife;
            this.lifeChannel.data[i + 1] = currentTotaLife;
            this.lifeChannel.data[i + 2] = lifePercent;
        }
    }

    @Override
    public void update() {
        int deltaMillis = (int)(this.controller.deltaTime * 1000.0f);
        if (this.delayTimer < this.delay) {
            this.delayTimer += (float)deltaMillis;
        } else {
            boolean emit;
            boolean bl = emit = this.emissionMode != EmissionMode.Disabled;
            if (this.durationTimer < this.duration) {
                this.durationTimer += (float)deltaMillis;
                this.percent = this.durationTimer / this.duration;
            } else if (this.continuous && emit && this.emissionMode == EmissionMode.Enabled) {
                this.controller.start();
            } else {
                emit = false;
            }
            if (emit) {
                this.emissionDelta += deltaMillis;
                float emissionTime = (float)this.emission + (float)this.emissionDiff * this.emissionValue.getScale(this.percent);
                if (emissionTime > 0.0f && (float)this.emissionDelta >= (emissionTime = 1000.0f / emissionTime)) {
                    int emitCount = (int)((float)this.emissionDelta / emissionTime);
                    emitCount = Math.min(emitCount, this.maxParticleCount - this.controller.particles.size);
                    this.emissionDelta = (int)((float)this.emissionDelta - (float)emitCount * emissionTime);
                    this.emissionDelta = (int)((float)this.emissionDelta % emissionTime);
                    this.addParticles(emitCount);
                }
                if (this.controller.particles.size < this.minParticleCount) {
                    this.addParticles(this.minParticleCount - this.controller.particles.size);
                }
            }
        }
        int activeParticles = this.controller.particles.size;
        int i = 0;
        int k = 0;
        while (i < this.controller.particles.size) {
            int n = k + 0;
            float f = this.lifeChannel.data[n] = this.lifeChannel.data[n] - (float)deltaMillis;
            if (f <= 0.0f) {
                this.controller.particles.removeElement(i);
                continue;
            }
            this.lifeChannel.data[k + 2] = 1.0f - this.lifeChannel.data[k + 0] / this.lifeChannel.data[k + 1];
            ++i;
            k += this.lifeChannel.strideSize;
        }
        if (this.controller.particles.size < activeParticles) {
            this.controller.killParticles(this.controller.particles.size, activeParticles - this.controller.particles.size);
        }
    }

    private void addParticles(int count) {
        if ((count = Math.min(count, this.maxParticleCount - this.controller.particles.size)) <= 0) {
            return;
        }
        this.controller.activateParticles(this.controller.particles.size, count);
        this.controller.particles.size += count;
    }

    public ScaledNumericValue getLife() {
        return this.lifeValue;
    }

    public ScaledNumericValue getEmission() {
        return this.emissionValue;
    }

    public RangedNumericValue getDuration() {
        return this.durationValue;
    }

    public RangedNumericValue getDelay() {
        return this.delayValue;
    }

    public ScaledNumericValue getLifeOffset() {
        return this.lifeOffsetValue;
    }

    public boolean isContinuous() {
        return this.continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public EmissionMode getEmissionMode() {
        return this.emissionMode;
    }

    public void setEmissionMode(EmissionMode emissionMode) {
        this.emissionMode = emissionMode;
    }

    @Override
    public boolean isComplete() {
        if (this.delayTimer < this.delay) {
            return false;
        }
        return this.durationTimer >= this.duration && this.controller.particles.size == 0;
    }

    public float getPercentComplete() {
        if (this.delayTimer < this.delay) {
            return 0.0f;
        }
        return Math.min(1.0f, this.durationTimer / this.duration);
    }

    public void set(RegularEmitter emitter) {
        super.set(emitter);
        this.delayValue.load(emitter.delayValue);
        this.durationValue.load(emitter.durationValue);
        this.lifeOffsetValue.load(emitter.lifeOffsetValue);
        this.lifeValue.load(emitter.lifeValue);
        this.emissionValue.load(emitter.emissionValue);
        this.emission = emitter.emission;
        this.emissionDiff = emitter.emissionDiff;
        this.emissionDelta = emitter.emissionDelta;
        this.lifeOffset = emitter.lifeOffset;
        this.lifeOffsetDiff = emitter.lifeOffsetDiff;
        this.life = emitter.life;
        this.lifeDiff = emitter.lifeDiff;
        this.duration = emitter.duration;
        this.delay = emitter.delay;
        this.durationTimer = emitter.durationTimer;
        this.delayTimer = emitter.delayTimer;
        this.continuous = emitter.continuous;
    }

    @Override
    public ParticleControllerComponent copy() {
        return new RegularEmitter(this);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("continous", this.continuous);
        json.writeValue("emission", this.emissionValue);
        json.writeValue("delay", this.delayValue);
        json.writeValue("duration", this.durationValue);
        json.writeValue("life", this.lifeValue);
        json.writeValue("lifeOffset", this.lifeOffsetValue);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.continuous = json.readValue("continous", Boolean.TYPE, jsonData);
        this.emissionValue = json.readValue("emission", ScaledNumericValue.class, jsonData);
        this.delayValue = json.readValue("delay", RangedNumericValue.class, jsonData);
        this.durationValue = json.readValue("duration", RangedNumericValue.class, jsonData);
        this.lifeValue = json.readValue("life", ScaledNumericValue.class, jsonData);
        this.lifeOffsetValue = json.readValue("lifeOffset", ScaledNumericValue.class, jsonData);
    }

    public static enum EmissionMode {
        Enabled,
        EnabledUntilCycleEnd,
        Disabled;

    }
}

