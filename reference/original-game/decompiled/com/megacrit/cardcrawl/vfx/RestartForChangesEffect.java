/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class RestartForChangesEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RestartForChangesEffect");
    public static final String[] TEXT = RestartForChangesEffect.uiStrings.TEXT;
    private float x;
    private float y;
    private Color color = Settings.RED_TEXT_COLOR.cpy();

    public RestartForChangesEffect() {
        this.duration = 2.0f;
        this.color.a = 0.0f;
        this.x = (float)Settings.WIDTH / 2.0f;
        this.y = Settings.OPTION_Y + 460.0f * Settings.scale;
        this.scale = 1.3f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale = MathHelper.popLerpSnap(this.scale, 1.0f);
        if (this.duration < 0.0f) {
            this.duration = 0.0f;
            this.isDone = true;
        }
        this.color.a = this.duration < 1.0f ? this.duration : 1.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.duration / 2.0f));
        float w = FontHelper.getWidth(FontHelper.panelNameFont, TEXT[0], this.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.x - w / 2.0f - 50.0f * Settings.scale, this.y - 25.0f * Settings.scale, w + 100.0f * Settings.scale, 50.0f * Settings.scale);
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[0], this.x, this.y, this.color, this.scale);
    }

    @Override
    public void dispose() {
    }
}

