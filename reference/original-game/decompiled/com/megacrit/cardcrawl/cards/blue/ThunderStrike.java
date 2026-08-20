/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.NewThunderStrikeAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.Lightning;

public class ThunderStrike
extends AbstractCard {
    public static final String ID = "Thunder Strike";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Thunder Strike");

    public ThunderStrike() {
        super(ID, ThunderStrike.cardStrings.NAME, "blue/attack/thunder_strike", 3, ThunderStrike.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ALL_ENEMY);
        this.baseMagicNumber = 0;
        this.magicNumber = 0;
        this.baseDamage = 7;
        this.tags.add(AbstractCard.CardTags.STRIKE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.baseMagicNumber = 0;
        for (AbstractOrb o : AbstractDungeon.actionManager.orbsChanneledThisCombat) {
            if (!(o instanceof Lightning)) continue;
            ++this.baseMagicNumber;
        }
        this.magicNumber = this.baseMagicNumber;
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new NewThunderStrikeAction(this));
        }
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        this.baseMagicNumber = 0;
        this.magicNumber = 0;
        for (AbstractOrb o : AbstractDungeon.actionManager.orbsChanneledThisCombat) {
            if (!(o instanceof Lightning)) continue;
            ++this.baseMagicNumber;
        }
        if (this.baseMagicNumber > 0) {
            this.rawDescription = ThunderStrike.cardStrings.DESCRIPTION + ThunderStrike.cardStrings.EXTENDED_DESCRIPTION[0];
            this.initializeDescription();
        }
    }

    @Override
    public void onMoveToDiscard() {
        this.rawDescription = ThunderStrike.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void calculateCardDamage(AbstractMonster mo) {
        super.calculateCardDamage(mo);
        if (this.baseMagicNumber > 0) {
            this.rawDescription = ThunderStrike.cardStrings.DESCRIPTION + ThunderStrike.cardStrings.EXTENDED_DESCRIPTION[0];
        }
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new ThunderStrike();
    }
}

