/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;

public class SporeCloudPower
extends AbstractPower {
    public static final String POWER_ID = "Spore Cloud";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Spore Cloud");
    public static final String NAME = SporeCloudPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = SporeCloudPower.powerStrings.DESCRIPTIONS;

    public SporeCloudPower(AbstractCreature owner, int vulnAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = vulnAmt;
        this.updateDescription();
        this.loadRegion("sporeCloud");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void onDeath() {
        if (AbstractDungeon.getCurrRoom().isBattleEnding()) {
            return;
        }
        CardCrawlGame.sound.play("SPORE_CLOUD_RELEASE");
        this.flashWithoutSound();
        this.addToTop(new ApplyPowerAction(AbstractDungeon.player, null, new VulnerablePower(AbstractDungeon.player, this.amount, true), this.amount));
    }
}

