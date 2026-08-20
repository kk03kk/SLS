/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Defend_Blue
extends AbstractCard {
    public static final String ID = "Defend_B";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Defend_B");

    public Defend_Blue() {
        super(ID, Defend_Blue.cardStrings.NAME, "blue/skill/defend", 1, Defend_Blue.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.BASIC, AbstractCard.CardTarget.SELF);
        this.baseBlock = 5;
        this.tags.add(AbstractCard.CardTags.STARTER_DEFEND);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (Settings.isDebug) {
            this.addToBot(new GainBlockAction((AbstractCreature)p, p, 50));
        } else {
            this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Defend_Blue();
    }
}

