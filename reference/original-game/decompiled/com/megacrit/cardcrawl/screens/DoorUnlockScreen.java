/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DoorLock;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.DoorShineParticleEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class DoorUnlockScreen {
    public static boolean show = false;
    private static Texture doorLeft;
    private static Texture doorRight;
    private static Texture circleLeft;
    private static Texture circleRight;
    private Color bgColor = new Color(337060863);
    private Color fadeColor = Color.BLACK.cpy();
    private Color fadeOutColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private float fadeTimer;
    private float lightUpTimer;
    private boolean fadeOut = false;
    private DoorLock lockGreen;
    private DoorLock lockBlue;
    private DoorLock lockRed;
    public ArrayList<AbstractGameEffect> effects = new ArrayList();
    private boolean animateCircle = false;
    private boolean rotatingCircle = true;
    private boolean eventVersion;
    private float circleAngle = -45.0f;
    private float doorOffset = 1.0f;
    private float circleTimer;
    private float circleTime;
    private float autoContinueTimer;
    private float doorTime;
    private float renderScale = Settings.xScale > Settings.yScale ? Settings.xScale : Settings.yScale;

    public void open(boolean eventVersion) {
        GameCursor.hidden = true;
        this.eventVersion = eventVersion;
        if (doorLeft == null) {
            doorLeft = ImageMaster.loadImage("images/ui/door/door_left.png");
            doorRight = ImageMaster.loadImage("images/ui/door/door_right.png");
            circleLeft = ImageMaster.loadImage("images/ui/door/circle_left.png");
            circleRight = ImageMaster.loadImage("images/ui/door/circle_right.png");
        } else if (this.lockRed != null) {
            this.lockRed.reset();
            this.lockGreen.reset();
            this.lockBlue.reset();
        }
        this.lockRed = new DoorLock(DoorLock.LockColor.RED, CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.IRONCLAD.name() + "_WIN", false), eventVersion);
        this.lockGreen = new DoorLock(DoorLock.LockColor.GREEN, CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.THE_SILENT.name() + "_WIN", false), eventVersion);
        this.lockBlue = new DoorLock(DoorLock.LockColor.BLUE, CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.DEFECT.name() + "_WIN", false), eventVersion);
        if (Settings.FAST_MODE) {
            this.circleTimer = 1.0f;
            this.circleTime = 1.0f;
            this.lightUpTimer = 1.0f;
            this.autoContinueTimer = 0.01f;
            this.doorTime = 2.0f;
            this.fadeTimer = 1.0f;
        } else {
            this.circleTimer = 3.0f;
            this.circleTime = 3.0f;
            this.lightUpTimer = 3.0f;
            this.autoContinueTimer = 0.5f;
            this.doorTime = 5.0f;
            this.fadeTimer = 3.0f;
        }
        this.circleAngle = -45.0f;
        this.doorOffset = 1.0f;
        this.rotatingCircle = true;
        this.fadeColor = Color.BLACK.cpy();
        CardCrawlGame.music.silenceBGM();
        this.fadeOut = false;
        GameCursor.hidden = true;
    }

    public void update() {
        this.updateFade();
        this.updateLightUp();
        this.updateCircle();
        this.lockRed.update();
        this.lockGreen.update();
        this.lockBlue.update();
        this.updateFadeInput();
        this.updateVfx();
    }

    private void updateFadeInput() {
        if (this.fadeOut) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            this.fadeOutColor.a = 1.0f - this.fadeTimer;
            if (this.fadeTimer < 0.0f) {
                this.exit();
            }
            return;
        }
        if (!this.animateCircle && this.fadeTimer == 0.0f) {
            if (this.circleTimer == 0.0f) {
                if (this.autoContinueTimer > 0.0f) {
                    this.autoContinueTimer -= Gdx.graphics.getDeltaTime();
                    if (this.autoContinueTimer < 0.0f) {
                        this.exit();
                    }
                } else if (InputHelper.justClickedLeft || CInputActionSet.proceed.isJustPressed() || CInputActionSet.select.isJustPressed()) {
                    this.exit();
                }
            } else if (this.circleTimer == this.circleTime && (InputHelper.justClickedLeft || CInputActionSet.proceed.isJustPressed() || CInputActionSet.select.isJustPressed())) {
                this.fadeOut = true;
                this.fadeTimer = 1.0f;
            }
        }
    }

    private void exit() {
        this.lockRed.dispose();
        this.lockGreen.dispose();
        this.lockBlue.dispose();
        if (!this.eventVersion) {
            GameCursor.hidden = false;
            CardCrawlGame.mainMenuScreen.lighten();
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
            CardCrawlGame.music.changeBGM("MENU");
        } else {
            CardCrawlGame.mode = CardCrawlGame.GameMode.GAMEPLAY;
            CardCrawlGame.nextDungeon = "TheEnding";
            CardCrawlGame.music.fadeOutBGM();
            CardCrawlGame.music.fadeOutTempBGM();
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
            AbstractDungeon.fadeOut();
            AbstractDungeon.isDungeonBeaten = true;
        }
    }

    private void updateLightUp() {
        if (this.animateCircle && this.lightUpTimer != 0.0f) {
            this.lightUpTimer -= Gdx.graphics.getDeltaTime();
            if (Settings.FAST_MODE) {
                if (this.lightUpTimer < 1.0f) {
                    this.lockRed.flash(this.eventVersion);
                }
                if (this.lightUpTimer < 0.75f) {
                    this.lockGreen.flash(this.eventVersion);
                }
                if (this.lightUpTimer < 0.5f) {
                    this.lockBlue.flash(this.eventVersion);
                }
            } else {
                if (this.lightUpTimer < 3.0f) {
                    this.lockRed.flash(this.eventVersion);
                }
                if (this.lightUpTimer < 2.5f) {
                    this.lockGreen.flash(this.eventVersion);
                }
                if (this.lightUpTimer < 2.0f) {
                    this.lockBlue.flash(this.eventVersion);
                }
            }
            if (this.lightUpTimer < 0.0f) {
                this.lightUpTimer = 0.0f;
                this.unlock();
            }
        }
    }

    private void updateVfx() {
        Iterator<AbstractGameEffect> i = this.effects.iterator();
        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    private void updateFade() {
        if (this.fadeTimer != 0.0f) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeTimer < 0.0f) {
                this.fadeTimer = 0.0f;
                this.animateCircle = this.eventVersion;
            }
            this.fadeColor.a = Interpolation.fade.apply(0.0f, 1.0f, this.fadeTimer / 3.0f);
        }
    }

    private void unlock() {
        if (this.animateCircle) {
            CardCrawlGame.sound.playA("ATTACK_HEAVY", 0.4f);
            CardCrawlGame.sound.playA("POWER_SHACKLE", 0.1f);
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, true);
            for (int i = 0; i < 50; ++i) {
                this.effects.add(new DoorShineParticleEffect(MathUtils.random((float)Settings.WIDTH * 0.45f, (float)Settings.WIDTH * 0.55f), MathUtils.random((float)Settings.HEIGHT * 0.45f, (float)Settings.HEIGHT * 0.55f)));
            }
            this.lockRed.unlock();
            this.lockGreen.unlock();
            this.lockBlue.unlock();
        }
    }

    private void updateCircle() {
        if (this.animateCircle && this.fadeTimer == 0.0f && this.lightUpTimer == 0.0f) {
            if (this.rotatingCircle) {
                this.circleTimer -= Gdx.graphics.getDeltaTime();
                this.circleAngle = Interpolation.fade.apply(0.0f, -45.0f, this.circleTimer / this.circleTime);
                if (this.circleTimer < 0.0f) {
                    this.rotatingCircle = false;
                    this.circleTimer = this.doorTime;
                    this.circleAngle = 0.0f;
                    CardCrawlGame.screenShake.mildRumble(this.doorTime - 0.25f);
                    CardCrawlGame.sound.playA("RELIC_DROP_ROCKY", 0.3f);
                    CardCrawlGame.sound.playA("RELIC_DROP_ROCKY", -0.6f);
                    CardCrawlGame.sound.playA("EVENT_GOLDEN", -0.3f);
                    CardCrawlGame.sound.playA("EVENT_WINDING", 0.5f);
                }
            } else {
                this.circleTimer -= Gdx.graphics.getDeltaTime();
                if (this.circleTimer < 0.0f) {
                    this.circleTimer = 0.0f;
                    this.animateCircle = false;
                }
                this.bgColor.r = MathHelper.slowColorLerpSnap(this.bgColor.r, 0.0f);
                this.bgColor.g = MathHelper.slowColorLerpSnap(this.bgColor.g, 0.0f);
                this.bgColor.b = MathHelper.slowColorLerpSnap(this.bgColor.b, 0.0f);
                this.doorOffset = 1200.0f * Settings.scale * Interpolation.pow3.apply(1.0f, 0.0f, this.circleTimer / this.doorTime);
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        this.renderMainDoor(sb);
        this.renderCircleMechanism(sb);
        this.lockRed.render(sb);
        this.lockGreen.render(sb);
        this.lockBlue.render(sb);
        this.renderFade(sb);
        if (this.fadeOut) {
            sb.setColor(this.fadeOutColor);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        }
        for (AbstractGameEffect e : this.effects) {
            e.render(sb);
        }
    }

    private void renderFade(SpriteBatch sb) {
        sb.setColor(this.fadeColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    private void renderMainDoor(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        float yOffset = 0.0f;
        if (this.eventVersion) {
            yOffset = -48.0f * Settings.scale;
        }
        sb.draw(doorLeft, (float)Settings.WIDTH / 2.0f - 960.0f - this.doorOffset, (float)Settings.HEIGHT / 2.0f - 600.0f + yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.renderScale, this.renderScale, 0.0f, 0, 0, 1920, 1200, false, false);
        sb.draw(doorRight, (float)Settings.WIDTH / 2.0f - 960.0f + this.doorOffset, (float)Settings.HEIGHT / 2.0f - 600.0f + yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.renderScale, this.renderScale, 0.0f, 0, 0, 1920, 1200, false, false);
    }

    private void renderCircleMechanism(SpriteBatch sb) {
        float yOffset = 0.0f;
        if (this.eventVersion) {
            yOffset = -48.0f * Settings.scale;
        }
        sb.draw(circleRight, (float)Settings.WIDTH / 2.0f - 960.0f + this.doorOffset, (float)Settings.HEIGHT / 2.0f - 600.0f + yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.renderScale, this.renderScale, this.circleAngle, 2, 2, 1920, 1200, false, false);
        sb.draw(circleLeft, (float)Settings.WIDTH / 2.0f - 960.0f - this.doorOffset, (float)Settings.HEIGHT / 2.0f - 600.0f + yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.renderScale, this.renderScale, this.circleAngle, 2, 2, 1920, 1200, false, false);
    }
}

