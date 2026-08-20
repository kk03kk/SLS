/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.actions.common.MakeTempCardAtBottomOfDeckAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class ControlledChaos
extends AbstractDailyMod {
    public static final String ID = "ControlledChaos";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("ControlledChaos");
    public static final String NAME = ControlledChaos.modStrings.NAME;
    public static final String DESC = ControlledChaos.modStrings.DESCRIPTION;

    public ControlledChaos() {
        super(ID, NAME, DESC, "controlled_chaos.png", true);
    }

    public static void modAction() {
        AbstractDungeon.actionManager.addToBottom(new MakeTempCardAtBottomOfDeckAction(10));
    }
}

