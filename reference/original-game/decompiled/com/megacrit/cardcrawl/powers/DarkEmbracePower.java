/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DarkEmbracePower
extends AbstractPower {
    public static final String POWER_ID = "Dark Embrace";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Dark Embrace");
    public static final String NAME = DarkEmbracePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DarkEmbracePower.powerStrings.DESCRIPTIONS;

    public DarkEmbracePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("darkembrace");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }

    @Override
    public void onExhaust(AbstractCard card) {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            this.addToBot(new DrawCardAction(this.owner, this.amount));
        }
    }
}

