/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rewards.chests;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;

public class SmallChest
extends AbstractChest {
    public SmallChest() {
        this.img = ImageMaster.S_CHEST;
        this.openedImg = ImageMaster.S_CHEST_OPEN;
        this.hb = new Hitbox(256.0f * Settings.scale, 200.0f * Settings.scale);
        this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 150.0f * Settings.scale);
        this.COMMON_CHANCE = 75;
        this.UNCOMMON_CHANCE = 25;
        this.RARE_CHANCE = 0;
        this.GOLD_CHANCE = 50;
        this.GOLD_AMT = 25;
        this.randomizeReward();
    }
}

