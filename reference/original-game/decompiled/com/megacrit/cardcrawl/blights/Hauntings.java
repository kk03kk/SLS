/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;

public class Hauntings
extends AbstractBlight {
    public static final String ID = "GraspOfShadows";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("GraspOfShadows");
    public static final String NAME = Hauntings.blightStrings.NAME;
    public static final String[] DESC = Hauntings.blightStrings.DESCRIPTION;

    public Hauntings() {
        super(ID, NAME, DESC[0] + 1 + DESC[1], "hauntings.png", false);
        this.counter = 1;
    }

    @Override
    public void stack() {
        ++this.counter;
        this.updateDescription();
        this.flash();
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0] + this.counter + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onCreateEnemy(AbstractMonster m) {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new IntangiblePlayerPower(m, this.counter), this.counter));
    }
}

