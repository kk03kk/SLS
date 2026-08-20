/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.actions.common.SuicideAction;
import com.megacrit.cardcrawl.actions.utility.HideHealthBarAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.BronzeOrb;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.combat.InflameEffect;
import com.megacrit.cardcrawl.vfx.combat.LaserBeamEffect;

public class BronzeAutomaton
extends AbstractMonster {
    public static final String ID = "BronzeAutomaton";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("BronzeAutomaton");
    public static final String NAME = BronzeAutomaton.monsterStrings.NAME;
    private static final String[] MOVES = BronzeAutomaton.monsterStrings.MOVES;
    private static final int HP = 300;
    private static final int A_2_HP = 320;
    private static final byte FLAIL = 1;
    private static final byte HYPER_BEAM = 2;
    private static final byte STUNNED = 3;
    private static final byte SPAWN_ORBS = 4;
    private static final byte BOOST = 5;
    private static final String BEAM_NAME = MOVES[0];
    private static final int FLAIL_DMG = 7;
    private static final int BEAM_DMG = 45;
    private static final int A_2_FLAIL_DMG = 8;
    private static final int A_2_BEAM_DMG = 50;
    private int flailDmg;
    private int beamDmg;
    private static final int BLOCK_AMT = 9;
    private static final int STR_AMT = 3;
    private static final int A_2_BLOCK_AMT = 12;
    private static final int A_2_STR_AMT = 4;
    private int strAmt;
    private int blockAmt;
    private int numTurns = 0;
    private boolean firstTurn = true;

    public BronzeAutomaton() {
        super(NAME, ID, 300, 0.0f, -30.0f, 270.0f, 400.0f, null, -50.0f, 20.0f);
        this.loadAnimation("images/monsters/theCity/automaton/skeleton.atlas", "images/monsters/theCity/automaton/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.type = AbstractMonster.EnemyType.BOSS;
        this.dialogX = -100.0f * Settings.scale;
        this.dialogY = 10.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(320);
            this.blockAmt = 12;
        } else {
            this.setHp(300);
            this.blockAmt = 9;
        }
        if (AbstractDungeon.ascensionLevel >= 4) {
            this.flailDmg = 8;
            this.beamDmg = 50;
            this.strAmt = 4;
        } else {
            this.flailDmg = 7;
            this.beamDmg = 45;
            this.strAmt = 3;
        }
        this.damage.add(new DamageInfo(this, this.flailDmg));
        this.damage.add(new DamageInfo(this, this.beamDmg));
    }

    @Override
    public void usePreBattleAction() {
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_CITY");
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ArtifactPower(this, 3)));
        UnlockTracker.markBossAsSeen("AUTOMATON");
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 4: {
                if (MathUtils.randomBoolean()) {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("AUTOMATON_ORB_SPAWN", MathUtils.random(-0.1f, 0.1f)));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_AUTOMATON_SUMMON", MathUtils.random(-0.1f, 0.1f)));
                }
                AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(new BronzeOrb(-300.0f, 200.0f, 0), true));
                if (MathUtils.randomBoolean()) {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("AUTOMATON_ORB_SPAWN", MathUtils.random(-0.1f, 0.1f)));
                } else {
                    AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_AUTOMATON_SUMMON", MathUtils.random(-0.1f, 0.1f)));
                }
                AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(new BronzeOrb(200.0f, 130.0f, 1), true));
                break;
            }
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                break;
            }
            case 5: {
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, this.blockAmt));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, this.strAmt), this.strAmt));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new LaserBeamEffect(this.hb.cX, this.hb.cY + 60.0f * Settings.scale), 1.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(1), AbstractGameAction.AttackEffect.NONE));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, TextAboveCreatureAction.TextType.STUNNED));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (this.firstTurn) {
            this.setMove((byte)4, AbstractMonster.Intent.UNKNOWN);
            this.firstTurn = false;
            return;
        }
        if (this.numTurns == 4) {
            this.setMove(BEAM_NAME, (byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)1)).base);
            this.numTurns = 0;
            return;
        }
        if (this.lastMove((byte)2)) {
            if (AbstractDungeon.ascensionLevel >= 19) {
                this.setMove((byte)5, AbstractMonster.Intent.DEFEND_BUFF);
                return;
            }
            this.setMove((byte)3, AbstractMonster.Intent.STUN);
            return;
        }
        if (this.lastMove((byte)3) || this.lastMove((byte)5) || this.lastMove((byte)4)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base, 2, true);
        } else {
            this.setMove((byte)5, AbstractMonster.Intent.DEFEND_BUFF);
        }
        ++this.numTurns;
    }

    @Override
    public void die() {
        this.useFastShakeAnimation(5.0f);
        CardCrawlGame.screenShake.rumble(4.0f);
        super.die();
        this.onBossVictoryLogic();
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m.isDead || m.isDying) continue;
            AbstractDungeon.actionManager.addToTop(new HideHealthBarAction(m));
            AbstractDungeon.actionManager.addToTop(new SuicideAction(m));
            AbstractDungeon.actionManager.addToTop(new VFXAction(m, new InflameEffect(m), 0.2f));
        }
        UnlockTracker.hardUnlockOverride("AUTOMATON");
        UnlockTracker.unlockAchievement("AUTOMATON");
    }
}

