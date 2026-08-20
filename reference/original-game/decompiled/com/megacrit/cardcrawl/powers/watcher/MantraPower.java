/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MantraPower
extends AbstractPower {
    public static final String POWER_ID = "Mantra";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Mantra");
    private final int PRAYER_REQUIRED = 10;

    public MantraPower(AbstractCreature owner, int amount) {
        this.name = MantraPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("mantra");
        this.type = AbstractPower.PowerType.BUFF;
        AbstractDungeon.actionManager.mantraGained += amount;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_MANTRA", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = MantraPower.powerStrings.DESCRIPTIONS[0] + 10 + MantraPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void stackPower(int stackAmount) {
        super.stackPower(stackAmount);
        if (this.amount >= 10) {
            this.addToTop(new ChangeStanceAction("Divinity"));
            this.amount -= 10;
            if (this.amount <= 0) {
                this.addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
            }
        }
    }
}

