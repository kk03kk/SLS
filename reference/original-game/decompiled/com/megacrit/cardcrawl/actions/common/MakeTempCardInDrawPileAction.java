/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDrawPileEffect;

public class MakeTempCardInDrawPileAction
extends AbstractGameAction {
    private AbstractCard cardToMake;
    private boolean randomSpot;
    private boolean autoPosition;
    private boolean toBottom;
    private float x;
    private float y;

    public MakeTempCardInDrawPileAction(AbstractCard card, int amount, boolean randomSpot, boolean autoPosition, boolean toBottom, float cardX, float cardY) {
        UnlockTracker.markCardAsSeen(card.cardID);
        this.setValues(this.target, this.source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.FAST_MODE ? Settings.ACTION_DUR_FAST : 0.5f;
        this.cardToMake = card;
        this.randomSpot = randomSpot;
        this.autoPosition = autoPosition;
        this.toBottom = toBottom;
        this.x = cardX;
        this.y = cardY;
    }

    public MakeTempCardInDrawPileAction(AbstractCard card, int amount, boolean randomSpot, boolean autoPosition, boolean toBottom) {
        this(card, amount, randomSpot, autoPosition, toBottom, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f);
    }

    public MakeTempCardInDrawPileAction(AbstractCard card, int amount, boolean shuffleInto, boolean autoPosition) {
        this(card, amount, shuffleInto, autoPosition, false);
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.amount < 6) {
                for (int i = 0; i < this.amount; ++i) {
                    AbstractCard c = this.cardToMake.makeStatEquivalentCopy();
                    if (c.type != AbstractCard.CardType.CURSE && c.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
                        c.upgrade();
                    }
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDrawPileEffect(c, this.x, this.y, this.randomSpot, this.autoPosition, this.toBottom));
                }
            } else {
                for (int i = 0; i < this.amount; ++i) {
                    AbstractCard c = this.cardToMake.makeStatEquivalentCopy();
                    if (c.type != AbstractCard.CardType.CURSE && c.type != AbstractCard.CardType.STATUS && AbstractDungeon.player.hasPower("MasterRealityPower")) {
                        c.upgrade();
                    }
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDrawPileEffect(c, this.randomSpot, this.toBottom));
                }
            }
            this.duration -= Gdx.graphics.getDeltaTime();
        }
        this.tickDuration();
    }
}

