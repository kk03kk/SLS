/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.vfx.scene.CeilingDustEffect;
import com.megacrit.cardcrawl.vfx.scene.FireFlyEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class TheCityScene
extends AbstractScene {
    private final TextureAtlas.AtlasRegion bg;
    private final TextureAtlas.AtlasRegion bgGlow;
    private final TextureAtlas.AtlasRegion bgGlow2;
    private final TextureAtlas.AtlasRegion bg2;
    private final TextureAtlas.AtlasRegion bg2Glow;
    private final TextureAtlas.AtlasRegion floor;
    private final TextureAtlas.AtlasRegion ceiling;
    private final TextureAtlas.AtlasRegion wall;
    private final TextureAtlas.AtlasRegion chains;
    private final TextureAtlas.AtlasRegion chainsGlow;
    private final TextureAtlas.AtlasRegion pillar1;
    private final TextureAtlas.AtlasRegion pillar2;
    private final TextureAtlas.AtlasRegion pillar3;
    private final TextureAtlas.AtlasRegion pillar4;
    private final TextureAtlas.AtlasRegion pillar5;
    private final TextureAtlas.AtlasRegion throne;
    private final TextureAtlas.AtlasRegion throneGlow;
    private final TextureAtlas.AtlasRegion mg;
    private final TextureAtlas.AtlasRegion mgGlow;
    private final TextureAtlas.AtlasRegion mg2;
    private final TextureAtlas.AtlasRegion fg;
    private final TextureAtlas.AtlasRegion fgGlow;
    private final TextureAtlas.AtlasRegion fg2;
    private Color overlayColor = Color.WHITE.cpy();
    private Color whiteColor = Color.WHITE.cpy();
    private Color yellowTint = new Color(1.0f, 1.0f, 0.9f, 1.0f);
    private boolean renderAltBg;
    private boolean renderMg;
    private boolean renderMgGlow;
    private boolean renderMgAlt;
    private boolean renderWall;
    private boolean renderChains;
    private boolean renderThrone;
    private boolean renderFg2;
    private boolean darkDay;
    private PillarConfig pillarConfig = PillarConfig.OPEN;
    private float ceilingDustTimer = 1.0f;
    private ArrayList<FireFlyEffect> fireFlies = new ArrayList();
    private boolean hasFlies;
    private boolean blueFlies;

    public TheCityScene() {
        super("cityScene/scene.atlas");
        this.bg = this.atlas.findRegion("mod/bg1");
        this.bgGlow = this.atlas.findRegion("mod/bgGlowv2");
        this.bgGlow2 = this.atlas.findRegion("mod/bgGlowBlur");
        this.bg2 = this.atlas.findRegion("mod/bg2");
        this.bg2Glow = this.atlas.findRegion("mod/bg2Glow");
        this.floor = this.atlas.findRegion("mod/floor");
        this.ceiling = this.atlas.findRegion("mod/ceiling");
        this.wall = this.atlas.findRegion("mod/wall");
        this.chains = this.atlas.findRegion("mod/chains");
        this.chainsGlow = this.atlas.findRegion("mod/chainsGlow");
        this.pillar1 = this.atlas.findRegion("mod/p1");
        this.pillar2 = this.atlas.findRegion("mod/p2");
        this.pillar3 = this.atlas.findRegion("mod/p3");
        this.pillar4 = this.atlas.findRegion("mod/p4");
        this.pillar5 = this.atlas.findRegion("mod/p5");
        this.throne = this.atlas.findRegion("mod/throne");
        this.throneGlow = this.atlas.findRegion("mod/throneGlow");
        this.mg = this.atlas.findRegion("mod/mg1");
        this.mgGlow = this.atlas.findRegion("mod/mg1Glow");
        this.mg2 = this.atlas.findRegion("mod/mg2");
        this.fg = this.atlas.findRegion("mod/fg");
        this.fgGlow = this.atlas.findRegion("mod/fgGlow");
        this.fg2 = this.atlas.findRegion("mod/fgHideWindow");
        this.ambianceName = "AMBIANCE_CITY";
        this.fadeInAmbiance();
    }

    @Override
    public void update() {
        super.update();
        this.updateFireFlies();
        if (!(AbstractDungeon.getCurrRoom() instanceof RestRoom) && !(AbstractDungeon.getCurrRoom() instanceof EventRoom)) {
            this.updateParticles();
        }
    }

    private void updateFireFlies() {
        Iterator<FireFlyEffect> e = this.fireFlies.iterator();
        while (e.hasNext()) {
            FireFlyEffect effect = e.next();
            effect.update();
            if (!effect.isDone) continue;
            e.remove();
        }
        if (this.fireFlies.size() < 9 && !Settings.DISABLE_EFFECTS && MathUtils.randomBoolean(0.1f)) {
            if (this.blueFlies) {
                this.fireFlies.add(new FireFlyEffect(new Color(MathUtils.random(0.1f, 0.2f), MathUtils.random(0.6f, 0.8f), MathUtils.random(0.8f, 1.0f), 1.0f)));
            } else {
                this.fireFlies.add(new FireFlyEffect(new Color(MathUtils.random(0.8f, 1.0f), MathUtils.random(0.5f, 0.8f), MathUtils.random(0.3f, 0.5f), 1.0f)));
            }
        }
    }

    private void updateParticles() {
        if (Settings.DISABLE_EFFECTS) {
            return;
        }
        this.ceilingDustTimer -= Gdx.graphics.getDeltaTime();
        if (this.ceilingDustTimer < 0.0f) {
            int roll = MathUtils.random(4);
            if (roll == 0) {
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                this.playDustSfx(false);
            } else if (roll == 1) {
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                this.playDustSfx(false);
            } else {
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                AbstractDungeon.effectsQueue.add(new CeilingDustEffect());
                if (!Settings.isBackgrounded) {
                    this.playDustSfx(true);
                }
            }
            this.ceilingDustTimer = MathUtils.random(0.5f, 60.0f);
        }
    }

    private void playDustSfx(boolean boom) {
        if (boom) {
            int roll = MathUtils.random(2);
            if (roll == 0) {
                CardCrawlGame.sound.play("CEILING_BOOM_1", 0.2f);
            } else if (roll == 1) {
                CardCrawlGame.sound.play("CEILING_BOOM_2", 0.2f);
            } else {
                CardCrawlGame.sound.play("CEILING_BOOM_3", 0.2f);
            }
        } else {
            int roll = MathUtils.random(2);
            if (roll == 0) {
                CardCrawlGame.sound.play("CEILING_DUST_1", 0.2f);
            } else if (roll == 1) {
                CardCrawlGame.sound.play("CEILING_DUST_2", 0.2f);
            } else {
                CardCrawlGame.sound.play("CEILING_DUST_3", 0.2f);
            }
        }
    }

    @Override
    public void randomizeScene() {
        int roll;
        this.hasFlies = MathUtils.randomBoolean();
        this.blueFlies = MathUtils.randomBoolean();
        this.overlayColor.r = MathUtils.random(0.8f, 0.9f);
        this.overlayColor.g = MathUtils.random(0.8f, 0.9f);
        this.overlayColor.b = MathUtils.random(0.95f, 1.0f);
        this.darkDay = MathUtils.randomBoolean(0.33f);
        if (this.darkDay) {
            this.overlayColor.r = 0.6f;
            this.overlayColor.g = MathUtils.random(0.7f, 0.8f);
            this.overlayColor.b = MathUtils.random(0.8f, 0.95f);
        }
        this.renderAltBg = MathUtils.randomBoolean();
        this.renderMg = true;
        if (this.renderMg) {
            this.renderMgAlt = MathUtils.randomBoolean();
            if (!this.renderMgAlt) {
                this.renderMgGlow = MathUtils.randomBoolean();
            }
        }
        this.renderWall = AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss ? false : MathUtils.random(4) == 4;
        this.renderChains = this.renderWall ? MathUtils.randomBoolean() : false;
        this.renderFg2 = MathUtils.randomBoolean();
        this.pillarConfig = this.renderWall ? ((roll = MathUtils.random(2)) == 0 ? PillarConfig.OPEN : (roll == 1 ? PillarConfig.LEFT_1 : PillarConfig.LEFT_2)) : ((roll = MathUtils.random(2)) == 0 ? PillarConfig.OPEN : (roll == 1 ? PillarConfig.SIDES_ONLY : PillarConfig.FULL));
        this.renderThrone = AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss && AbstractDungeon.getCurrRoom().monsters.getMonster("TheCollector") != null;
    }

    @Override
    public void nextRoom(AbstractRoom room) {
        super.nextRoom(room);
        this.fireFlies.clear();
        this.randomizeScene();
        if (room instanceof MonsterRoomBoss) {
            CardCrawlGame.music.silenceBGM();
        }
        this.fadeInAmbiance();
    }

    @Override
    public void renderCombatRoomBg(SpriteBatch sb) {
        sb.setColor(this.overlayColor);
        this.renderAtlasRegionIf(sb, this.bg, true);
        sb.setBlendFunction(770, 1);
        this.renderAtlasRegionIf(sb, this.bgGlow, true);
        if (this.darkDay) {
            sb.setColor(Color.WHITE);
            this.renderAtlasRegionIf(sb, this.bgGlow2, true);
            this.renderAtlasRegionIf(sb, this.bgGlow2, true);
        }
        sb.setBlendFunction(770, 771);
        this.renderAtlasRegionIf(sb, this.bg2, this.renderAltBg);
        sb.setBlendFunction(770, 1);
        this.renderAtlasRegionIf(sb, this.bg2Glow, this.renderAltBg);
        sb.setBlendFunction(770, 771);
        sb.setColor(this.overlayColor);
        this.renderAtlasRegionIf(sb, this.floor, true);
        this.renderAtlasRegionIf(sb, this.ceiling, true);
        this.renderAtlasRegionIf(sb, this.wall, this.renderWall);
        this.renderAtlasRegionIf(sb, this.chains, this.renderChains);
        if (this.renderChains) {
            sb.setBlendFunction(770, 1);
            this.whiteColor.a = MathUtils.cosDeg(System.currentTimeMillis() / 1L % 360L) / 10.0f + 0.9f;
            sb.setColor(this.whiteColor);
            this.renderAtlasRegionIf(sb, this.chainsGlow, true);
            this.renderAtlasRegionIf(sb, this.chainsGlow, true);
            sb.setBlendFunction(770, 771);
            sb.setColor(this.overlayColor);
        }
        this.renderAtlasRegionIf(sb, this.mg, this.renderMg);
        sb.setBlendFunction(770, 1);
        if (this.renderMgGlow) {
            this.whiteColor.a = MathUtils.cosDeg(System.currentTimeMillis() / 10L % 360L) / 2.0f + 0.5f;
            sb.setColor(this.whiteColor);
            this.renderAtlasRegionIf(sb, this.mgGlow, this.renderMg);
            this.renderAtlasRegionIf(sb, this.mgGlow, this.renderMg);
            sb.setColor(this.yellowTint);
        } else {
            this.renderAtlasRegionIf(sb, this.mgGlow, this.renderMg);
        }
        sb.setBlendFunction(770, 771);
        this.renderAtlasRegionIf(sb, this.mg2, this.renderMgAlt);
        switch (this.pillarConfig) {
            case OPEN: {
                break;
            }
            case SIDES_ONLY: {
                this.renderAtlasRegionIf(sb, this.pillar1, true);
                this.renderAtlasRegionIf(sb, this.pillar5, true);
                break;
            }
            case FULL: {
                this.renderAtlasRegionIf(sb, this.pillar1, true);
                this.renderAtlasRegionIf(sb, this.pillar2, true);
                this.renderAtlasRegionIf(sb, this.pillar3, true);
                this.renderAtlasRegionIf(sb, this.pillar4, true);
                this.renderAtlasRegionIf(sb, this.pillar5, true);
                break;
            }
            case LEFT_1: {
                this.renderAtlasRegionIf(sb, this.pillar1, true);
                break;
            }
            case LEFT_2: {
                this.renderAtlasRegionIf(sb, this.pillar1, true);
                this.renderAtlasRegionIf(sb, this.pillar2, true);
            }
        }
        this.renderAtlasRegionIf(sb, this.throne, this.renderThrone);
        sb.setBlendFunction(770, 1);
        this.renderAtlasRegionIf(sb, this.throneGlow, this.renderThrone);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void renderCombatRoomFg(SpriteBatch sb) {
        if (!this.isCamp && this.hasFlies) {
            for (FireFlyEffect e : this.fireFlies) {
                e.render(sb);
            }
        }
        sb.setColor(Color.WHITE);
        this.renderAtlasRegionIf(sb, this.fg, true);
        sb.setBlendFunction(770, 1);
        this.renderAtlasRegionIf(sb, this.fgGlow, true);
        sb.setBlendFunction(770, 771);
        this.renderAtlasRegionIf(sb, this.fg2, this.renderFg2);
    }

    @Override
    public void renderCampfireRoom(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        this.renderAtlasRegionIf(sb, this.campfireBg, true);
        sb.setBlendFunction(770, 1);
        this.whiteColor.a = MathUtils.cosDeg(System.currentTimeMillis() / 3L % 360L) / 10.0f + 0.8f;
        sb.setColor(this.whiteColor);
        this.renderQuadrupleSize(sb, this.campfireGlow, !CampfireUI.hidden);
        sb.setBlendFunction(770, 771);
        sb.setColor(Color.WHITE);
        this.renderAtlasRegionIf(sb, this.campfireKindling, true);
    }

    private static enum PillarConfig {
        OPEN,
        SIDES_ONLY,
        FULL,
        LEFT_1,
        LEFT_2;

    }
}

