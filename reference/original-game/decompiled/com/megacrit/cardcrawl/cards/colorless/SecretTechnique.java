/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.unique.SkillFromDeckToHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SecretTechnique
extends AbstractCard {
    public static final String ID = "Secret Technique";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Secret Technique");

    public SecretTechnique() {
        super(ID, SecretTechnique.cardStrings.NAME, "colorless/skill/secret_technique", 0, SecretTechnique.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new SkillFromDeckToHandAction(1));
    }

    @Override
    public boolean canUse(AbstractPlayer p, AbstractMonster m) {
        boolean canUse = super.canUse(p, m);
        if (!canUse) {
            return false;
        }
        boolean hasSkill = false;
        for (AbstractCard c : p.drawPile.group) {
            if (c.type != AbstractCard.CardType.SKILL) continue;
            hasSkill = true;
        }
        if (!hasSkill) {
            this.cantUseMessage = SecretTechnique.cardStrings.EXTENDED_DESCRIPTION[0];
            canUse = false;
        }
        return canUse;
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = SecretTechnique.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new SecretTechnique();
    }
}

