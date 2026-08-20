/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class LevelTransitionTextOverlayEffect
extends AbstractGameEffect {
    private String name;
    private String levelNum;
    private static final float DUR = 5.0f;
    private float alpha = 0.0f;
    private Color c1 = Settings.GOLD_COLOR.cpy();
    private Color c2 = Settings.BLUE_TEXT_COLOR.cpy();
    private boolean higher = false;

    public LevelTransitionTextOverlayEffect(String name, String levelNum, boolean higher) {
        this.name = name;
        this.levelNum = levelNum;
        this.duration = 5.0f;
        this.startingDuration = 5.0f;
        this.color = Settings.GOLD_COLOR.cpy();
        this.color.a = 0.0f;
        this.higher = higher;
    }

    public LevelTransitionTextOverlayEffect(String name, String levelNum) {
        this.name = name;
        this.levelNum = levelNum;
        this.duration = 5.0f;
        this.startingDuration = 5.0f;
        this.color = Settings.GOLD_COLOR.cpy();
        this.color.a = 0.0f;
    }

    @Override
    public void update() {
        this.alpha = this.duration > 4.0f ? Interpolation.pow5Out.apply(1.0f, 0.0f, (this.duration - 4.0f) / 4.0f) : Interpolation.fade.apply(0.0f, 1.0f, this.duration / 2.5f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.c1.a = this.alpha;
        this.c2.a = this.alpha;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.higher) {
            FontHelper.renderFontCentered(sb, FontHelper.SCP_cardDescFont, this.levelNum, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 290.0f * Settings.scale, this.c2, 1.0f);
            FontHelper.renderFontCentered(sb, FontHelper.dungeonTitleFont, this.name, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 190.0f * Settings.scale, this.c1);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.SCP_cardDescFont, this.levelNum, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 90.0f * Settings.scale, this.c2, 1.0f);
            FontHelper.renderFontCentered(sb, FontHelper.dungeonTitleFont, this.name, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 10.0f * Settings.scale, this.c1);
        }
    }

    @Override
    public void dispose() {
    }
}

