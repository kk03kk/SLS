/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class DollysMirror
extends AbstractRelic {
    public static final String ID = "DollysMirror";
    private boolean cardSelected = true;

    public DollysMirror() {
        super(ID, "mirror.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.cardSelected = false;
        if (AbstractDungeon.isScreenUp) {
            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.overlayMenu.cancelButton.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.INCOMPLETE;
        AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck, 1, this.DESCRIPTIONS[1], false, false, false, false);
    }

    @Override
    public void update() {
        super.update();
        if (!this.cardSelected && AbstractDungeon.gridSelectScreen.selectedCards.size() == 1) {
            this.cardSelected = true;
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0).makeStatEquivalentCopy();
            c.inBottleFlame = false;
            c.inBottleLightning = false;
            c.inBottleTornado = false;
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DollysMirror();
    }
}

