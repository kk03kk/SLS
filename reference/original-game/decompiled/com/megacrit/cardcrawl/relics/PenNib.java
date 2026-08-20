/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PenNibPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PenNib
extends AbstractRelic {
    public static final String ID = "Pen Nib";
    public static final int COUNT = 10;

    public PenNib() {
        super(ID, "penNib.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.CLINK);
        this.counter = 0;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.ATTACK) {
            ++this.counter;
            if (this.counter == 10) {
                this.counter = 0;
                this.flash();
                this.pulse = false;
            } else if (this.counter == 9) {
                this.beginPulse();
                this.pulse = true;
                AbstractDungeon.player.hand.refreshHandLayout();
                this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
                this.addToBot(new ApplyPowerAction((AbstractCreature)AbstractDungeon.player, (AbstractCreature)AbstractDungeon.player, (AbstractPower)new PenNibPower(AbstractDungeon.player, 1), 1, true));
            }
        }
    }

    @Override
    public void atBattleStart() {
        if (this.counter == 9) {
            this.beginPulse();
            this.pulse = true;
            AbstractDungeon.player.hand.refreshHandLayout();
            this.addToBot(new ApplyPowerAction((AbstractCreature)AbstractDungeon.player, (AbstractCreature)AbstractDungeon.player, (AbstractPower)new PenNibPower(AbstractDungeon.player, 1), 1, true));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PenNib();
    }
}

