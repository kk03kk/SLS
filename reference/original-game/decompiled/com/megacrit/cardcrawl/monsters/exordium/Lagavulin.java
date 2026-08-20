/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.utility.TextAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Lagavulin
extends AbstractMonster {
    private static final Logger logger = LogManager.getLogger(Lagavulin.class.getName());
    public static final String ID = "Lagavulin";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Lagavulin");
    public static final String NAME = Lagavulin.monsterStrings.NAME;
    public static final String[] MOVES = Lagavulin.monsterStrings.MOVES;
    public static final String[] DIALOG = Lagavulin.monsterStrings.DIALOG;
    private static final int HP_MIN = 109;
    private static final int HP_MAX = 111;
    private static final int A_2_HP_MIN = 112;
    private static final int A_2_HP_MAX = 115;
    private static final byte DEBUFF = 1;
    private static final byte STRONG_ATK = 3;
    private static final byte OPEN = 4;
    private static final byte IDLE = 5;
    private static final byte OPEN_NATURAL = 6;
    private static final String DEBUFF_NAME = MOVES[0];
    private static final int STRONG_ATK_DMG = 18;
    private static final int DEBUFF_AMT = -1;
    private static final int A_18_DEBUFF_AMT = -2;
    private static final int A_2_STRONG_ATK_DMG = 20;
    private int attackDmg;
    private int debuff;
    private static final int ARMOR_AMT = 8;
    private boolean isOut = false;
    private boolean asleep;
    private boolean isOutTriggered = false;
    private int idleCount = 0;
    private int debuffTurnCount = 0;

    public Lagavulin(boolean setAsleep) {
        super(NAME, ID, 111, 0.0f, -25.0f, 320.0f, 220.0f, null, 0.0f, 20.0f);
        this.type = AbstractMonster.EnemyType.ELITE;
        this.dialogX = -100.0f * Settings.scale;
        if (AbstractDungeon.ascensionLevel >= 8) {
            this.setHp(112, 115);
        } else {
            this.setHp(109, 111);
        }
        this.attackDmg = AbstractDungeon.ascensionLevel >= 3 ? 20 : 18;
        this.debuff = AbstractDungeon.ascensionLevel >= 18 ? -2 : -1;
        this.damage.add(new DamageInfo(this, this.attackDmg));
        this.asleep = setAsleep;
        this.loadAnimation("images/monsters/theBottom/lagavulin/skeleton.atlas", "images/monsters/theBottom/lagavulin/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = null;
        if (!this.asleep) {
            this.isOut = true;
            this.isOutTriggered = true;
            e = this.state.setAnimation(0, "Idle_2", true);
            this.updateHitbox(0.0f, -25.0f, 320.0f, 370.0f);
        } else {
            e = this.state.setAnimation(0, "Idle_1", true);
        }
        this.stateData.setMix("Attack", "Idle_2", 0.25f);
        this.stateData.setMix("Hit", "Idle_2", 0.25f);
        this.stateData.setMix("Idle_1", "Idle_2", 0.5f);
        e.setTime(e.getEndTime() * MathUtils.random());
    }

    @Override
    public void usePreBattleAction() {
        if (this.asleep) {
            CardCrawlGame.music.precacheTempBgm("ELITE");
            AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)this, this, 8));
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new MetallicizePower(this, 8), 8));
        } else {
            CardCrawlGame.music.unsilenceBGM();
            AbstractDungeon.scene.fadeOutAmbiance();
            CardCrawlGame.music.playTempBgmInstantly("ELITE");
            this.setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
        }
    }

    @Override
    public void takeTurn() {
        block0 : switch (this.nextMove) {
            case 1: {
                this.debuffTurnCount = 0;
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "DEBUFF"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new DexterityPower(AbstractDungeon.player, this.debuff), this.debuff));
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player, this, new StrengthPower(AbstractDungeon.player, this.debuff), this.debuff));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 3: {
                ++this.debuffTurnCount;
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 5: {
                ++this.idleCount;
                if (this.idleCount >= 3) {
                    logger.info("idle happened");
                    this.isOutTriggered = true;
                    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OPEN"));
                    AbstractDungeon.actionManager.addToBottom(new SetMoveAction((AbstractMonster)this, 3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base));
                } else {
                    this.setMove((byte)5, AbstractMonster.Intent.SLEEP);
                }
                switch (this.idleCount) {
                    case 1: {
                        AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[1], 0.5f, 2.0f));
                        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                        break block0;
                    }
                    case 2: {
                        AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2], 0.5f, 2.0f));
                        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                        break block0;
                    }
                }
                break;
            }
            case 4: {
                AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction((AbstractCreature)this, TextAboveCreatureAction.TextType.STUNNED));
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
            case 6: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OPEN"));
                this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                this.createIntent();
                this.isOutTriggered = true;
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
                break;
            }
        }
    }

    @Override
    public void changeState(String stateName) {
        if (stateName.equals("ATTACK")) {
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle_2", true, 0.0f);
        } else if (stateName.equals("DEBUFF")) {
            this.state.setAnimation(0, "Debuff", false);
            this.state.addAnimation(0, "Idle_2", true, 0.0f);
        } else if (stateName.equals("OPEN") && !this.isDying) {
            this.isOut = true;
            this.updateHitbox(0.0f, -25.0f, 320.0f, 360.0f);
            AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[3], 0.5f, 2.0f));
            AbstractDungeon.actionManager.addToBottom(new ReducePowerAction((AbstractCreature)this, (AbstractCreature)this, "Metallicize", 8));
            CardCrawlGame.music.unsilenceBGM();
            AbstractDungeon.scene.fadeOutAmbiance();
            CardCrawlGame.music.playPrecachedTempBgm();
            this.state.setAnimation(0, "Coming_out", false);
            this.state.addAnimation(0, "Idle_2", true, 0.0f);
        }
    }

    @Override
    public void damage(DamageInfo info) {
        int previousHealth = this.currentHealth;
        super.damage(info);
        if (this.currentHealth != previousHealth && !this.isOutTriggered) {
            this.setMove((byte)4, AbstractMonster.Intent.STUN);
            this.createIntent();
            this.isOutTriggered = true;
            AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OPEN"));
        } else if (this.isOutTriggered && info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle_2", true, 0.0f);
        }
    }

    @Override
    protected void getMove(int num) {
        if (this.isOut) {
            if (this.debuffTurnCount < 2) {
                if (this.lastTwoMoves((byte)3)) {
                    this.setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
                } else {
                    this.setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
                }
            } else {
                this.setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
            }
        } else {
            this.setMove((byte)5, AbstractMonster.Intent.SLEEP);
        }
    }

    @Override
    public void die() {
        super.die();
        AbstractDungeon.scene.fadeInAmbiance();
        CardCrawlGame.music.fadeOutTempBGM();
    }
}

