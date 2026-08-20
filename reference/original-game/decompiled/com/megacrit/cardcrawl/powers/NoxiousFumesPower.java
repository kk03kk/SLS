/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PoisonPower;

public class NoxiousFumesPower
extends AbstractPower {
    public static final String POWER_ID = "Noxious Fumes";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Noxious Fumes");
    public static final String NAME = NoxiousFumesPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = NoxiousFumesPower.powerStrings.DESCRIPTIONS;

    public NoxiousFumesPower(AbstractCreature owner, int poisonAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = poisonAmount;
        this.updateDescription();
        this.loadRegion("fumes");
    }

    @Override
    public void atStartOfTurnPostDraw() {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m.isDead || m.isDying) continue;
                this.addToBot(new ApplyPowerAction(m, this.owner, new PoisonPower(m, this.owner, this.amount), this.amount));
            }
        }
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

