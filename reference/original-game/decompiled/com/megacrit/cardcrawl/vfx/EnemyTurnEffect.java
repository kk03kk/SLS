/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.BattleStartEffect;

public class EnemyTurnEffect
extends AbstractGameEffect {
    private static final float DUR = 2.0f;
    private static final float HEIGHT_DIV_2 = (float)Settings.HEIGHT / 2.0f;
    private static final float WIDTH_DIV_2 = (float)Settings.WIDTH / 2.0f;
    private Color bgColor;
    private static final float TARGET_HEIGHT = 150.0f * Settings.scale;
    private static final float BG_RECT_EXPAND_SPEED = 3.0f;
    private float currentHeight = 0.0f;

    public EnemyTurnEffect() {
        this.duration = 2.0f;
        this.startingDuration = 2.0f;
        this.bgColor = new Color(AbstractDungeon.fadeColor.r / 2.0f, AbstractDungeon.fadeColor.g / 2.0f, AbstractDungeon.fadeColor.b / 2.0f, 0.0f);
        this.color = Settings.GOLD_COLOR.cpy();
        this.color.a = 0.0f;
        CardCrawlGame.sound.play("ENEMY_TURN");
        this.scale = 1.0f;
    }

    @Override
    public void update() {
        if (this.duration > 1.5f) {
            this.currentHeight = MathUtils.lerp(this.currentHeight, TARGET_HEIGHT, Gdx.graphics.getDeltaTime() * 3.0f);
        } else if (this.duration < 0.5f) {
            this.currentHeight = MathUtils.lerp(this.currentHeight, 0.0f, Gdx.graphics.getDeltaTime() * 3.0f);
        }
        if (this.duration > 1.5f) {
            this.scale = Interpolation.exp10In.apply(1.0f, 3.0f, (this.duration - 1.5f) * 2.0f);
            this.color.a = Interpolation.exp10In.apply(1.0f, 0.0f, (this.duration - 1.5f) * 2.0f);
        } else if (this.duration < 0.5f) {
            this.scale = Interpolation.pow3In.apply(0.9f, 1.0f, this.duration * 2.0f);
            this.color.a = Interpolation.pow3In.apply(0.0f, 1.0f, this.duration * 2.0f);
        }
        this.bgColor.a = this.color.a * 0.8f;
        if (Settings.FAST_MODE) {
            this.duration -= Gdx.graphics.getDeltaTime();
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            sb.setColor(this.bgColor);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, HEIGHT_DIV_2 - this.currentHeight / 2.0f, (float)Settings.WIDTH, this.currentHeight);
            sb.setBlendFunction(770, 1);
            FontHelper.renderFontCentered(sb, FontHelper.bannerNameFont, BattleStartEffect.ENEMY_TURN_MSG, WIDTH_DIV_2, HEIGHT_DIV_2, this.color, this.scale);
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}

