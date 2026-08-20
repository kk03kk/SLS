/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.AnimatedSlashEffect;

public class FlyingSleeves
extends AbstractCard {
    public static final String ID = "FlyingSleeves";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("FlyingSleeves");

    public FlyingSleeves() {
        super(ID, FlyingSleeves.cardStrings.NAME, "purple/attack/flying_sleeves", 1, FlyingSleeves.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 4;
        this.selfRetain = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new SFXAction("ATTACK_WHIFF_2", 0.3f));
            this.addToBot(new SFXAction("ATTACK_FAST", 0.2f));
            this.addToBot(new VFXAction(new AnimatedSlashEffect(m.hb.cX, m.hb.cY - 30.0f * Settings.scale, 500.0f, 200.0f, 290.0f, 3.0f, Color.VIOLET, Color.PINK)));
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
        if (m != null) {
            this.addToBot(new SFXAction("ATTACK_WHIFF_1", 0.2f));
            this.addToBot(new SFXAction("ATTACK_FAST", 0.2f));
            this.addToBot(new VFXAction(new AnimatedSlashEffect(m.hb.cX, m.hb.cY - 30.0f * Settings.scale, 500.0f, -200.0f, 250.0f, 3.0f, Color.VIOLET, Color.PINK)));
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
    }

    @Override
    public AbstractCard makeCopy() {
        return new FlyingSleeves();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
        }
    }
}

