/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class BottledTornado
extends AbstractRelic {
    public static final String ID = "Bottled Tornado";
    private boolean cardSelected = true;
    public AbstractCard card = null;

    public BottledTornado() {
        super(ID, "bottledTornado.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public AbstractCard getCard() {
        return this.card.makeCopy();
    }

    @Override
    public void onEquip() {
        if (AbstractDungeon.player.masterDeck.getPurgeableCards().getPowers().size() > 0) {
            this.cardSelected = false;
            if (AbstractDungeon.isScreenUp) {
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.previousScreen = AbstractDungeon.screen;
            }
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.INCOMPLETE;
            AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck.getPurgeableCards().getPowers(), 1, this.DESCRIPTIONS[1] + this.name + LocalizedStrings.PERIOD, false, false, false, false);
        }
    }

    @Override
    public void onUnequip() {
        AbstractCard cardInDeck;
        if (this.card != null && (cardInDeck = AbstractDungeon.player.masterDeck.getSpecificCard(this.card)) != null) {
            cardInDeck.inBottleTornado = false;
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.cardSelected && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            this.cardSelected = true;
            this.card = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            this.card.inBottleTornado = true;
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            this.description = this.DESCRIPTIONS[2] + FontHelper.colorString(this.card.name, "y") + this.DESCRIPTIONS[3];
            this.tips.clear();
            this.tips.add(new PowerTip(this.name, this.description));
            this.initializeTips();
        }
    }

    public void setDescriptionAfterLoading() {
        this.description = this.DESCRIPTIONS[2] + FontHelper.colorString(this.card.name, "y") + this.DESCRIPTIONS[3];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public boolean canSpawn() {
        return CardHelper.hasCardType(AbstractCard.CardType.POWER);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BottledTornado();
    }
}

