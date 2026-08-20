/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BeatOfDeathPower
extends AbstractPower {
    public static final String POWER_ID = "BeatOfDeath";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("BeatOfDeath");
    public static final String NAME = BeatOfDeathPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = BeatOfDeathPower.powerStrings.DESCRIPTIONS;

    public BeatOfDeathPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
        this.loadRegion("beat");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        this.flash();
        this.addToBot(new DamageAction((AbstractCreature)AbstractDungeon.player, new DamageInfo(this.owner, this.amount, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

