/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Defend_Watcher
extends AbstractCard {
    public static final String ID = "Defend_P";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Defend_P");

    public Defend_Watcher() {
        super(ID, Defend_Watcher.cardStrings.NAME, "purple/skill/defend", 1, Defend_Watcher.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.BASIC, AbstractCard.CardTarget.SELF);
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
    public AbstractCard makeCopy() {
        return new Defend_Watcher();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(3);
        }
    }
}

