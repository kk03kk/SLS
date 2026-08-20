/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.city;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.unique.ApplyStasisAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.SmallLaserEffect;

public class BronzeOrb
extends AbstractMonster {
    public static final String ID = "BronzeOrb";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("BronzeOrb");
    public static final String[] MOVES = BronzeOrb.monsterStrings.MOVES;
    public static final String[] DIALOG = BronzeOrb.monsterStrings.DIALOG;
    private static final int HP_MIN = 52;
    private static final int HP_MAX = 58;
    private static final int A_2_HP_MIN = 54;
    private static final int A_2_HP_MAX = 60;
    private static final int BEAM_DMG = 8;
    private static final int BLOCK_AMT = 12;
    private static final byte BEAM = 1;
    private static final byte SUPPORT_BEAM = 2;
    private static final byte STASIS = 3;
    private boolean usedStasis = false;
    private int count;

    public BronzeOrb(float x, float y, int count) {
        super(BronzeOrb.monsterStrings.NAME, ID, AbstractDungeon.monsterHpRng.random(52, 58), 0.0f, 0.0f, 160.0f, 160.0f, "images/monsters/theCity/automaton/orb.png", x, y);
        if (AbstractDungeon.ascensionLevel >= 9) {
            this.setHp(54, 60);
        } else {
            this.setHp(52, 58);
        }
        this.count = count;
        this.damage.add(new DamageInfo(this, 8));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_MAGIC_BEAM_SHORT", 0.5f));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new BorderFlashEffect(Color.SKY)));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new SmallLaserEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, this.hb.cX, this.hb.cY), 0.3f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.NONE));
                break;
            }
            case 2: {
                AbstractDungeon.actionManager.addToBottom(new GainBlockAction((AbstractCreature)AbstractDungeon.getMonsters().getMonster("BronzeAutomaton"), this, 12));
                break;
            }
            case 3: {
                AbstractDungeon.actionManager.addToBottom(new ApplyStasisAction(this));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    public void update() {
        super.update();
        this.animY = this.count % 2 == 0 ? MathUtils.cosDeg(System.currentTimeMillis() / 6L % 360L) * 6.0f * Settings.scale : -MathUtils.cosDeg(System.currentTimeMillis() / 6L % 360L) * 6.0f * Settings.scale;
    }

    @Override
    protected void getMove(int num) {
        if (!this.usedStasis && num >= 25) {
            this.setMove((byte)3, AbstractMonster.Intent.STRONG_DEBUFF);
            this.usedStasis = true;
            return;
        }
        if (num >= 70 && !this.lastTwoMoves((byte)2)) {
            this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
            return;
        }
        if (!this.lastTwoMoves((byte)1)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, 8);
            return;
        }
        this.setMove((byte)2, AbstractMonster.Intent.DEFEND);
    }
}

