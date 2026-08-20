/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.WebLineEffect;
import com.megacrit.cardcrawl.vfx.combat.WebParticleEffect;

public class WebEffect
extends AbstractGameEffect {
    private float timer = 0.0f;
    private int count = 0;
    private AbstractCreature target;
    private float srcX;
    private float srcY;

    public WebEffect(AbstractCreature target, float srcX, float srcY) {
        this.target = target;
        this.srcX = srcX;
        this.srcY = srcY;
        this.duration = 1.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.timer -= Gdx.graphics.getDeltaTime();
        if (this.timer < 0.0f) {
            this.timer += 0.1f;
            switch (this.count) {
                case 0: {
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    AbstractDungeon.effectsQueue.add(new WebParticleEffect(this.target.hb.cX - 90.0f * Settings.scale, this.target.hb.cY - 10.0f * Settings.scale));
                    break;
                }
                case 1: {
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    break;
                }
                case 2: {
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    AbstractDungeon.effectsQueue.add(new WebLineEffect(this.srcX, this.srcY, true));
                    AbstractDungeon.effectsQueue.add(new WebParticleEffect(this.target.hb.cX + 70.0f * Settings.scale, this.target.hb.cY + 80.0f * Settings.scale));
                    break;
                }
                case 3: {
                    break;
                }
                case 4: {
                    AbstractDungeon.effectsQueue.add(new WebParticleEffect(this.target.hb.cX + 30.0f * Settings.scale, this.target.hb.cY - 100.0f * Settings.scale));
                    break;
                }
            }
            ++this.count;
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

