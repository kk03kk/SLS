/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.WaterDropEffect;

public class OfferingEffect
extends AbstractGameEffect {
    private int count = 0;
    private float timer = 0.0f;

    @Override
    public void update() {
        this.timer -= Gdx.graphics.getDeltaTime();
        if (this.timer < 0.0f) {
            this.timer += 0.3f;
            switch (this.count) {
                case 0: {
                    CardCrawlGame.sound.playA("ATTACK_FIRE", -0.5f);
                    CardCrawlGame.sound.playA("BLOOD_SPLAT", -0.75f);
                    AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(new Color(1.0f, 0.1f, 0.1f, 1.0f)));
                    AbstractDungeon.effectsQueue.add(new WaterDropEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY + 250.0f * Settings.scale));
                    break;
                }
                case 1: {
                    AbstractDungeon.effectsQueue.add(new WaterDropEffect(AbstractDungeon.player.hb.cX + 150.0f * Settings.scale, AbstractDungeon.player.hb.cY - 80.0f * Settings.scale));
                    break;
                }
                case 2: {
                    AbstractDungeon.effectsQueue.add(new WaterDropEffect(AbstractDungeon.player.hb.cX - 200.0f * Settings.scale, AbstractDungeon.player.hb.cY + 50.0f * Settings.scale));
                    break;
                }
                case 3: {
                    AbstractDungeon.effectsQueue.add(new WaterDropEffect(AbstractDungeon.player.hb.cX + 200.0f * Settings.scale, AbstractDungeon.player.hb.cY + 50.0f * Settings.scale));
                    break;
                }
                case 4: {
                    AbstractDungeon.effectsQueue.add(new WaterDropEffect(AbstractDungeon.player.hb.cX - 150.0f * Settings.scale, AbstractDungeon.player.hb.cY - 80.0f * Settings.scale));
                    break;
                }
            }
            ++this.count;
            if (this.count == 6) {
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

