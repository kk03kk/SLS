/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashPowerEffect;
import com.megacrit.cardcrawl.vfx.combat.GainPowerEffect;
import com.megacrit.cardcrawl.vfx.combat.SilentGainPowerEffect;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractPower
implements Comparable<AbstractPower> {
    private static final Logger logger = LogManager.getLogger(AbstractPower.class.getName());
    public static TextureAtlas atlas;
    public TextureAtlas.AtlasRegion region48;
    public TextureAtlas.AtlasRegion region128;
    private static final int RAW_W = 32;
    protected static final float POWER_STACK_FONT_SCALE = 8.0f;
    private static final float FONT_LERP = 10.0f;
    private static final float FONT_SNAP_THRESHOLD = 0.05f;
    protected float fontScale = 1.0f;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color redColor = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    private Color greenColor = new Color(0.0f, 1.0f, 0.0f, 1.0f);
    private ArrayList<AbstractGameEffect> effect = new ArrayList();
    public AbstractCreature owner;
    public String name;
    public String description;
    public String ID;
    public Texture img;
    public int amount = -1;
    public int priority = 5;
    public PowerType type = PowerType.BUFF;
    protected boolean isTurnBased = false;
    public boolean isPostActionPower = false;
    public boolean canGoNegative = false;
    public static String[] DESCRIPTIONS;

    public static void initialize() {
        atlas = new TextureAtlas(Gdx.files.internal("powers/powers.atlas"));
    }

    protected void loadRegion(String fileName) {
        this.region48 = atlas.findRegion("48/" + fileName);
        this.region128 = atlas.findRegion("128/" + fileName);
    }

    public String toString() {
        return "[" + this.name + "]: " + this.description;
    }

    public void playApplyPowerSfx() {
        if (this.type == PowerType.BUFF) {
            int roll = MathUtils.random(0, 2);
            if (roll == 0) {
                CardCrawlGame.sound.play("BUFF_1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("BUFF_2");
            } else {
                CardCrawlGame.sound.play("BUFF_3");
            }
        } else {
            int roll = MathUtils.random(0, 2);
            if (roll == 0) {
                CardCrawlGame.sound.play("DEBUFF_1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("DEBUFF_2");
            } else {
                CardCrawlGame.sound.play("DEBUFF_3");
            }
        }
    }

    public void updateParticles() {
    }

    public void update(int slot) {
        this.updateFlash();
        this.updateFontScale();
        this.updateColor();
    }

    protected void addToBot(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    protected void addToTop(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToTop(action);
    }

    private void updateFlash() {
        Iterator<AbstractGameEffect> i = this.effect.iterator();
        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    private void updateColor() {
        if (this.color.a != 1.0f) {
            this.color.a = MathHelper.fadeLerpSnap(this.color.a, 1.0f);
        }
    }

    private void updateFontScale() {
        if (this.fontScale != 1.0f) {
            this.fontScale = MathUtils.lerp(this.fontScale, 1.0f, Gdx.graphics.getDeltaTime() * 10.0f);
            if (this.fontScale - 1.0f < 0.05f) {
                this.fontScale = 1.0f;
            }
        }
    }

    public void updateDescription() {
    }

    public void stackPower(int stackAmount) {
        if (this.amount == -1) {
            logger.info(this.name + " does not stack");
            return;
        }
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    public void reducePower(int reduceAmount) {
        if (this.amount - reduceAmount <= 0) {
            this.fontScale = 8.0f;
            this.amount = 0;
        } else {
            this.fontScale = 8.0f;
            this.amount -= reduceAmount;
        }
    }

    public String getHoverMessage() {
        return this.name + ":\n" + this.description;
    }

    public void renderIcons(SpriteBatch sb, float x, float y, Color c) {
        if (this.img != null) {
            sb.setColor(c);
            sb.draw(this.img, x - 12.0f, y - 12.0f, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 32, 32, false, false);
        } else {
            sb.setColor(c);
            if (Settings.isMobile) {
                sb.draw(this.region48, x - (float)this.region48.packedWidth / 2.0f, y - (float)this.region48.packedHeight / 2.0f, (float)this.region48.packedWidth / 2.0f, (float)this.region48.packedHeight / 2.0f, this.region48.packedWidth, this.region48.packedHeight, Settings.scale * 1.17f, Settings.scale * 1.17f, 0.0f);
            } else {
                sb.draw(this.region48, x - (float)this.region48.packedWidth / 2.0f, y - (float)this.region48.packedHeight / 2.0f, (float)this.region48.packedWidth / 2.0f, (float)this.region48.packedHeight / 2.0f, this.region48.packedWidth, this.region48.packedHeight, Settings.scale, Settings.scale, 0.0f);
            }
        }
        for (AbstractGameEffect e : this.effect) {
            e.render(sb, x, y);
        }
    }

    public void renderAmount(SpriteBatch sb, float x, float y, Color c) {
        if (this.amount > 0) {
            if (!this.isTurnBased) {
                this.greenColor.a = c.a;
                c = this.greenColor;
            }
            FontHelper.renderFontRightTopAligned(sb, FontHelper.powerAmountFont, Integer.toString(this.amount), x, y, this.fontScale, c);
        } else if (this.amount < 0 && this.canGoNegative) {
            this.redColor.a = c.a;
            c = this.redColor;
            FontHelper.renderFontRightTopAligned(sb, FontHelper.powerAmountFont, Integer.toString(this.amount), x, y, this.fontScale, c);
        }
    }

    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        return damage;
    }

    public float atDamageFinalGive(float damage, DamageInfo.DamageType type) {
        return damage;
    }

    public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
        return damage;
    }

    public float atDamageReceive(float damage, DamageInfo.DamageType damageType) {
        return damage;
    }

    public float atDamageGive(float damage, DamageInfo.DamageType type, AbstractCard card) {
        return this.atDamageGive(damage, type);
    }

    public float atDamageFinalGive(float damage, DamageInfo.DamageType type, AbstractCard card) {
        return this.atDamageFinalGive(damage, type);
    }

    public float atDamageFinalReceive(float damage, DamageInfo.DamageType type, AbstractCard card) {
        return this.atDamageFinalReceive(damage, type);
    }

    public float atDamageReceive(float damage, DamageInfo.DamageType damageType, AbstractCard card) {
        return this.atDamageReceive(damage, damageType);
    }

    public void atStartOfTurn() {
    }

    public void duringTurn() {
    }

    public void atStartOfTurnPostDraw() {
    }

    public void atEndOfTurn(boolean isPlayer) {
    }

    public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
    }

    public void atEndOfRound() {
    }

    public void onScry() {
    }

    public void onDamageAllEnemies(int[] damage) {
    }

    public int onHeal(int healAmount) {
        return healAmount;
    }

    public int onAttacked(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
    }

    public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public int onAttackToChangeDamage(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public void onInflictDamage(DamageInfo info, int damageAmount, AbstractCreature target) {
    }

    public void onEvokeOrb(AbstractOrb orb) {
    }

    public void onCardDraw(AbstractCard card) {
    }

    public void onPlayCard(AbstractCard card, AbstractMonster m) {
    }

    public void onUseCard(AbstractCard card, UseCardAction action) {
    }

    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    }

    public void wasHPLost(DamageInfo info, int damageAmount) {
    }

    public void onSpecificTrigger() {
    }

    public void triggerMarks(AbstractCard card) {
    }

    public void onDeath() {
    }

    public void onChannel(AbstractOrb orb) {
    }

    public void atEnergyGain() {
    }

    public void onExhaust(AbstractCard card) {
    }

    public void onChangeStance(AbstractStance oldStance, AbstractStance newStance) {
    }

    public float modifyBlock(float blockAmount) {
        return blockAmount;
    }

    public float modifyBlock(float blockAmount, AbstractCard card) {
        return this.modifyBlock(blockAmount);
    }

    public float modifyBlockLast(float blockAmount) {
        return blockAmount;
    }

    public void onGainedBlock(float blockAmount) {
    }

    public int onPlayerGainedBlock(float blockAmount) {
        return MathUtils.floor(blockAmount);
    }

    public int onPlayerGainedBlock(int blockAmount) {
        return blockAmount;
    }

    public void onGainCharge(int chargeAmount) {
    }

    public void onRemove() {
    }

    public void onEnergyRecharge() {
    }

    public void onDrawOrDiscard() {
    }

    public void onAfterCardPlayed(AbstractCard usedCard) {
    }

    public void onInitialApplication() {
    }

    @Override
    public int compareTo(AbstractPower other) {
        return this.priority - other.priority;
    }

    public void flash() {
        this.effect.add(new GainPowerEffect(this));
        AbstractDungeon.effectList.add(new FlashPowerEffect(this));
    }

    public void flashWithoutSound() {
        this.effect.add(new SilentGainPowerEffect(this));
        AbstractDungeon.effectList.add(new FlashPowerEffect(this));
    }

    public void onApplyPower(AbstractPower power, AbstractCreature target, AbstractCreature source) {
    }

    public HashMap<String, Serializable> getLocStrings() {
        HashMap<String, Serializable> powerData = new HashMap<String, Serializable>();
        powerData.put("name", (Serializable)((Object)this.name));
        powerData.put("description", (Serializable)DESCRIPTIONS);
        return powerData;
    }

    public int onLoseHp(int damageAmount) {
        return damageAmount;
    }

    public void onVictory() {
    }

    public boolean canPlayCard(AbstractCard card) {
        return true;
    }

    public static enum PowerType {
        BUFF,
        DEBUFF;

    }
}

