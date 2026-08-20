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
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;

public class AncientAugmentation
extends AbstractBlight {
    public static final String ID = "MetallicRebirth";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("MetallicRebirth");
    public static final String NAME = AncientAugmentation.blightStrings.NAME;
    public static final String[] DESC = AncientAugmentation.blightStrings.DESCRIPTION;

    public AncientAugmentation() {
        super(ID, NAME, DESC[0] + 1 + DESC[1] + 10 + DESC[2] + 10 + DESC[3], "ancient.png", false);
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
        this.description = DESC[0] + this.counter + DESC[1] + this.counter * 10 + DESC[2] + this.counter * 10 + DESC[3];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onCreateEnemy(AbstractMonster m) {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new ArtifactPower(m, this.counter), this.counter));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new PlatedArmorPower(m, this.counter * 10), this.counter * 10));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new RegenerateMonsterPower(m, this.counter * 10), this.counter * 10));
    }
}

