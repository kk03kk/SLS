/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.NightmarePower;

public class NightmareAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CopyAction");
    public static final String[] TEXT = NightmareAction.uiStrings.TEXT;
    private AbstractPlayer p;
    public static int numDiscarded;
    private static final float DURATION;

    public NightmareAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = DURATION;
        this.p = (AbstractPlayer)target;
    }

    @Override
    public void update() {
        if (this.duration == DURATION) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() == 1) {
                this.addToTop(new ApplyPowerAction(this.p, this.p, new NightmarePower(this.p, this.amount, this.p.hand.getBottomCard())));
                this.isDone = true;
                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            AbstractCard tmpCard = AbstractDungeon.handCardSelectScreen.selectedCards.getBottomCard();
            this.addToTop(new ApplyPowerAction(this.p, this.p, new NightmarePower(this.p, this.amount, tmpCard)));
            AbstractDungeon.player.hand.addToHand(tmpCard);
            AbstractDungeon.handCardSelectScreen.selectedCards.clear();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }

    static {
        DURATION = Settings.ACTION_DUR_XFAST;
    }
}

