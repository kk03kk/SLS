/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDrawPileEffect;

public class MakeTempCardInDiscardAndDeckAction
extends AbstractGameAction {
    private AbstractCard cardToMake;

    public MakeTempCardInDiscardAndDeckAction(AbstractCard card) {
        UnlockTracker.markCardAsSeen(card.cardID);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.FAST_MODE ? Settings.ACTION_DUR_FAST : 0.5f;
        this.cardToMake = card;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            AbstractCard tmp = this.cardToMake.makeStatEquivalentCopy();
            AbstractDungeon.effectList.add(new ShowCardAndAddToDrawPileEffect(tmp, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f - 10.0f * Settings.xScale, (float)Settings.HEIGHT / 2.0f, true, false));
            tmp = this.cardToMake.makeStatEquivalentCopy();
            AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(tmp));
            tmp.target_x = tmp.current_x = (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f + 10.0f * Settings.xScale;
        }
        this.tickDuration();
    }
}

