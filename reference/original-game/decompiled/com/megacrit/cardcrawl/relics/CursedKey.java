/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class CursedKey
extends AbstractRelic {
    public static final String ID = "Cursed Key";

    public CursedKey() {
        super(ID, "cursedKey.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        if (room instanceof TreasureRoom) {
            this.flash();
            this.pulse = true;
        } else {
            this.pulse = false;
        }
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[1] + this.DESCRIPTIONS[0];
    }

    @Override
    public void onChestOpen(boolean bossChest) {
        if (!bossChest) {
            AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(AbstractDungeon.returnRandomCurse(), Settings.WIDTH / 2, Settings.HEIGHT / 2));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        }
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
    public AbstractRelic makeCopy() {
        return new CursedKey();
    }
}

