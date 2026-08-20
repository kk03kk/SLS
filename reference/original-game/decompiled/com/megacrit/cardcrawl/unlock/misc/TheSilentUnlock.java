/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.misc;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class TheSilentUnlock
extends AbstractUnlock {
    public static final String KEY = "The Silent";

    public TheSilentUnlock() {
        this.type = AbstractUnlock.UnlockType.CHARACTER;
        this.key = KEY;
        this.title = KEY;
    }

    @Override
    public void onUnlockScreenOpen() {
        this.player = CardCrawlGame.characterManager.getCharacter(AbstractPlayer.PlayerClass.THE_SILENT);
        this.player.drawX = (float)Settings.WIDTH / 2.0f - 20.0f * Settings.scale;
        this.player.drawY = (float)Settings.HEIGHT / 2.0f - 118.0f * Settings.scale;
    }
}

