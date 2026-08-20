/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.SpeechTextEffect;

public class GameSavedEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("GameSavedEffect");
    public static final String[] TEXT = GameSavedEffect.uiStrings.TEXT;

    @Override
    public void update() {
        if (ModHelper.enabledMods.size() > 0) {
            if (ModHelper.enabledMods.size() > 3) {
                AbstractDungeon.topLevelEffects.add(new SpeechTextEffect(1600.0f * Settings.scale, (float)Settings.HEIGHT - 74.0f * Settings.scale, 2.0f, TEXT[0], DialogWord.AppearEffect.FADE_IN));
            } else {
                AbstractDungeon.topLevelEffects.add(new SpeechTextEffect(1600.0f * Settings.scale, (float)Settings.HEIGHT - 26.0f * Settings.scale, 2.0f, TEXT[0], DialogWord.AppearEffect.FADE_IN));
            }
        } else {
            AbstractDungeon.topLevelEffects.add(new SpeechTextEffect(1450.0f * Settings.scale, (float)Settings.HEIGHT - 26.0f * Settings.scale, 2.0f, TEXT[0], DialogWord.AppearEffect.FADE_IN));
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

