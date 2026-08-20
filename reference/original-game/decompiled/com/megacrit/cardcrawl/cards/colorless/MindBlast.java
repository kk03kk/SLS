/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.MindblastEffect;

public class MindBlast
extends AbstractCard {
    public static final String ID = "Mind Blast";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Mind Blast");

    public MindBlast() {
        super(ID, MindBlast.cardStrings.NAME, "colorless/attack/mind_blast", 2, MindBlast.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.isInnate = true;
        this.baseDamage = 0;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new VFXAction(new MindblastEffect(p.dialogX, p.dialogY, p.flipHorizontal)));
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, DamageInfo.DamageType.NORMAL), AbstractGameAction.AttackEffect.NONE));
        this.rawDescription = MindBlast.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        this.baseDamage = AbstractDungeon.player.drawPile.size();
        super.applyPowers();
        this.rawDescription = MindBlast.cardStrings.DESCRIPTION + MindBlast.cardStrings.EXTENDED_DESCRIPTION[0];
        this.initializeDescription();
    }

    @Override
    public void calculateCardDamage(AbstractMonster mo) {
        super.calculateCardDamage(mo);
        this.rawDescription = MindBlast.cardStrings.DESCRIPTION + MindBlast.cardStrings.EXTENDED_DESCRIPTION[0];
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new MindBlast();
    }
}

