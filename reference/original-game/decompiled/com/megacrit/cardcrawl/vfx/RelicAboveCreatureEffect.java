/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class RelicAboveCreatureEffect
extends AbstractGameEffect {
    private static final float TEXT_DURATION = 1.5f;
    private static final float STARTING_OFFSET_Y = 0.0f * Settings.scale;
    private static final float TARGET_OFFSET_Y = 60.0f * Settings.scale;
    private static final float LERP_RATE = 5.0f;
    private static final int W = 128;
    private float x;
    private float y;
    private float offsetY;
    private AbstractRelic relic;
    private Color outlineColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Color shineColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public RelicAboveCreatureEffect(float x, float y, AbstractRelic relic) {
        this.duration = 1.5f;
        this.startingDuration = 1.5f;
        this.relic = relic;
        this.x = x;
        this.y = y;
        this.color = Color.WHITE.cpy();
        this.offsetY = STARTING_OFFSET_Y;
        this.scale = Settings.scale;
    }

    @Override
    public void update() {
        if (this.duration > 1.0f) {
            this.color.a = Interpolation.exp5In.apply(1.0f, 0.0f, (this.duration - 1.0f) * 2.0f);
        }
        super.update();
        this.offsetY = AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.DEFECT ? MathUtils.lerp(this.offsetY, TARGET_OFFSET_Y + 80.0f * Settings.scale, Gdx.graphics.getDeltaTime() * 5.0f) : MathUtils.lerp(this.offsetY, TARGET_OFFSET_Y, Gdx.graphics.getDeltaTime() * 5.0f);
        this.y += Gdx.graphics.getDeltaTime() * 12.0f * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        this.outlineColor.a = this.color.a / 2.0f;
        sb.setColor(this.outlineColor);
        sb.draw(this.relic.outlineImg, this.x - 64.0f, this.y - 64.0f + this.offsetY, 64.0f, 64.0f, 128.0f, 128.0f, this.scale * (2.5f - this.duration), this.scale * (2.5f - this.duration), this.rotation, 0, 0, 128, 128, false, false);
        sb.setColor(this.color);
        sb.draw(this.relic.img, this.x - 64.0f, this.y - 64.0f + this.offsetY, 64.0f, 64.0f, 128.0f, 128.0f, this.scale * (2.5f - this.duration), this.scale * (2.5f - this.duration), this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 1);
        this.shineColor.a = this.color.a / 4.0f;
        sb.setColor(this.shineColor);
        sb.draw(this.relic.img, this.x - 64.0f, this.y - 64.0f + this.offsetY, 64.0f, 64.0f, 128.0f, 128.0f, this.scale * (2.7f - this.duration), this.scale * (2.7f - this.duration), this.rotation, 0, 0, 128, 128, false, false);
        sb.draw(this.relic.img, this.x - 64.0f, this.y - 64.0f + this.offsetY, 64.0f, 64.0f, 128.0f, 128.0f, this.scale * (3.0f - this.duration), this.scale * (3.0f - this.duration), this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

