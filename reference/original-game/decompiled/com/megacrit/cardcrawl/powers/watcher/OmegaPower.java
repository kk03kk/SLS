/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.OmegaFlashEffect;

public class OmegaPower
extends AbstractPower {
    public static final String POWER_ID = "OmegaPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("OmegaPower");

    public OmegaPower(AbstractCreature owner, int newAmount) {
        this.name = OmegaPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("omega");
    }

    @Override
    public void updateDescription() {
        this.description = OmegaPower.powerStrings.DESCRIPTIONS[0] + this.amount + OmegaPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.flash();
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m == null || m.isDeadOrEscaped()) continue;
                if (Settings.FAST_MODE) {
                    this.addToBot(new VFXAction(new OmegaFlashEffect(m.hb.cX, m.hb.cY)));
                    continue;
                }
                this.addToBot(new VFXAction(new OmegaFlashEffect(m.hb.cX, m.hb.cY), 0.2f));
            }
            this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(this.amount, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.FIRE, true));
        }
    }
}

