/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class BanditPointy
extends AbstractMonster {
    public static final String ID = "BanditChild";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("BanditChild");
    public static final String NAME = BanditPointy.monsterStrings.NAME;
    public static final String[] MOVES = BanditPointy.monsterStrings.MOVES;
    public static final String[] DIALOG = BanditPointy.monsterStrings.DIALOG;
    public static final int HP = 30;
    public static final int A_2_HP_MIN = 34;
    private static final int ATTACK_DMG = 5;
    private static final int A_2_ATTACK_DMG = 6;
    private int attackDmg;
    private static final byte POINTY_SPECIAL = 1;

    public BanditPointy(float x, float y) {
        super(NAME, ID, 30, -5.0f, -4.0f, 190.0f, 180.0f, null, x, y);
        this.dialogX = 0.0f * Settings.scale;
        this.dialogY = 50.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(34);
        } else {
            this.setHp(30);
        }
        this.attackDmg = AbstractDungeon.ascensionLevel >= 2 ? 6 : 5;
        this.damage.add(new DamageInfo(this, this.attackDmg, DamageInfo.DamageType.NORMAL));
        this.loadAnimation("images/monsters/theCity/pointy/skeleton.atlas", "images/monsters/theCity/pointy/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.stateData.setMix("Hit", "Idle", 0.2f);
        this.state.setTimeScale(1.0f);
    }

    @Override
    public void takeTurn() {
        AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "SLASH"));
        AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_VERTICAL));
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
        AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, 1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true));
    }

    @Override
    public void deathReact() {
        if (!this.isDeadOrEscaped()) {
            AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[0]));
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "SLASH": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.setTimeScale(1.0f);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
    }
}

