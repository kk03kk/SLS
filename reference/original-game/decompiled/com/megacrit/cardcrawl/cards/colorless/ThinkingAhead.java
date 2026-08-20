/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.PutOnDeckAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class ThinkingAhead
extends AbstractCard {
    public static final String ID = "Thinking Ahead";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Thinking Ahead");

    public ThinkingAhead() {
        super(ID, ThinkingAhead.cardStrings.NAME, "colorless/skill/thinking_ahead", 0, ThinkingAhead.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(p, 2));
        if (AbstractDungeon.player.hand.size() > 0) {
            this.addToBot(new PutOnDeckAction(p, p, 1, false));
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new ThinkingAhead();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = ThinkingAhead.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}

