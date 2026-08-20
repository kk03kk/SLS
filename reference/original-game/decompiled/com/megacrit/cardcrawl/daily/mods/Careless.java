/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.actions.common.MillAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Careless
extends AbstractDailyMod {
    public static final String ID = "Careless";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Careless");
    public static final String NAME = Careless.modStrings.NAME;
    public static final String DESC = Careless.modStrings.DESCRIPTION;

    public Careless() {
        super(ID, NAME, DESC, "slow_start.png", false);
    }

    public static void modAction() {
        AbstractDungeon.actionManager.addToBottom(new MillAction(AbstractDungeon.player, AbstractDungeon.player, 1));
    }
}

