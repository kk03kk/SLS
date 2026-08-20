/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.utility.ConditionalDrawAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Impatience
extends AbstractCard {
    public static final String ID = "Impatience";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Impatience");

    public Impatience() {
        super(ID, Impatience.cardStrings.NAME, "colorless/skill/impatience", 0, Impatience.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ConditionalDrawAction(this.magicNumber, AbstractCard.CardType.ATTACK));
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = this.shouldGlow() ? AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy() : AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
    }

    private boolean shouldGlow() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c.type != AbstractCard.CardType.ATTACK) continue;
            return false;
        }
        return true;
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Impatience();
    }
}

