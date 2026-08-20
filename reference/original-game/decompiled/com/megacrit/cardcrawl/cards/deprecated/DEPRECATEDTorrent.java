/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.utility.ShakeScreenAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;

public class DEPRECATEDTorrent
extends AbstractCard {
    public static final String ID = "Torrent";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Torrent");

    public DEPRECATEDTorrent() {
        super(ID, DEPRECATEDTorrent.cardStrings.NAME, null, 1, DEPRECATEDTorrent.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ALL_ENEMY);
        this.exhaust = true;
        this.baseDamage = 1;
        this.isMultiDamage = true;
        this.baseMagicNumber = 4;
        this.magicNumber = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(new BorderLongFlashEffect(Color.CYAN)));
        this.addToBot(new ShakeScreenAction(0.0f, ScreenShake.ShakeDur.MED, ScreenShake.ShakeIntensity.HIGH));
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new DamageAllEnemiesAction(p, this.multiDamage, this.damageTypeForTurn, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL, true));
        }
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
        return new DEPRECATEDTorrent();
    }
}

