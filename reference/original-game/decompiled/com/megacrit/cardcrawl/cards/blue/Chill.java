/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Frost;

public class Chill
extends AbstractCard {
    public static final String ID = "Chill";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Chill");

    public Chill() {
        super(ID, Chill.cardStrings.NAME, "blue/skill/chill", 0, Chill.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
        this.showEvokeValue = true;
        this.showEvokeOrbCount = 3;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        int count = 0;
        for (AbstractMonster mon : AbstractDungeon.getMonsters().monsters) {
            if (mon.isDeadOrEscaped()) continue;
            ++count;
        }
        for (int i = 0; i < count * this.magicNumber; ++i) {
            this.addToBot(new ChannelAction(new Frost()));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.isInnate = true;
            this.rawDescription = Chill.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Chill();
    }
}

