/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GetAllInBattleInstances;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import java.util.UUID;

public class RitualDaggerAction
extends AbstractGameAction {
    private int increaseAmount;
    private DamageInfo info;
    private UUID uuid;

    public RitualDaggerAction(AbstractCreature target, DamageInfo info, int incAmount, UUID targetUUID) {
        this.info = info;
        this.setValues(target, info);
        this.increaseAmount = incAmount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.duration = 0.1f;
        this.uuid = targetUUID;
    }

    @Override
    public void update() {
        if (this.duration == 0.1f && this.target != null) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
            this.target.damage(this.info);
            if (!(!this.target.isDying && this.target.currentHealth > 0 || this.target.halfDead || this.target.hasPower("Minion"))) {
                for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                    if (!c.uuid.equals(this.uuid)) continue;
                    c.misc += this.increaseAmount;
                    c.applyPowers();
                    c.baseDamage = c.misc;
                    c.isDamageModified = false;
                }
                for (AbstractCard c : GetAllInBattleInstances.get(this.uuid)) {
                    c.misc += this.increaseAmount;
                    c.applyPowers();
                    c.baseDamage = c.misc;
                }
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
        }
        this.tickDuration();
    }
}

