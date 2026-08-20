/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class CacheAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CacheAction");
    public static final String[] TEXT = CacheAction.uiStrings.TEXT;
    private AbstractPlayer p = AbstractDungeon.player;

    public CacheAction(int amount) {
        this.setValues(this.p, AbstractDungeon.player, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            if (AbstractDungeon.player.drawPile.size() <= 1) {
                this.isDone = true;
                return;
            }
            if (this.amount == 1) {
                AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.drawPile, this.amount, TEXT[0], false);
            } else {
                if (AbstractDungeon.player.drawPile.size() > this.amount) {
                    this.amount = AbstractDungeon.player.drawPile.size();
                }
                AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.drawPile, this.amount, TEXT[1], false);
            }
            this.tickDuration();
            return;
        }
        if (AbstractDungeon.gridSelectScreen.selectedCards.size() != 0) {
            for (int i = AbstractDungeon.gridSelectScreen.selectedCards.size() - 1; i > -1; --i) {
                AbstractDungeon.gridSelectScreen.selectedCards.get(i).unhover();
                this.p.drawPile.moveToDeck(AbstractDungeon.gridSelectScreen.selectedCards.get(i), false);
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
        }
        this.tickDuration();
    }
}

