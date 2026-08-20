/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BetaCardArtUnlockedTextEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("BetaArtUnlockEffect");
    public static final String[] TEXT = BetaCardArtUnlockedTextEffect.uiStrings.TEXT;
    private static final float TEXT_DURATION = 3.0f;
    private static final float START_Y = (float)Settings.HEIGHT - 410.0f * Settings.scale;
    private static final float TARGET_Y = (float)Settings.HEIGHT - 310.0f * Settings.scale;
    private float y;
    private String msg;

    public BetaCardArtUnlockedTextEffect(int unlockValue) {
        CardCrawlGame.sound.play("UNLOCK_PING");
        this.duration = 3.0f;
        this.startingDuration = 3.0f;
        this.y = START_Y;
        this.color = Settings.GREEN_TEXT_COLOR.cpy();
        switch (unlockValue) {
            case 0: {
                this.msg = TEXT[0];
                break;
            }
            case 1: {
                this.msg = TEXT[1];
                break;
            }
            case 2: {
                this.msg = TEXT[2];
                break;
            }
            case 3: {
                this.msg = TEXT[3];
                break;
            }
            case 4: {
                this.msg = TEXT[4];
                break;
            }
            default: {
                this.msg = "ERROR";
            }
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
        }
        if (this.duration > 2.5f) {
            this.y = Interpolation.elasticIn.apply(TARGET_Y, START_Y, (this.duration - 2.5f) * 2.0f);
            this.color.a = Interpolation.pow2In.apply(1.0f, 0.0f, (this.duration - 2.5f) * 2.0f);
        } else if (this.duration < 0.5f) {
            this.color.a = Interpolation.pow2In.apply(0.0f, 1.0f, this.duration * 2.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.msg, (float)Settings.WIDTH / 2.0f, this.y, this.color);
    }

    @Override
    public void dispose() {
    }
}

