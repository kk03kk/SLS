/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.status;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Burn
extends AbstractCard {
    public static final String ID = "Burn";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Burn");

    public Burn() {
        super(ID, Burn.cardStrings.NAME, "status/burn", -2, Burn.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
        this.magicNumber = 2;
        this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.dontTriggerOnUseCard) {
            this.addToBot(new DamageAction((AbstractCreature)AbstractDungeon.player, new DamageInfo(AbstractDungeon.player, this.magicNumber, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.FIRE));
        }
    }

    @Override
    public void triggerOnEndOfTurnForPlayingCard() {
        this.dontTriggerOnUseCard = true;
        AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem((AbstractCard)this, true));
    }

    @Override
    public AbstractCard makeCopy() {
        Burn retVal = new Burn();
        return retVal;
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
            this.rawDescription = Burn.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}

