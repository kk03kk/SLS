/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.ShowCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StasisPower;

public class ApplyStasisAction
extends AbstractGameAction {
    private AbstractCreature owner;
    private float startingDuration;
    private AbstractCard card = null;

    public ApplyStasisAction(AbstractCreature owner) {
        this.owner = owner;
        this.duration = Settings.ACTION_DUR_LONG;
        this.startingDuration = Settings.ACTION_DUR_LONG;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (AbstractDungeon.player.drawPile.isEmpty() && AbstractDungeon.player.discardPile.isEmpty()) {
            this.isDone = true;
            return;
        }
        if (this.duration == this.startingDuration) {
            if (AbstractDungeon.player.drawPile.isEmpty()) {
                this.card = AbstractDungeon.player.discardPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.RARE);
                if (this.card == null) {
                    this.card = AbstractDungeon.player.discardPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.UNCOMMON);
                    if (this.card == null) {
                        this.card = AbstractDungeon.player.discardPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.COMMON);
                        if (this.card == null) {
                            this.card = AbstractDungeon.player.discardPile.getRandomCard(AbstractDungeon.cardRandomRng);
                        }
                    }
                }
                AbstractDungeon.player.discardPile.removeCard(this.card);
            } else {
                this.card = AbstractDungeon.player.drawPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.RARE);
                if (this.card == null) {
                    this.card = AbstractDungeon.player.drawPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.UNCOMMON);
                    if (this.card == null) {
                        this.card = AbstractDungeon.player.drawPile.getRandomCard(AbstractDungeon.cardRandomRng, AbstractCard.CardRarity.COMMON);
                        if (this.card == null) {
                            this.card = AbstractDungeon.player.drawPile.getRandomCard(AbstractDungeon.cardRandomRng);
                        }
                    }
                }
                AbstractDungeon.player.drawPile.removeCard(this.card);
            }
            AbstractDungeon.player.limbo.addToBottom(this.card);
            this.card.setAngle(0.0f);
            this.card.targetDrawScale = 0.75f;
            this.card.target_x = (float)Settings.WIDTH / 2.0f;
            this.card.target_y = (float)Settings.HEIGHT / 2.0f;
            this.card.lighten(false);
            this.card.unfadeOut();
            this.card.unhover();
            this.card.untip();
            this.card.stopGlowing();
        }
        this.tickDuration();
        if (this.isDone && this.card != null) {
            this.addToTop(new ApplyPowerAction(this.owner, this.owner, new StasisPower(this.owner, this.card)));
            this.addToTop(new ShowCardAction(this.card));
        }
    }
}

