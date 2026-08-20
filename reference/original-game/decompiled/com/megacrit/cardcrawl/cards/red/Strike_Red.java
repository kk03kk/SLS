/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Strike_Red
extends AbstractCard {
    public static final String ID = "Strike_R";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Strike_R");

    public Strike_Red() {
        super(ID, Strike_Red.cardStrings.NAME, "red/attack/strike", 1, Strike_Red.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.BASIC, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 6;
        this.tags.add(AbstractCard.CardTags.STRIKE);
        this.tags.add(AbstractCard.CardTags.STARTER_STRIKE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (Settings.isDebug) {
            if (Settings.isInfo) {
                this.multiDamage = new int[AbstractDungeon.getCurrRoom().monsters.monsters.size()];
                for (int i = 0; i < AbstractDungeon.getCurrRoom().monsters.monsters.size(); ++i) {
                    this.multiDamage[i] = 150;
                }
                this.addToBot(new DamageAllEnemiesAction((AbstractCreature)p, this.multiDamage, this.damageTypeForTurn, AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
            } else {
                this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, 150, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            }
        } else {
            this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
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
        return new Strike_Red();
    }
}

