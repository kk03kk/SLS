/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.actions.watcher.NotStanceCheckAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.EmptyStanceEffect;

public class EmptyMind
extends AbstractCard {
    public static final String ID = "EmptyMind";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("EmptyMind");

    public EmptyMind() {
        super(ID, EmptyMind.cardStrings.NAME, "purple/skill/empty_mind", 1, EmptyMind.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = 2;
        this.baseMagicNumber = 2;
        this.tags.add(AbstractCard.CardTags.EMPTY);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(this.magicNumber));
        this.addToBot(new NotStanceCheckAction("Neutral", new VFXAction(new EmptyStanceEffect(p.hb.cX, p.hb.cY), 0.1f)));
        this.addToBot(new ChangeStanceAction("Neutral"));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.rawDescription = EmptyMind.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new EmptyMind();
    }
}

