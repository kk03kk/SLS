/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class VoidEssence
extends AbstractBlight {
    public static final String ID = "VoidEssence";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("VoidEssence");
    public static final String NAME = VoidEssence.blightStrings.NAME;
    public static final String[] DESC = VoidEssence.blightStrings.DESCRIPTION;

    public VoidEssence() {
        super(ID, NAME, DESC[0] + "1" + DESC[1], "void.png", false);
        this.counter = 1;
        this.updateDescription();
    }

    @Override
    public void stack() {
        ++this.counter;
        this.updateDescription();
        if (AbstractDungeon.player.energy.energyMaster > 0) {
            --AbstractDungeon.player.energy.energyMaster;
        }
        this.flash();
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = DESC[0] + this.counter + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void updateDescription() {
        if (AbstractDungeon.player != null) {
            this.description = DESC[0] + this.counter + DESC[1];
        }
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onEquip() {
        if (AbstractDungeon.player.energy.energyMaster > 0) {
            --AbstractDungeon.player.energy.energyMaster;
        }
    }
}

