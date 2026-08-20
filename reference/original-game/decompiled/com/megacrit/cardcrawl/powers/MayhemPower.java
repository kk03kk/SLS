/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.PlayTopCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MayhemPower
extends AbstractPower {
    public static final String POWER_ID = "Mayhem";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Mayhem");
    public static final String NAME = MayhemPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = MayhemPower.powerStrings.DESCRIPTIONS;

    public MayhemPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("mayhem");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }

    @Override
    public void atStartOfTurn() {
        this.flash();
        for (int i = 0; i < this.amount; ++i) {
            this.addToBot(new AbstractGameAction(){

                @Override
                public void update() {
                    this.addToBot(new PlayTopCardAction(AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true, AbstractDungeon.cardRandomRng), false));
                    this.isDone = true;
                }
            });
        }
    }
}

