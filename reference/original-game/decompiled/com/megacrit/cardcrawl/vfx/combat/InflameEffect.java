/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ExhaustEmberEffect;
import com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect;

public class InflameEffect
extends AbstractGameEffect {
    float x;
    float y;

    public InflameEffect(AbstractCreature creature) {
        this.x = creature.hb.cX;
        this.y = creature.hb.cY;
    }

    @Override
    public void update() {
        int i;
        CardCrawlGame.sound.play("ATTACK_FIRE");
        for (i = 0; i < 75; ++i) {
            AbstractDungeon.effectsQueue.add(new FlameParticleEffect(this.x, this.y));
        }
        for (i = 0; i < 20; ++i) {
            AbstractDungeon.effectsQueue.add(new ExhaustEmberEffect(this.x, this.y));
        }
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

