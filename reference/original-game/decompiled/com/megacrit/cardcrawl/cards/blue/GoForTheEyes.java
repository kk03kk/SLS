/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.defect.ForTheEyesAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class GoForTheEyes
extends AbstractCard {
    public static final String ID = "Go for the Eyes";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Go for the Eyes");

    public GoForTheEyes() {
        super(ID, GoForTheEyes.cardStrings.NAME, "blue/attack/go_for_the_eyes", 0, GoForTheEyes.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 3;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
        this.addToBot(new ForTheEyesAction(this.magicNumber, m));
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDeadOrEscaped() || m.getIntentBaseDmg() < 0) continue;
            this.glowColor = AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy();
            break;
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeDamage(1);
            this.upgradeMagicNumber(1);
            this.upgradeName();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new GoForTheEyes();
    }
}

