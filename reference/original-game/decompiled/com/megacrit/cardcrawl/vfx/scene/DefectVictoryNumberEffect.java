/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DefectVictoryNumberEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float incrementTimer;
    private String num = "";
    private boolean dontIncrement = false;

    public DefectVictoryNumberEffect() {
        this.renderBehind = true;
        this.x = MathUtils.random(0.0f, 1870.0f) * Settings.xScale;
        this.y = MathUtils.random(50.0f, 990.0f) * Settings.yScale;
        this.duration = MathUtils.random(2.0f, 4.0f);
        this.color = new Color(MathUtils.random(0.5f, 1.0f), MathUtils.random(0.5f, 1.0f), MathUtils.random(0.5f, 1.0f), 0.0f);
        this.scale = MathUtils.random(0.7f, 1.3f);
        this.incrementTimer = MathUtils.random(0.02f, 0.1f);
        switch (MathUtils.random(100)) {
            case 0: {
                this.num = "H3110";
                this.dontIncrement = true;
                break;
            }
            case 1: {
                this.num = "D00T D00T";
                this.dontIncrement = true;
                break;
            }
            case 2: {
                this.num = "<ERR0R>";
                this.dontIncrement = true;
                break;
            }
        }
    }

    @Override
    public void update() {
        if (!this.dontIncrement) {
            this.incrementTimer -= Gdx.graphics.getDeltaTime();
            if (this.incrementTimer < 0.0f) {
                this.num = MathUtils.randomBoolean() ? this.num + "0" : this.num + "1";
                this.incrementTimer = MathUtils.random(0.1f, 0.4f);
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            return;
        }
        this.color.a = this.duration < 1.0f ? Interpolation.bounceOut.apply(0.0f, 0.5f, this.duration) : MathHelper.slowColorLerpSnap(this.color.a, 0.5f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        FontHelper.energyNumFontBlue.getData().setScale(this.scale);
        FontHelper.renderFont(sb, FontHelper.energyNumFontBlue, this.num, this.x, this.y, this.color);
        FontHelper.energyNumFontBlue.getData().setScale(1.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

