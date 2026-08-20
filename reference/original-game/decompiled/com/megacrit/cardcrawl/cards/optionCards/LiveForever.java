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
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.MiracleEffect;

public class LiveForever
extends AbstractCard {
    public static final String ID = "LiveForever";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("LiveForever");

    public LiveForever() {
        super(ID, LiveForever.cardStrings.NAME, "colorless/power/live_forever", -2, LiveForever.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 6;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.onChoseThisOption();
    }

    @Override
    public void onChoseThisOption() {
        AbstractPlayer p = AbstractDungeon.player;
        this.addToBot(new VFXAction(new BorderFlashEffect(Color.CHARTREUSE, true)));
        this.addToBot(new VFXAction(p, new MiracleEffect(Color.CHARTREUSE, Color.LIME, "BLOCK_GAIN_1"), 1.0f));
        this.addToBot(new ApplyPowerAction(p, p, new PlatedArmorPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new LiveForever();
    }
}

