/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class HexPower
extends AbstractPower {
    public static final String POWER_ID = "Hex";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Hex");
    public static final String NAME = HexPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = HexPower.powerStrings.DESCRIPTIONS;

    public HexPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
        this.loadRegion("hex");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type != AbstractCard.CardType.ATTACK) {
            this.flash();
            this.addToBot(new MakeTempCardInDrawPileAction(new Dazed(), this.amount, true, true));
        }
    }
}

