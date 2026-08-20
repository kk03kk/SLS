/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.vfx.combat.ExplosionSmallEffect;

public class ExplosivePotion
extends AbstractPotion {
    public static final String POTION_ID = "Explosive Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Explosive Potion");

    public ExplosivePotion() {
        super(ExplosivePotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.H, AbstractPotion.PotionColor.EXPLOSIVE);
        this.isThrown = true;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = ExplosivePotion.potionStrings.DESCRIPTIONS[0] + this.potency + ExplosivePotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m.isDeadOrEscaped()) continue;
            this.addToBot(new VFXAction(new ExplosionSmallEffect(m.hb.cX, m.hb.cY), 0.1f));
        }
        this.addToBot(new WaitAction(0.5f));
        this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(this.potency, true), DamageInfo.DamageType.NORMAL, AbstractGameAction.AttackEffect.NONE));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 10;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new ExplosivePotion();
    }
}

