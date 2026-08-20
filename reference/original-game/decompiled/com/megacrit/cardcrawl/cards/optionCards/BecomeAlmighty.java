/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.optionCards;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.InflameEffect;

public class BecomeAlmighty
extends AbstractCard {
    public static final String ID = "BecomeAlmighty";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("BecomeAlmighty");

    public BecomeAlmighty() {
        super(ID, BecomeAlmighty.cardStrings.NAME, "colorless/power/become_almighty", -2, BecomeAlmighty.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.onChoseThisOption();
    }

    @Override
    public void onChoseThisOption() {
        AbstractPlayer p = AbstractDungeon.player;
        this.addToBot(new VFXAction(new BorderLongFlashEffect(Color.FIREBRICK, true)));
        this.addToBot(new VFXAction(p, new InflameEffect(p), 1.0f));
        this.addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, this.magicNumber), this.magicNumber));
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
        return new BecomeAlmighty();
    }
}

