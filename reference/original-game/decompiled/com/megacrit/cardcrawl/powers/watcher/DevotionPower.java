/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.watcher.MantraPower;

public class DevotionPower
extends AbstractPower {
    public static final String POWER_ID = "DevotionPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("DevotionPower");

    public DevotionPower(AbstractCreature owner, int newAmount) {
        this.name = DevotionPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("devotion");
    }

    @Override
    public void updateDescription() {
        this.description = DevotionPower.powerStrings.DESCRIPTIONS[0] + this.amount + DevotionPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atStartOfTurnPostDraw() {
        this.flash();
        if (!AbstractDungeon.player.hasPower("Mantra") && this.amount >= 10) {
            this.addToBot(new ChangeStanceAction("Divinity"));
        } else {
            this.addToBot(new ApplyPowerAction(this.owner, this.owner, new MantraPower(this.owner, this.amount), this.amount));
        }
    }
}

