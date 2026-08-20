/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.ArrayList;

public class ArmamentsAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ArmamentsAction");
    public static final String[] TEXT = ArmamentsAction.uiStrings.TEXT;
    private AbstractPlayer p;
    private ArrayList<AbstractCard> cannotUpgrade = new ArrayList();
    private boolean upgraded = false;

    public ArmamentsAction(boolean armamentsPlus) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
        this.upgraded = armamentsPlus;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.upgraded) {
                for (AbstractCard c : this.p.hand.group) {
                    if (!c.canUpgrade()) continue;
                    c.upgrade();
                    c.superFlash();
                    c.applyPowers();
                }
                this.isDone = true;
                return;
            }
            for (AbstractCard c : this.p.hand.group) {
                if (c.canUpgrade()) continue;
                this.cannotUpgrade.add(c);
            }
            if (this.cannotUpgrade.size() == this.p.hand.group.size()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.group.size() - this.cannotUpgrade.size() == 1) {
                for (AbstractCard c : this.p.hand.group) {
                    if (!c.canUpgrade()) continue;
                    c.upgrade();
                    c.superFlash();
                    c.applyPowers();
                    this.isDone = true;
                    return;
                }
            }
            this.p.hand.group.removeAll(this.cannotUpgrade);
            if (this.p.hand.group.size() > 1) {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false, false, true);
                this.tickDuration();
                return;
            }
            if (this.p.hand.group.size() == 1) {
                this.p.hand.getTopCard().upgrade();
                this.p.hand.getTopCard().superFlash();
                this.returnCards();
                this.isDone = true;
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                c.upgrade();
                c.superFlash();
                c.applyPowers();
                this.p.hand.addToTop(c);
            }
            this.returnCards();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
            AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
            this.isDone = true;
        }
        this.tickDuration();
    }

    private void returnCards() {
        for (AbstractCard c : this.cannotUpgrade) {
            this.p.hand.addToTop(c);
        }
        this.p.hand.refreshHandLayout();
    }
}

