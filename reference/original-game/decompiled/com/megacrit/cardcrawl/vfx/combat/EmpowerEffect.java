/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.EmpowerCircleEffect;

public class EmpowerEffect
extends AbstractGameEffect {
    private static final float SHAKE_DURATION = 0.25f;

    public EmpowerEffect(float x, float y) {
        CardCrawlGame.sound.play("CARD_POWER_IMPACT", 0.1f);
        for (int i = 0; i < 18; ++i) {
            AbstractDungeon.effectList.add(new EmpowerCircleEffect(x, y));
        }
        CardCrawlGame.screenShake.rumble(0.25f);
    }

    @Override
    public void update() {
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

