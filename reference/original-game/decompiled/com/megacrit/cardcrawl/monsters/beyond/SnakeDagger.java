/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SnakeDagger
extends AbstractMonster {
    public static final String ID = "Dagger";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Dagger");
    public static final String NAME = SnakeDagger.monsterStrings.NAME;
    public static final String[] MOVES = SnakeDagger.monsterStrings.MOVES;
    public static final String[] DIALOG = SnakeDagger.monsterStrings.DIALOG;
    private static final int HP_MIN = 20;
    private static final int HP_MAX = 25;
    private static final int STAB_DMG = 9;
    private static final int SACRIFICE_DMG = 25;
    private static final byte WOUND = 1;
    private static final byte EXPLODE = 2;
    public boolean firstMove = true;

    public SnakeDagger(float x, float y) {
        super(NAME, ID, AbstractDungeon.monsterHpRng.random(20, 25), 0.0f, -50.0f, 140.0f, 130.0f, null, x, y);
        this.initializeAnimation();
        this.damage.add(new DamageInfo(this, 9));
        this.damage.add(new DamageInfo(this, 25));
        this.damage.add(new DamageInfo(this, 25, DamageInfo.DamageType.HP_LOSS));
    }

    public void initializeAnimation() {
        this.loadAnimation("images/monsters/theForest/mage_dagger/skeleton.atlas", "images/monsters/theForest/mage_dagger/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
                AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDiscardAction((AbstractCard)new Wound(), 1));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "SUICIDE"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new LoseHPAction(this, this, this.currentHealth));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hurt", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
            this.stateData.setMix("Hurt", "Idle", 0.1f);
            this.stateData.setMix("Idle", "Hurt", 0.1f);
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.firstMove) {
            this.firstMove = false;
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, 9);
            return;
        }
        this.setMove((byte)2, AbstractMonster.Intent.ATTACK, 25);
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
            case "SUICIDE": {
                this.state.setAnimation(0, "Attack2", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }
}

