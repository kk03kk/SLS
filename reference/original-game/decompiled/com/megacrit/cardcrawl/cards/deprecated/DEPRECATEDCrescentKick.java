/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.watcher.CrescentKickAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDCrescentKick
extends AbstractCard {
    public static final String ID = "CrescentKick";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("CrescentKick");
    public boolean hadVigor = false;

    public DEPRECATEDCrescentKick() {
        super(ID, DEPRECATEDCrescentKick.cardStrings.NAME, "red/attack/drop_kick", 1, DEPRECATEDCrescentKick.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new CrescentKickAction(p, this));
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = AbstractDungeon.player.hasPower("Vigor") ? AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy() : AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDCrescentKick();
    }
}

