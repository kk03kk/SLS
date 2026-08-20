/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.utility.ShakeScreenAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.DieDieDieEffect;

public class DieDieDie
extends AbstractCard {
    public static final String ID = "Die Die Die";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Die Die Die");

    public DieDieDie() {
        super(ID, DieDieDie.cardStrings.NAME, "green/attack/die_die_die", 1, DieDieDie.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ALL_ENEMY);
        this.exhaust = true;
        this.baseDamage = 13;
        this.isMultiDamage = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(new BorderLongFlashEffect(Color.LIGHT_GRAY)));
        this.addToBot(new VFXAction(new DieDieDieEffect(), 0.7f));
        this.addToBot(new ShakeScreenAction(0.0f, ScreenShake.ShakeDur.MED, ScreenShake.ShakeIntensity.HIGH));
        this.addToBot(new DamageAllEnemiesAction((AbstractCreature)p, this.multiDamage, this.damageTypeForTurn, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(4);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DieDieDie();
    }
}

