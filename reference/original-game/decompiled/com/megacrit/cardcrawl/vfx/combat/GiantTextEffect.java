/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.purple.Judgement;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class GiantTextEffect
extends AbstractGameEffect {
    private StringBuilder sBuilder = new StringBuilder("");
    private String targetString;
    private int index;
    private float x;
    private float y;

    public GiantTextEffect(float x, float y) {
        this.targetString = Judgement.cardStrings.EXTENDED_DESCRIPTION[0];
        this.index = 0;
        this.sBuilder.setLength(0);
        this.x = x;
        this.y = y + 100.0f * Settings.scale;
        this.color = Color.WHITE.cpy();
        this.duration = 1.0f;
    }

    @Override
    public void update() {
        this.color.a = Interpolation.pow5Out.apply(0.0f, 0.8f, this.duration);
        this.color.a += MathUtils.random(-0.05f, 0.05f);
        this.color.a = MathUtils.clamp(this.color.a, 0.0f, 1.0f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.index < this.targetString.length()) {
            this.sBuilder.append(this.targetString.charAt(this.index));
            ++this.index;
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.SCP_cardTitleFont_small, Judgement.cardStrings.EXTENDED_DESCRIPTION[0], this.x, this.y, this.color, 2.5f - this.duration / 4.0f + MathUtils.random(0.05f));
        sb.setBlendFunction(770, 1);
        FontHelper.renderFontCentered(sb, FontHelper.SCP_cardTitleFont_small, Judgement.cardStrings.EXTENDED_DESCRIPTION[0], this.x, this.y, this.color, 0.05f + (2.5f - this.duration / 4.0f) + MathUtils.random(0.05f));
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

