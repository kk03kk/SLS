/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.utils.Pool;

public class ParticleEffectPool
extends Pool<PooledEffect> {
    private final ParticleEffect effect;

    public ParticleEffectPool(ParticleEffect effect, int initialCapacity, int max) {
        super(initialCapacity, max);
        this.effect = effect;
    }

    @Override
    protected PooledEffect newObject() {
        return new PooledEffect(this.effect);
    }

    @Override
    public PooledEffect obtain() {
        PooledEffect effect = (PooledEffect)super.obtain();
        effect.reset();
        return effect;
    }

    public class PooledEffect
    extends ParticleEffect {
        PooledEffect(ParticleEffect effect) {
            super(effect);
        }

        @Override
        public void reset() {
            super.reset();
        }

        public void free() {
            ParticleEffectPool.this.free(this);
        }
    }
}

