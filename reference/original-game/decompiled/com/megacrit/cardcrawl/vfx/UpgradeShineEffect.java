/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.UpgradeHammerImprintEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineParticleEffect;

public class UpgradeShineEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private boolean clang1 = false;
    private boolean clang2 = false;

    public UpgradeShineEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.duration = 0.8f;
    }

    @Override
    public void update() {
        if (this.duration < 0.6f && !this.clang1) {
            CardCrawlGame.sound.play("CARD_UPGRADE");
            this.clang1 = true;
            this.clank(this.x - 80.0f * Settings.scale, this.y + 0.0f * Settings.scale);
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, false);
        }
        if (this.duration < 0.2f && !this.clang2) {
            this.clang2 = true;
            this.clank(this.x + 90.0f * Settings.scale, this.y - 110.0f * Settings.scale);
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, false);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.clank(this.x + 30.0f * Settings.scale, this.y + 120.0f * Settings.scale);
            this.isDone = true;
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    private void clank(float x, float y) {
        AbstractDungeon.topLevelEffectsQueue.add(new UpgradeHammerImprintEffect(x, y));
        if (Settings.DISABLE_EFFECTS) {
            return;
        }
        for (int i = 0; i < 30; ++i) {
            AbstractDungeon.topLevelEffectsQueue.add(new UpgradeShineParticleEffect(x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, y + MathUtils.random(-10.0f, 10.0f) * Settings.scale));
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}

