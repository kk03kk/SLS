/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class ExhaustAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExhaustAction");
    public static final String[] TEXT = ExhaustAction.uiStrings.TEXT;
    private AbstractPlayer p;
    private boolean isRandom;
    private boolean anyNumber;
    private boolean canPickZero;
    public static int numExhausted;

    public ExhaustAction(int amount, boolean isRandom, boolean anyNumber, boolean canPickZero) {
        this.anyNumber = anyNumber;
        this.p = AbstractDungeon.player;
        this.canPickZero = canPickZero;
        this.isRandom = isRandom;
        this.amount = amount;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.EXHAUST;
    }

    public ExhaustAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom, boolean anyNumber) {
        this(amount, isRandom, anyNumber);
        this.target = target;
        this.source = source;
    }

    public ExhaustAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom) {
        this(amount, isRandom, false, false);
        this.target = target;
        this.source = source;
    }

    public ExhaustAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom, boolean anyNumber, boolean canPickZero) {
        this(amount, isRandom, anyNumber, canPickZero);
        this.target = target;
        this.source = source;
    }

    public ExhaustAction(boolean isRandom, boolean anyNumber, boolean canPickZero) {
        this(99, isRandom, anyNumber, canPickZero);
    }

    public ExhaustAction(int amount, boolean canPickZero) {
        this(amount, false, false, canPickZero);
    }

    public ExhaustAction(int amount, boolean isRandom, boolean anyNumber) {
        this(amount, isRandom, anyNumber, false);
    }

    public ExhaustAction(int amount, boolean isRandom, boolean anyNumber, boolean canPickZero, float duration) {
        this(amount, isRandom, anyNumber, canPickZero);
        this.duration = this.startDuration = duration;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.p.hand.size() == 0) {
                this.isDone = true;
                return;
            }
            if (!this.anyNumber && this.p.hand.size() <= this.amount) {
                numExhausted = this.amount = this.p.hand.size();
                int tmp = this.p.hand.size();
                for (int i = 0; i < tmp; ++i) {
                    AbstractCard c = this.p.hand.getTopCard();
                    this.p.hand.moveToExhaustPile(c);
                }
                CardCrawlGame.dungeon.checkForPactAchievement();
                return;
            }
            if (this.isRandom) {
                for (int i = 0; i < this.amount; ++i) {
                    this.p.hand.moveToExhaustPile(this.p.hand.getRandomCard(AbstractDungeon.cardRandomRng));
                }
                CardCrawlGame.dungeon.checkForPactAchievement();
            } else {
                numExhausted = this.amount;
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, this.anyNumber, this.canPickZero);
                this.tickDuration();
                return;
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                this.p.hand.moveToExhaustPile(c);
            }
            CardCrawlGame.dungeon.checkForPactAchievement();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

