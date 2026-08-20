/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class AccuracyPower
extends AbstractPower {
    public static final String POWER_ID = "Accuracy";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Accuracy");
    public static final String[] DESCRIPTIONS = AccuracyPower.powerStrings.DESCRIPTIONS;

    public AccuracyPower(AbstractCreature owner, int amt) {
        this.name = AccuracyPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("accuracy");
        this.updateExistingShivs();
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        this.updateExistingShivs();
    }

    private void updateExistingShivs() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!(c instanceof Shiv)) continue;
            if (!c.upgraded) {
                c.baseDamage = 4 + this.amount;
                continue;
            }
            c.baseDamage = 6 + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (!(c instanceof Shiv)) continue;
            if (!c.upgraded) {
                c.baseDamage = 4 + this.amount;
                continue;
            }
            c.baseDamage = 6 + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (!(c instanceof Shiv)) continue;
            if (!c.upgraded) {
                c.baseDamage = 4 + this.amount;
                continue;
            }
            c.baseDamage = 6 + this.amount;
        }
        for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
            if (!(c instanceof Shiv)) continue;
            if (!c.upgraded) {
                c.baseDamage = 4 + this.amount;
                continue;
            }
            c.baseDamage = 6 + this.amount;
        }
    }

    @Override
    public void onDrawOrDiscard() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!(c instanceof Shiv)) continue;
            if (!c.upgraded) {
                c.baseDamage = 4 + this.amount;
                continue;
            }
            c.baseDamage = 6 + this.amount;
        }
    }
}

