/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.WindyParticleEffect;

public class WhirlwindEffect
extends AbstractGameEffect {
    private int count = 0;
    private float timer = 0.0f;
    private boolean reverse = false;

    public WhirlwindEffect(Color setColor, boolean reverse) {
        this.color = setColor.cpy();
        this.reverse = reverse;
    }

    public WhirlwindEffect() {
        this(new Color(0.9f, 0.9f, 1.0f, 1.0f), false);
    }

    @Override
    public void update() {
        this.timer -= Gdx.graphics.getDeltaTime();
        if (this.timer < 0.0f) {
            this.timer += 0.05f;
            if (this.count == 0) {
                AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(this.color.cpy()));
            }
            AbstractDungeon.effectsQueue.add(new WindyParticleEffect(this.color, this.reverse));
            ++this.count;
            if (this.count == 18) {
                this.isDone = true;
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

