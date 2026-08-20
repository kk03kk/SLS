/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.actions.utility.ScryAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ForesightPower
extends AbstractPower {
    public static final String POWER_ID = "WireheadingPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("WireheadingPower");

    public ForesightPower(AbstractCreature owner, int scryAmt) {
        this.name = ForesightPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = scryAmt;
        this.updateDescription();
        this.loadRegion("wireheading");
    }

    @Override
    public void updateDescription() {
        this.description = ForesightPower.powerStrings.DESCRIPTIONS[0] + this.amount + ForesightPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atStartOfTurn() {
        if (AbstractDungeon.player.drawPile.size() <= 0) {
            this.addToTop(new EmptyDeckShuffleAction());
        }
        this.flash();
        this.addToBot(new ScryAction(this.amount));
    }
}

