/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PrismaticShard
extends AbstractRelic {
    public static final String ID = "PrismaticShard";

    public PrismaticShard() {
        super(ID, "prism.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PrismaticShard();
    }

    @Override
    public void onEquip() {
        if (AbstractDungeon.player.chosenClass != AbstractPlayer.PlayerClass.DEFECT && AbstractDungeon.player.masterMaxOrbs == 0) {
            AbstractDungeon.player.masterMaxOrbs = 1;
        }
    }
}

