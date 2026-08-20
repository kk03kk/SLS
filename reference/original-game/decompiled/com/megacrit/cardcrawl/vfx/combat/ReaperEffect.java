/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;

public class ReaperEffect
extends AbstractGameEffect {
    @Override
    public void update() {
        CardCrawlGame.sound.playA("ORB_LIGHTNING_EVOKE", 0.9f);
        CardCrawlGame.sound.playA("ORB_LIGHTNING_PASSIVE", -0.3f);
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m.isDeadOrEscaped()) continue;
            AbstractDungeon.effectsQueue.add(new LightningEffect(m.hb.cX, m.hb.cY));
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

