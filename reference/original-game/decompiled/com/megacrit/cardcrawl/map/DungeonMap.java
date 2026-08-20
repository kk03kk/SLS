/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.Legend;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;

public class DungeonMap {
    private static Texture top;
    private static Texture mid;
    private static Texture bot;
    private static Texture blend;
    public static Texture boss;
    public static Texture bossOutline;
    public float targetAlpha = 0.0f;
    private static final Color NOT_TAKEN_COLOR;
    private Color bossNodeColor = NOT_TAKEN_COLOR.cpy();
    private Color baseMapColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private float mapMidDist;
    private static float mapOffsetY;
    private static final float BOSS_W;
    private static final float BOSS_OFFSET_Y;
    private static final float H;
    private static final float BLEND_H;
    public Hitbox bossHb;
    public boolean atBoss = false;
    private Color reticleColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    public Legend legend = new Legend();

    public DungeonMap() {
        if (top == null) {
            top = ImageMaster.loadImage("images/ui/map/mapTop.png");
            mid = ImageMaster.loadImage("images/ui/map/mapMid.png");
            bot = ImageMaster.loadImage("images/ui/map/mapBot.png");
            blend = ImageMaster.loadImage("images/ui/map/mapBlend.png");
        }
        this.bossHb = new Hitbox(400.0f * Settings.scale, 360.0f * Settings.scale);
    }

    public void update() {
        this.legend.update(this.baseMapColor.a, AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP);
        this.baseMapColor.a = MathHelper.fadeLerpSnap(this.baseMapColor.a, this.targetAlpha);
        this.bossHb.move((float)Settings.WIDTH / 2.0f, DungeonMapScreen.offsetY + mapOffsetY + BOSS_OFFSET_Y + BOSS_W / 2.0f);
        this.bossHb.update();
        this.updateReticle();
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMPLETE && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP && (Settings.isDebug || AbstractDungeon.getCurrMapNode().y == 14 || AbstractDungeon.id.equals("TheEnding") && AbstractDungeon.getCurrMapNode().y == 2) && this.bossHb.hovered && (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed())) {
            AbstractDungeon.getCurrMapNode().taken = true;
            MapRoomNode node2 = AbstractDungeon.getCurrMapNode();
            for (MapEdge e : node2.getEdges()) {
                if (e == null) continue;
                e.markAsTaken();
            }
            InputHelper.justClickedLeft = false;
            CardCrawlGame.music.fadeOutTempBGM();
            MapRoomNode node = new MapRoomNode(-1, 15);
            node.room = new MonsterRoomBoss();
            AbstractDungeon.nextRoom = node;
            if (AbstractDungeon.pathY.size() > 1) {
                AbstractDungeon.pathX.add(AbstractDungeon.pathX.get(AbstractDungeon.pathX.size() - 1));
                AbstractDungeon.pathY.add(AbstractDungeon.pathY.get(AbstractDungeon.pathY.size() - 1) + 1);
            } else {
                AbstractDungeon.pathX.add(1);
                AbstractDungeon.pathY.add(15);
            }
            AbstractDungeon.nextRoomTransitionStart();
            this.bossHb.hovered = false;
        }
        if (this.bossHb.hovered || this.atBoss) {
            this.bossNodeColor = MapRoomNode.AVAILABLE_COLOR.cpy();
        } else {
            this.bossNodeColor.lerp(NOT_TAKEN_COLOR, Gdx.graphics.getDeltaTime() * 8.0f);
        }
        this.bossNodeColor.a = this.baseMapColor.a;
    }

    private void updateReticle() {
        if (!Settings.isControllerMode) {
            return;
        }
        if (this.bossHb.hovered) {
            this.reticleColor.a += Gdx.graphics.getDeltaTime() * 3.0f;
            if (this.reticleColor.a > 1.0f) {
                this.reticleColor.a = 1.0f;
            }
        } else {
            this.reticleColor.a = 0.0f;
        }
    }

    private float calculateMapSize() {
        if (AbstractDungeon.id.equals("TheEnding")) {
            return Settings.MAP_DST_Y * 4.0f - 1380.0f * Settings.scale;
        }
        return Settings.MAP_DST_Y * 16.0f - 1380.0f * Settings.scale;
    }

    public void show() {
        this.targetAlpha = 1.0f;
        this.mapMidDist = this.calculateMapSize();
        mapOffsetY = this.mapMidDist - 120.0f * Settings.scale;
    }

    public void hide() {
        this.targetAlpha = 0.0f;
    }

    public void hideInstantly() {
        this.targetAlpha = 0.0f;
        this.baseMapColor.a = 0.0f;
        this.legend.c.a = 0.0f;
    }

    public void render(SpriteBatch sb) {
        if (!AbstractDungeon.id.equals("TheEnding")) {
            this.renderNormalMap(sb);
        } else {
            this.renderFinalActMap(sb);
        }
    }

    private void renderNormalMap(SpriteBatch sb) {
        sb.setColor(this.baseMapColor);
        if (!Settings.isMobile) {
            sb.draw(top, 0.0f, H + DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH, 1080.0f * Settings.scale);
        } else {
            sb.draw(top, (float)(-Settings.WIDTH) * 0.05f, H + DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH * 1.1f, 1080.0f * Settings.scale);
        }
        this.renderMapCenters(sb);
        if (!Settings.isMobile) {
            sb.draw(bot, 0.0f, -this.mapMidDist + DungeonMapScreen.offsetY + mapOffsetY + 1.0f, (float)Settings.WIDTH, 1080.0f * Settings.scale);
        } else {
            sb.draw(bot, (float)(-Settings.WIDTH) * 0.05f, -this.mapMidDist + DungeonMapScreen.offsetY + mapOffsetY + 1.0f, (float)Settings.WIDTH * 1.1f, 1080.0f * Settings.scale);
        }
        this.renderMapBlender(sb);
        this.legend.render(sb);
    }

    private void renderFinalActMap(SpriteBatch sb) {
        sb.setColor(this.baseMapColor);
        if (!Settings.isMobile) {
            sb.draw(top, 0.0f, H + DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH, 1080.0f * Settings.scale);
            sb.draw(bot, 0.0f, -this.mapMidDist + DungeonMapScreen.offsetY + mapOffsetY + 1.0f, (float)Settings.WIDTH, 1080.0f * Settings.scale);
        } else {
            sb.draw(top, (float)(-Settings.WIDTH) * 0.05f, H + DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH * 1.1f, 1080.0f * Settings.scale);
            sb.draw(bot, (float)(-Settings.WIDTH) * 0.05f, -this.mapMidDist + DungeonMapScreen.offsetY + mapOffsetY + 1.0f, (float)Settings.WIDTH * 1.1f, 1080.0f * Settings.scale);
        }
        this.renderMapBlender(sb);
        this.legend.render(sb);
    }

    public void renderBossIcon(SpriteBatch sb) {
        if (boss != null) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.bossNodeColor.a));
            if (!Settings.isMobile) {
                sb.draw(bossOutline, (float)Settings.WIDTH / 2.0f - BOSS_W / 2.0f, DungeonMapScreen.offsetY + mapOffsetY + BOSS_OFFSET_Y, BOSS_W, BOSS_W);
                sb.setColor(this.bossNodeColor);
                sb.draw(boss, (float)Settings.WIDTH / 2.0f - BOSS_W / 2.0f, DungeonMapScreen.offsetY + mapOffsetY + BOSS_OFFSET_Y, BOSS_W, BOSS_W);
            } else {
                sb.draw(bossOutline, (float)Settings.WIDTH / 2.0f - BOSS_W / 2.0f, DungeonMapScreen.offsetY + mapOffsetY + BOSS_OFFSET_Y, BOSS_W, BOSS_W);
                sb.setColor(this.bossNodeColor);
                sb.draw(boss, (float)Settings.WIDTH / 2.0f - BOSS_W / 2.0f, DungeonMapScreen.offsetY + mapOffsetY + BOSS_OFFSET_Y, BOSS_W, BOSS_W);
            }
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
            this.bossHb.render(sb);
            if (Settings.isControllerMode && AbstractDungeon.dungeonMapScreen.map.bossHb.hovered) {
                this.renderReticle(sb, AbstractDungeon.dungeonMapScreen.map.bossHb);
            }
        }
    }

    private void renderMapCenters(SpriteBatch sb) {
        if (!Settings.isMobile) {
            sb.draw(mid, 0.0f, DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH, 1080.0f * Settings.scale);
        } else {
            sb.draw(mid, (float)(-Settings.WIDTH) * 0.05f, DungeonMapScreen.offsetY + mapOffsetY, (float)Settings.WIDTH * 1.1f, 1080.0f * Settings.scale);
        }
    }

    public void renderReticle(SpriteBatch sb, Hitbox hb) {
        float offset = Interpolation.fade.apply(24.0f * Settings.scale, 12.0f * Settings.scale, this.reticleColor.a);
        sb.setColor(this.reticleColor);
        this.renderReticleCorner(sb, -hb.width / 2.0f + offset, hb.height / 2.0f - offset, hb, false, false);
        this.renderReticleCorner(sb, hb.width / 2.0f - offset, hb.height / 2.0f - offset, hb, true, false);
        this.renderReticleCorner(sb, -hb.width / 2.0f + offset, -hb.height / 2.0f + offset, hb, false, true);
        this.renderReticleCorner(sb, hb.width / 2.0f - offset, -hb.height / 2.0f + offset, hb, true, true);
    }

    private void renderReticleCorner(SpriteBatch sb, float x, float y, Hitbox hb, boolean flipX, boolean flipY) {
        sb.draw(ImageMaster.RETICLE_CORNER, hb.cX + x - 18.0f, hb.cY + y - 18.0f, 18.0f, 18.0f, 36.0f, 36.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 36, 36, flipX, flipY);
    }

    private void renderMapBlender(SpriteBatch sb) {
        if (!AbstractDungeon.id.equals("TheEnding")) {
            if (!Settings.isMobile) {
                sb.draw(blend, 0.0f, DungeonMapScreen.offsetY + mapOffsetY + 800.0f * Settings.scale, (float)Settings.WIDTH, BLEND_H);
                sb.draw(blend, 0.0f, DungeonMapScreen.offsetY + mapOffsetY - 220.0f * Settings.scale, (float)Settings.WIDTH, BLEND_H);
            } else {
                sb.draw(blend, (float)(-Settings.WIDTH) * 0.05f, DungeonMapScreen.offsetY + mapOffsetY + 800.0f * Settings.scale, (float)Settings.WIDTH * 1.1f, BLEND_H);
                sb.draw(blend, (float)(-Settings.WIDTH) * 0.05f, DungeonMapScreen.offsetY + mapOffsetY - 220.0f * Settings.scale, (float)Settings.WIDTH * 1.1f, BLEND_H);
            }
        }
    }

    static {
        NOT_TAKEN_COLOR = new Color(0.34f, 0.34f, 0.34f, 1.0f);
        BOSS_W = Settings.isMobile ? 560.0f * Settings.scale : 512.0f * Settings.scale;
        BOSS_OFFSET_Y = 1416.0f * Settings.scale;
        H = 1020.0f * Settings.scale;
        BLEND_H = 512.0f * Settings.scale;
    }
}

