/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Regret
extends AbstractCard {
    public static final String ID = "Regret";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Regret");

    public Regret() {
        super(ID, Regret.cardStrings.NAME, "curse/regret", -2, Regret.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.CURSE, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.dontTriggerOnUseCard) {
            this.addToTop(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, this.magicNumber, AbstractGameAction.AttackEffect.FIRE));
        }
    }

    @Override
    public void triggerOnEndOfTurnForPlayingCard() {
        this.dontTriggerOnUseCard = true;
        this.magicNumber = this.baseMagicNumber = AbstractDungeon.player.hand.size();
        AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem((AbstractCard)this, true));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Regret();
    }
}

