/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

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

public class Strike_Purple
extends AbstractCard {
    public static final String ID = "Strike_P";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Strike_P");

    public Strike_Purple() {
        super(ID, Strike_Purple.cardStrings.NAME, "purple/attack/strike", 1, Strike_Purple.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.BASIC, AbstractCard.CardTarget.ENEMY);
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
                this.addToBot(new DamageAllEnemiesAction((AbstractCreature)p, this.multiDamage, this.damageTypeForTurn, AbstractGameAction.AttackEffect.BLUNT_LIGHT));
            } else {
                this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, 150, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            }
        } else {
            this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
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
        return new Strike_Purple();
    }
}

