/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.SlimeAnimListener;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.powers.WeakPower;

public class AcidSlime_S
extends AbstractMonster {
    public static final String ID = "AcidSlime_S";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("AcidSlime_S");
    public static final String NAME = AcidSlime_S.monsterStrings.NAME;
    public static final String[] MOVES = AcidSlime_S.monsterStrings.MOVES;
    public static final String[] DIALOG = AcidSlime_S.monsterStrings.DIALOG;
    public static final int HP_MIN = 8;
    public static final int HP_MAX = 12;
    public static final int A_2_HP_MIN = 9;
    public static final int A_2_HP_MAX = 13;
    public static final int TACKLE_DAMAGE = 3;
    public static final int WEAK_TURNS = 1;
    public static final int A_2_TACKLE_DAMAGE = 4;
    private static final byte TACKLE = 1;
    private static final byte DEBUFF = 2;

    public AcidSlime_S(float x, float y, int poisonAmount) {
        super(NAME, ID, 12, 0.0f, -4.0f, 130.0f, 100.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(9, 13);
        } else {
            this.setHp(8, 12);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 4));
        } else {
            this.damage.add(new DamageInfo(this, 3));
        }
        if (poisonAmount >= 1) {
            this.powers.add(new PoisonPower(this, this, poisonAmount));
        }
        this.loadAnimation("images/monsters/theBottom/slimeS/skeleton.atlas", "images/monsters/theBottom/slimeS/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.state.addListener(new SlimeAnimListener());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                this.setMove((byte)2, AbstractMonster.Intent.DEBUFF);
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new WeakPower(AbstractDungeon.player, 1, true), 1));
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            }
        }
    }

    @Override
    protected void getMove(int num) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            if (this.lastTwoMoves((byte)1)) {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            } else {
                this.setMove((byte)2, AbstractMonster.Intent.DEBUFF);
            }
        } else if (AbstractDungeon.aiRng.randomBoolean()) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
        } else {
            this.setMove((byte)2, AbstractMonster.Intent.DEBUFF);
        }
    }
}

