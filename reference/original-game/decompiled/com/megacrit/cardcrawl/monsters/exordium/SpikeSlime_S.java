/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.SlimeAnimListener;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;

public class SpikeSlime_S
extends AbstractMonster {
    public static final String ID = "SpikeSlime_S";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("SpikeSlime_S");
    public static final String NAME = SpikeSlime_S.monsterStrings.NAME;
    public static final String[] MOVES = SpikeSlime_S.monsterStrings.MOVES;
    public static final String[] DIALOG = SpikeSlime_S.monsterStrings.DIALOG;
    public static final int HP_MIN = 10;
    public static final int HP_MAX = 14;
    public static final int A_2_HP_MIN = 11;
    public static final int A_2_HP_MAX = 15;
    public static final int TACKLE_DAMAGE = 5;
    public static final int A_2_TACKLE_DAMAGE = 6;
    private static final byte TACKLE = 1;

    public SpikeSlime_S(float x, float y, int poisonAmount) {
        super(NAME, ID, 14, 0.0f, -24.0f, 130.0f, 100.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(11, 15);
        } else {
            this.setHp(10, 14);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.damage.add(new DamageInfo(this, 6));
        } else {
            this.damage.add(new DamageInfo(this, 5));
        }
        if (poisonAmount >= 1) {
            this.powers.add(new PoisonPower(this, this, poisonAmount));
        }
        this.loadAnimation("images/monsters/theBottom/slimeAltS/skeleton.atlas", "images/monsters/theBottom/slimeAltS/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.state.addListener(new SlimeAnimListener());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
            }
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
    }
}

