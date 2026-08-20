/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.SmithOption;

public class FusionHammer
extends AbstractRelic {
    public static final String ID = "Fusion Hammer";

    public FusionHammer() {
        super(ID, "burnHammer.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[1] + this.DESCRIPTIONS[0];
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.setDescription(c);
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public boolean canUseCampfireOption(AbstractCampfireOption option) {
        if (option instanceof SmithOption && option.getClass().getName().equals(SmithOption.class.getName())) {
            ((SmithOption)option).updateUsability(false);
            return false;
        }
        return true;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new FusionHammer();
    }
}

