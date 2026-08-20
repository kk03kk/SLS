/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class DiscardAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("DiscardAction");
    public static final String[] TEXT = DiscardAction.uiStrings.TEXT;
    private AbstractPlayer p;
    private boolean isRandom;
    private boolean endTurn;
    public static int numDiscarded;
    private static final float DURATION;

    public DiscardAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom) {
        this(target, source, amount, isRandom, false);
    }

    public DiscardAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom, boolean endTurn) {
        this.p = (AbstractPlayer)target;
        this.isRandom = isRandom;
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.DISCARD;
        this.endTurn = endTurn;
        this.duration = DURATION;
    }

    @Override
    public void update() {
        if (this.duration == DURATION) {
            if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() <= this.amount) {
                this.amount = this.p.hand.size();
                int tmp = this.p.hand.size();
                for (int i = 0; i < tmp; ++i) {
                    AbstractCard c = this.p.hand.getTopCard();
                    this.p.hand.moveToDiscardPile(c);
                    if (!this.endTurn) {
                        c.triggerOnManualDiscard();
                    }
                    GameActionManager.incrementDiscard(this.endTurn);
                }
                AbstractDungeon.player.hand.applyPowers();
                this.tickDuration();
                return;
            }
            if (this.isRandom) {
                for (int i = 0; i < this.amount; ++i) {
                    AbstractCard c = this.p.hand.getRandomCard(AbstractDungeon.cardRandomRng);
                    this.p.hand.moveToDiscardPile(c);
                    c.triggerOnManualDiscard();
                    GameActionManager.incrementDiscard(this.endTurn);
                }
            } else {
                if (this.amount < 0) {
                    AbstractDungeon.handCardSelectScreen.open(TEXT[0], 99, true, true);
                    AbstractDungeon.player.hand.applyPowers();
                    this.tickDuration();
                    return;
                }
                numDiscarded = this.amount;
                if (this.p.hand.size() > this.amount) {
                    AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, false);
                }
                AbstractDungeon.player.hand.applyPowers();
                this.tickDuration();
                return;
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                this.p.hand.moveToDiscardPile(c);
                c.triggerOnManualDiscard();
                GameActionManager.incrementDiscard(this.endTurn);
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }

    static {
        DURATION = Settings.ACTION_DUR_XFAST;
    }
}

