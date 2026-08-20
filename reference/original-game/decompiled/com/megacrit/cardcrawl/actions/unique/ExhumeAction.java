/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.ArrayList;
import java.util.Iterator;

public class ExhumeAction
extends AbstractGameAction {
    private AbstractPlayer p;
    private final boolean upgrade;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExhumeAction");
    public static final String[] TEXT = ExhumeAction.uiStrings.TEXT;
    private ArrayList<AbstractCard> exhumes = new ArrayList();

    public ExhumeAction(boolean upgrade) {
        this.upgrade = upgrade;
        this.p = AbstractDungeon.player;
        this.setValues(this.p, AbstractDungeon.player, this.amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (AbstractDungeon.player.hand.size() == 10) {
                AbstractDungeon.player.createHandIsFullDialog();
                this.isDone = true;
                return;
            }
            if (this.p.exhaustPile.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.exhaustPile.size() == 1) {
                if (this.p.exhaustPile.group.get((int)0).cardID.equals("Exhume")) {
                    this.isDone = true;
                    return;
                }
                AbstractCard c = this.p.exhaustPile.getTopCard();
                c.unfadeOut();
                this.p.hand.addToHand(c);
                if (AbstractDungeon.player.hasPower("Corruption") && c.type == AbstractCard.CardType.SKILL) {
                    c.setCostForTurn(-9);
                }
                this.p.exhaustPile.removeCard(c);
                if (this.upgrade && c.canUpgrade()) {
                    c.upgrade();
                }
                c.unhover();
                c.fadingOut = false;
                this.isDone = true;
                return;
            }
            for (AbstractCard c : this.p.exhaustPile.group) {
                c.stopGlowing();
                c.unhover();
                c.unfadeOut();
            }
            Iterator<AbstractCard> c = this.p.exhaustPile.group.iterator();
            while (c.hasNext()) {
                AbstractCard derp = c.next();
                if (!derp.cardID.equals("Exhume")) continue;
                c.remove();
                this.exhumes.add(derp);
            }
            if (this.p.exhaustPile.isEmpty()) {
                this.p.exhaustPile.group.addAll(this.exhumes);
                this.exhumes.clear();
                this.isDone = true;
                return;
            }
            AbstractDungeon.gridSelectScreen.open(this.p.exhaustPile, 1, TEXT[0], false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                this.p.hand.addToHand(c);
                if (AbstractDungeon.player.hasPower("Corruption") && c.type == AbstractCard.CardType.SKILL) {
                    c.setCostForTurn(-9);
                }
                this.p.exhaustPile.removeCard(c);
                if (this.upgrade && c.canUpgrade()) {
                    c.upgrade();
                }
                c.unhover();
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            this.p.hand.refreshHandLayout();
            this.p.exhaustPile.group.addAll(this.exhumes);
            this.exhumes.clear();
            for (AbstractCard c : this.p.exhaustPile.group) {
                c.unhover();
                c.target_x = CardGroup.DISCARD_PILE_X;
                c.target_y = 0.0f;
            }
        }
        this.tickDuration();
    }
}

