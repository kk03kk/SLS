/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.ArrayList;

public class ContemplateAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ArmamentsAction");
    public static final String[] TEXT = ContemplateAction.uiStrings.TEXT;
    private boolean upgraded;
    private AbstractPlayer p;
    private ArrayList<AbstractCard> cannotUpgrade = new ArrayList();

    public ContemplateAction(boolean upgraded) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.upgraded = upgraded;
        this.p = AbstractDungeon.player;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
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
                    this.isDone = true;
                    return;
                }
            }
            if (this.upgraded) {
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
            } else {
                AbstractCard c = this.p.hand.group.get(AbstractDungeon.cardRandomRng.random(0, this.p.hand.group.size() - 1));
                c.upgrade();
                c.superFlash();
                this.isDone = true;
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                c.upgrade();
                c.superFlash();
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

