/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.TorchHeadFireEffect;

public class TorchHead
extends AbstractMonster {
    public static final String ID = "TorchHead";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("TorchHead");
    public static final String NAME = TorchHead.monsterStrings.NAME;
    public static final String[] MOVES = TorchHead.monsterStrings.MOVES;
    public static final String[] DIALOG = TorchHead.monsterStrings.DIALOG;
    public static final int HP_MIN = 38;
    public static final int HP_MAX = 40;
    public static final int A_2_HP_MIN = 40;
    public static final int A_2_HP_MAX = 45;
    public static final int ATTACK_DMG = 7;
    private static final byte TACKLE = 1;
    private float fireTimer = 0.0f;
    private static final float FIRE_TIME = 0.04f;

    public TorchHead(float x, float y) {
        super(NAME, ID, AbstractDungeon.monsterHpRng.random(38, 40), -5.0f, -20.0f, 145.0f, 240.0f, null, x, y);
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
        this.damage.add(new DamageInfo(this, 7));
        this.loadAnimation("images/monsters/theCity/torchHead/skeleton.atlas", "images/monsters/theCity/torchHead/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(40, 45);
        } else {
            this.setHp(38, 40);
        }
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 1, AbstractMonster.Intent.ATTACK, 7));
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.isDying) {
            this.fireTimer -= Gdx.graphics.getDeltaTime();
            if (this.fireTimer < 0.0f) {
                this.fireTimer = 0.04f;
                AbstractDungeon.effectList.add(new TorchHeadFireEffect(this.skeleton.getX() + this.skeleton.findBone("fireslot").getX() + 10.0f * Settings.scale, this.skeleton.getY() + this.skeleton.findBone("fireslot").getY() + 110.0f * Settings.scale));
            }
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
    }
}

