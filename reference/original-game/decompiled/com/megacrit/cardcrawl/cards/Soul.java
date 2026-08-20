/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.CardTrailEffect;
import com.megacrit.cardcrawl.vfx.combat.EmpowerEffect;
import java.util.ArrayList;

public class Soul {
    public AbstractCard card;
    public CardGroup group;
    private CatmullRomSpline<Vector2> crs = new CatmullRomSpline();
    private ArrayList<Vector2> controlPoints = new ArrayList();
    private static final int NUM_POINTS = 20;
    private Vector2[] points = new Vector2[20];
    private Vector2 pos;
    private Vector2 target;
    private static final float VFX_INTERVAL = 0.015f;
    private float backUpTimer;
    private float vfxTimer = 0.015f;
    private static final float BACK_UP_TIME = 1.5f;
    private float spawnStutterTimer = 0.0f;
    private static final float STUTTER_TIME_MAX = 0.12f;
    private boolean isInvisible = false;
    public static final Pool<CardTrailEffect> trailEffectPool = new Pool<CardTrailEffect>(){

        @Override
        protected CardTrailEffect newObject() {
            return new CardTrailEffect();
        }
    };
    private static final float DISCARD_X = (float)Settings.WIDTH * 0.96f;
    private static final float DISCARD_Y = (float)Settings.HEIGHT * 0.06f;
    private static final float DRAW_PILE_X = (float)Settings.WIDTH * 0.04f;
    private static final float DRAW_PILE_Y = (float)Settings.HEIGHT * 0.06f;
    private static final float MASTER_DECK_X = (float)Settings.WIDTH - 96.0f * Settings.scale;
    private static final float MASTER_DECK_Y = (float)Settings.HEIGHT - 32.0f * Settings.scale;
    private float currentSpeed = 0.0f;
    private static final float START_VELOCITY = 200.0f * Settings.scale;
    private static final float MAX_VELOCITY = 6000.0f * Settings.scale;
    private static final float VELOCITY_RAMP_RATE = 3000.0f * Settings.scale;
    public boolean isReadyForReuse;
    public boolean isDone;
    private static final float DST_THRESHOLD = 36.0f * Settings.scale;
    private static final float HOME_IN_THRESHOLD = 72.0f * Settings.scale;
    private float rotation;
    private boolean rotateClockwise = true;
    private boolean stopRotating = false;
    private float rotationRate;
    private static final float ROTATION_RATE = 150.0f * Settings.scale;
    private static final float ROTATION_RAMP_RATE = 800.0f;
    private Vector2 tmp = new Vector2();

    public Soul() {
        this.crs.controlPoints = new Vector2[1];
        this.isReadyForReuse = true;
    }

    public void discard(AbstractCard card, boolean visualOnly) {
        this.card = card;
        this.group = AbstractDungeon.player.discardPile;
        if (!visualOnly) {
            this.group.addToTop(card);
        }
        this.pos = new Vector2(card.current_x, card.current_y);
        this.target = new Vector2(DISCARD_X, DISCARD_Y);
        this.setSharedVariables();
        this.rotation = card.angle + 270.0f;
        this.rotateClockwise = false;
        this.currentSpeed = Settings.FAST_MODE ? START_VELOCITY * MathUtils.random(4.0f, 6.0f) : START_VELOCITY * MathUtils.random(1.0f, 4.0f);
    }

    public void discard(AbstractCard card) {
        this.discard(card, false);
    }

    public void shuffle(AbstractCard card, boolean isInvisible) {
        this.isInvisible = isInvisible;
        this.card = card;
        this.group = AbstractDungeon.player.drawPile;
        this.group.addToTop(card);
        this.pos = new Vector2(DISCARD_X, DISCARD_Y);
        this.target = new Vector2(DRAW_PILE_X, DRAW_PILE_Y);
        this.setSharedVariables();
        this.rotation = MathUtils.random(260.0f, 310.0f);
        this.currentSpeed = Settings.FAST_MODE ? START_VELOCITY * MathUtils.random(8.0f, 12.0f) : START_VELOCITY * MathUtils.random(2.0f, 5.0f);
        this.rotateClockwise = true;
        this.spawnStutterTimer = MathUtils.random(0.0f, 0.12f);
    }

    public void onToDeck(AbstractCard card, boolean randomSpot, boolean visualOnly) {
        this.card = card;
        this.group = AbstractDungeon.player.drawPile;
        if (!visualOnly) {
            if (randomSpot) {
                this.group.addToRandomSpot(card);
            } else {
                this.group.addToTop(card);
            }
        }
        this.pos = new Vector2(card.current_x, card.current_y);
        this.target = new Vector2(DRAW_PILE_X, DRAW_PILE_Y);
        this.setSharedVariables();
        this.rotation = card.angle + 270.0f;
        this.rotateClockwise = true;
    }

    public void onToDeck(AbstractCard card, boolean randomSpot) {
        this.onToDeck(card, randomSpot, false);
    }

    public void onToBottomOfDeck(AbstractCard card) {
        this.card = card;
        this.group = AbstractDungeon.player.drawPile;
        this.group.addToBottom(card);
        this.pos = new Vector2(card.current_x, card.current_y);
        this.target = new Vector2(DRAW_PILE_X, DRAW_PILE_Y);
        this.setSharedVariables();
        this.rotation = card.angle + 270.0f;
        this.rotateClockwise = true;
    }

    public void empower(AbstractCard card) {
        CardCrawlGame.sound.play("CARD_POWER_WOOSH", 0.1f);
        this.card = card;
        this.group = null;
        this.pos = new Vector2(card.current_x, card.current_y);
        this.target = new Vector2(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY);
        this.setSharedVariables();
    }

    public void obtain(AbstractCard card) {
        this.card = card;
        this.group = AbstractDungeon.player.masterDeck;
        this.group.addToTop(card);
        if (ModHelper.isModEnabled("Hoarder")) {
            this.group.addToTop(card.makeStatEquivalentCopy());
            this.group.addToTop(card.makeStatEquivalentCopy());
        }
        this.pos = new Vector2(card.current_x, card.current_y);
        this.target = new Vector2(MASTER_DECK_X, MASTER_DECK_Y);
        this.setSharedVariables();
    }

    private void setSharedVariables() {
        this.controlPoints.clear();
        if (Settings.FAST_MODE) {
            this.rotationRate = ROTATION_RATE * MathUtils.random(4.0f, 6.0f);
            this.currentSpeed = START_VELOCITY * MathUtils.random(1.0f, 1.5f);
            this.backUpTimer = 0.5f;
        } else {
            this.rotationRate = ROTATION_RATE * MathUtils.random(1.0f, 2.0f);
            this.currentSpeed = START_VELOCITY * MathUtils.random(0.2f, 1.0f);
            this.backUpTimer = 1.5f;
        }
        this.stopRotating = false;
        this.rotateClockwise = MathUtils.randomBoolean();
        this.rotation = MathUtils.random(0, 359);
        this.isReadyForReuse = false;
        this.isDone = false;
    }

    public void update() {
        if (this.isCarryingCard()) {
            this.card.update();
            this.card.targetAngle = this.rotation + 90.0f;
            this.card.current_x = this.pos.x;
            this.card.current_y = this.pos.y;
            this.card.target_x = this.card.current_x;
            this.card.target_y = this.card.current_y;
            if (this.spawnStutterTimer > 0.0f) {
                this.spawnStutterTimer -= Gdx.graphics.getDeltaTime();
                return;
            }
            this.updateMovement();
            this.updateBackUpTimer();
        } else {
            this.isDone = true;
        }
        if (this.isDone) {
            if (this.group == null) {
                AbstractDungeon.effectList.add(new EmpowerEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY));
                this.isReadyForReuse = true;
                return;
            }
            switch (this.group.type) {
                case MASTER_DECK: {
                    this.card.setAngle(0.0f);
                    this.card.targetDrawScale = 0.75f;
                    break;
                }
                case DRAW_PILE: {
                    this.card.targetDrawScale = 0.75f;
                    this.card.setAngle(0.0f);
                    this.card.lighten(false);
                    this.card.clearPowers();
                    AbstractDungeon.overlayMenu.combatDeckPanel.pop();
                    break;
                }
                case DISCARD_PILE: {
                    this.card.targetDrawScale = 0.75f;
                    this.card.setAngle(0.0f);
                    this.card.lighten(false);
                    this.card.clearPowers();
                    this.card.teleportToDiscardPile();
                    AbstractDungeon.overlayMenu.discardPilePanel.pop();
                    break;
                }
                case EXHAUST_PILE: {
                    break;
                }
            }
            if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                AbstractDungeon.player.hand.applyPowers();
            }
            this.isReadyForReuse = true;
        }
    }

    private boolean isCarryingCard() {
        if (this.group == null) {
            return true;
        }
        switch (this.group.type) {
            case DRAW_PILE: {
                return AbstractDungeon.player.drawPile.contains(this.card);
            }
            case DISCARD_PILE: {
                return AbstractDungeon.player.discardPile.contains(this.card);
            }
        }
        return true;
    }

    private void updateMovement() {
        this.tmp.x = this.pos.x - this.target.x;
        this.tmp.y = this.pos.y - this.target.y;
        this.tmp.nor();
        float targetAngle = this.tmp.angle();
        this.rotationRate += Gdx.graphics.getDeltaTime() * 800.0f;
        if (!this.stopRotating) {
            if (this.rotateClockwise) {
                this.rotation += Gdx.graphics.getDeltaTime() * this.rotationRate;
            } else {
                this.rotation -= Gdx.graphics.getDeltaTime() * this.rotationRate;
                if (this.rotation < 0.0f) {
                    this.rotation += 360.0f;
                }
            }
            this.rotation %= 360.0f;
            if (!this.stopRotating) {
                if (this.target.dst(this.pos) < HOME_IN_THRESHOLD) {
                    this.rotation = targetAngle;
                    this.stopRotating = true;
                } else if (Math.abs(this.rotation - targetAngle) < Gdx.graphics.getDeltaTime() * this.rotationRate) {
                    this.rotation = targetAngle;
                    this.stopRotating = true;
                }
            }
        }
        this.tmp.setAngle(this.rotation);
        this.tmp.x *= Gdx.graphics.getDeltaTime() * this.currentSpeed;
        this.tmp.y *= Gdx.graphics.getDeltaTime() * this.currentSpeed;
        this.pos.sub(this.tmp);
        this.currentSpeed = this.stopRotating && this.backUpTimer < 1.3499999f ? (this.currentSpeed += Gdx.graphics.getDeltaTime() * VELOCITY_RAMP_RATE * 3.0f) : (this.currentSpeed += Gdx.graphics.getDeltaTime() * VELOCITY_RAMP_RATE * 1.5f);
        if (this.currentSpeed > MAX_VELOCITY) {
            this.currentSpeed = MAX_VELOCITY;
        }
        if (this.target.x < (float)Settings.WIDTH / 2.0f && this.pos.x < 0.0f) {
            this.isDone = true;
        } else if (this.target.x > (float)Settings.WIDTH / 2.0f && this.pos.x > (float)Settings.WIDTH) {
            this.isDone = true;
        }
        if (this.target.dst(this.pos) < DST_THRESHOLD) {
            this.isDone = true;
        }
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (!this.isDone && this.vfxTimer < 0.0f) {
            this.vfxTimer = 0.015f;
            if (!this.controlPoints.isEmpty()) {
                if (!this.controlPoints.get(0).equals(this.pos)) {
                    this.controlPoints.add(this.pos.cpy());
                }
            } else {
                this.controlPoints.add(this.pos.cpy());
            }
            if (this.controlPoints.size() > 10) {
                this.controlPoints.remove(0);
            }
            if (this.controlPoints.size() > 3) {
                Vector2[] vec2Array = new Vector2[]{};
                this.crs.set(this.controlPoints.toArray(vec2Array), false);
                for (int i = 0; i < 20; ++i) {
                    if (this.points[i] == null) {
                        this.points[i] = new Vector2();
                    }
                    Vector2 derp = this.crs.valueAt(this.points[i], (float)i / 19.0f);
                    CardTrailEffect effect = trailEffectPool.obtain();
                    effect.init(derp.x, derp.y);
                    AbstractDungeon.topLevelEffects.add(effect);
                }
            }
        }
    }

    private void updateBackUpTimer() {
        this.backUpTimer -= Gdx.graphics.getDeltaTime();
        if (this.backUpTimer < 0.0f) {
            this.isDone = true;
        }
    }

    public void render(SpriteBatch sb) {
        if (!this.isInvisible) {
            this.card.renderOuterGlow(sb);
            this.card.render(sb);
        }
    }
}

