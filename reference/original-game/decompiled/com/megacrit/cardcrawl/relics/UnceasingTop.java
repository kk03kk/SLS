/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class UnceasingTop
extends AbstractRelic {
    public static final String ID = "Unceasing Top";
    private boolean canDraw = false;
    private boolean disabledUntilEndOfTurn = false;

    public UnceasingTop() {
        super(ID, "top.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atPreBattle() {
        this.canDraw = false;
    }

    @Override
    public void atTurnStart() {
        this.canDraw = true;
        this.disabledUntilEndOfTurn = false;
    }

    public void disableUntilTurnEnds() {
        this.disabledUntilEndOfTurn = true;
    }

    @Override
    public void onRefreshHand() {
        if (!(!AbstractDungeon.actionManager.actions.isEmpty() || !AbstractDungeon.player.hand.isEmpty() || AbstractDungeon.actionManager.turnHasEnded || !this.canDraw || AbstractDungeon.player.hasPower("No Draw") || AbstractDungeon.isScreenUp || AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT || this.disabledUntilEndOfTurn || AbstractDungeon.player.discardPile.size() <= 0 && AbstractDungeon.player.drawPile.size() <= 0)) {
            this.flash();
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new DrawCardAction(AbstractDungeon.player, 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new UnceasingTop();
    }
}

