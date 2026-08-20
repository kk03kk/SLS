/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.HeelHookAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class HeelHook
extends AbstractCard {
    public static final String ID = "Heel Hook";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Heel Hook");

    public HeelHook() {
        super(ID, HeelHook.cardStrings.NAME, "green/attack/heel_hook", 1, HeelHook.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new HeelHookAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn)));
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDeadOrEscaped() || !m.hasPower("Weakened")) continue;
            this.glowColor = AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy();
            break;
        }
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
        return new HeelHook();
    }
}

