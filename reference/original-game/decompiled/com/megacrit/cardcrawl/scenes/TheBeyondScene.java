/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.scenes.AbstractScene;

public class TheBeyondScene
extends AbstractScene {
    private final TextureAtlas.AtlasRegion bg1;
    private final TextureAtlas.AtlasRegion bg2;
    private final TextureAtlas.AtlasRegion floor;
    private final TextureAtlas.AtlasRegion ceiling;
    private final TextureAtlas.AtlasRegion fg;
    private final TextureAtlas.AtlasRegion mg1;
    private final TextureAtlas.AtlasRegion mg2;
    private final TextureAtlas.AtlasRegion mg3;
    private final TextureAtlas.AtlasRegion mg4;
    private final TextureAtlas.AtlasRegion c1;
    private final TextureAtlas.AtlasRegion c2;
    private final TextureAtlas.AtlasRegion c3;
    private final TextureAtlas.AtlasRegion c4;
    private final TextureAtlas.AtlasRegion f1;
    private final TextureAtlas.AtlasRegion f2;
    private final TextureAtlas.AtlasRegion f3;
    private final TextureAtlas.AtlasRegion f4;
    private final TextureAtlas.AtlasRegion f5;
    private final TextureAtlas.AtlasRegion i1;
    private final TextureAtlas.AtlasRegion i2;
    private final TextureAtlas.AtlasRegion i3;
    private final TextureAtlas.AtlasRegion i4;
    private final TextureAtlas.AtlasRegion i5;
    private final TextureAtlas.AtlasRegion s1;
    private final TextureAtlas.AtlasRegion s2;
    private final TextureAtlas.AtlasRegion s3;
    private final TextureAtlas.AtlasRegion s4;
    private final TextureAtlas.AtlasRegion s5;
    private boolean renderAltBg;
    private boolean renderM1;
    private boolean renderM2;
    private boolean renderM3;
    private boolean renderM4;
    private boolean renderF1;
    private boolean renderF2;
    private boolean renderF3;
    private boolean renderF4;
    private boolean renderF5;
    private boolean renderIce;
    private boolean renderI1;
    private boolean renderI2;
    private boolean renderI3;
    private boolean renderI4;
    private boolean renderI5;
    private boolean renderStalactites;
    private boolean renderS1;
    private boolean renderS2;
    private boolean renderS3;
    private boolean renderS4;
    private boolean renderS5;
    private ColumnConfig columnConfig = ColumnConfig.OPEN;
    private Color overlayColor = new Color(1.0f, 1.0f, 1.0f, 0.2f);
    private Color tmpColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private Color whiteColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    public TheBeyondScene() {
        super("beyondScene/scene.atlas");
        this.bg1 = this.atlas.findRegion("mod/bg1");
        this.bg2 = this.atlas.findRegion("mod/bg2");
        this.floor = this.atlas.findRegion("mod/floor");
        this.ceiling = this.atlas.findRegion("mod/ceiling");
        this.fg = this.atlas.findRegion("mod/fg");
        this.mg1 = this.atlas.findRegion("mod/mod1");
        this.mg2 = this.atlas.findRegion("mod/mod2");
        this.mg3 = this.atlas.findRegion("mod/mod3");
        this.mg4 = this.atlas.findRegion("mod/mod4");
        this.c1 = this.atlas.findRegion("mod/c1");
        this.c2 = this.atlas.findRegion("mod/c2");
        this.c3 = this.atlas.findRegion("mod/c3");
        this.c4 = this.atlas.findRegion("mod/c4");
        this.f1 = this.atlas.findRegion("mod/f1");
        this.f2 = this.atlas.findRegion("mod/f2");
        this.f3 = this.atlas.findRegion("mod/f3");
        this.f4 = this.atlas.findRegion("mod/f4");
        this.f5 = this.atlas.findRegion("mod/f5");
        this.i1 = this.atlas.findRegion("mod/i1");
        this.i2 = this.atlas.findRegion("mod/i2");
        this.i3 = this.atlas.findRegion("mod/i3");
        this.i4 = this.atlas.findRegion("mod/i4");
        this.i5 = this.atlas.findRegion("mod/i5");
        this.s1 = this.atlas.findRegion("mod/s1");
        this.s2 = this.atlas.findRegion("mod/s2");
        this.s3 = this.atlas.findRegion("mod/s3");
        this.s4 = this.atlas.findRegion("mod/s4");
        this.s5 = this.atlas.findRegion("mod/s5");
        this.ambianceName = "AMBIANCE_BEYOND";
        this.fadeInAmbiance();
    }

    @Override
    public void randomizeScene() {
        this.overlayColor.r = MathUtils.random(0.7f, 0.9f);
        this.overlayColor.g = MathUtils.random(0.7f, 0.9f);
        this.overlayColor.b = MathUtils.random(0.7f, 1.0f);
        this.overlayColor.a = MathUtils.random(0.0f, 0.2f);
        this.renderAltBg = MathUtils.randomBoolean(0.2f);
        this.renderM1 = false;
        this.renderM2 = false;
        this.renderM3 = false;
        this.renderM4 = false;
        if (!this.renderAltBg && MathUtils.randomBoolean(0.8f)) {
            this.renderM1 = MathUtils.randomBoolean();
            this.renderM2 = MathUtils.randomBoolean();
            this.renderM3 = MathUtils.randomBoolean();
            if (!this.renderM3) {
                this.renderM4 = MathUtils.randomBoolean();
            }
        }
        this.columnConfig = MathUtils.randomBoolean(0.6f) ? ColumnConfig.OPEN : (MathUtils.randomBoolean() ? ColumnConfig.SMALL_ONLY : (MathUtils.randomBoolean() ? ColumnConfig.SMALL_PLUS_LEFT : ColumnConfig.SMALL_PLUS_RIGHT));
        this.renderF1 = false;
        this.renderF2 = false;
        this.renderF3 = false;
        this.renderF4 = false;
        this.renderF5 = false;
        int floaterCount = 0;
        this.renderF1 = MathUtils.randomBoolean(0.25f);
        if (this.renderF1) {
            ++floaterCount;
        }
        this.renderF2 = MathUtils.randomBoolean(0.25f);
        if (this.renderF2) {
            ++floaterCount;
        }
        if (floaterCount < 2) {
            this.renderF3 = MathUtils.randomBoolean(0.25f);
            if (this.renderF3) {
                ++floaterCount;
            }
        }
        if (floaterCount < 2) {
            this.renderF4 = MathUtils.randomBoolean(0.25f);
            if (this.renderF4) {
                ++floaterCount;
            }
        }
        if (floaterCount < 2) {
            this.renderF5 = MathUtils.randomBoolean(0.25f);
        }
        if (MathUtils.randomBoolean(0.3f) || Settings.DISABLE_EFFECTS) {
            this.renderF1 = false;
            this.renderF2 = false;
            this.renderF3 = false;
            this.renderF4 = false;
            this.renderF5 = false;
        }
        this.renderIce = MathUtils.randomBoolean();
        if (this.renderIce) {
            this.renderIce = true;
            this.renderI1 = MathUtils.randomBoolean();
            this.renderI2 = MathUtils.randomBoolean();
            this.renderI3 = MathUtils.randomBoolean();
            this.renderI4 = MathUtils.randomBoolean();
            this.renderI5 = MathUtils.randomBoolean();
        } else {
            this.renderI1 = false;
            this.renderI2 = false;
            this.renderI3 = false;
            this.renderI4 = false;
            this.renderI5 = false;
        }
        this.renderStalactites = MathUtils.randomBoolean();
        if (this.renderStalactites) {
            this.renderStalactites = true;
            this.renderS1 = MathUtils.randomBoolean();
            this.renderS2 = MathUtils.randomBoolean();
            this.renderS3 = MathUtils.randomBoolean();
            this.renderS4 = MathUtils.randomBoolean();
            this.renderS5 = MathUtils.randomBoolean();
        } else {
            this.renderS1 = false;
            this.renderS2 = false;
            this.renderS3 = false;
            this.renderS4 = false;
            this.renderS5 = false;
        }
    }

    @Override
    public void nextRoom(AbstractRoom room) {
        super.nextRoom(room);
        this.randomizeScene();
        if (room instanceof MonsterRoomBoss) {
            CardCrawlGame.music.silenceBGM();
        }
        this.fadeInAmbiance();
    }

    @Override
    public void renderCombatRoomBg(SpriteBatch sb) {
        float prevAlpha = this.overlayColor.a;
        this.overlayColor.a = 1.0f;
        sb.setColor(this.overlayColor);
        this.overlayColor.a = prevAlpha;
        this.renderAtlasRegionIf(sb, this.floor, true);
        this.renderAtlasRegionIf(sb, this.ceiling, true);
        this.renderAtlasRegionIf(sb, this.bg1, true);
        this.renderAtlasRegionIf(sb, this.bg2, this.renderAltBg);
        this.renderAtlasRegionIf(sb, this.mg2, this.renderM2);
        this.renderAtlasRegionIf(sb, this.mg1, this.renderM1);
        this.renderAtlasRegionIf(sb, this.mg3, this.renderM3);
        this.renderAtlasRegionIf(sb, this.mg4, this.renderM4);
        switch (this.columnConfig) {
            case OPEN: {
                break;
            }
            case SMALL_ONLY: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
                break;
            }
            case SMALL_PLUS_LEFT: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c2, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
                break;
            }
            case SMALL_PLUS_RIGHT: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c3, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
            }
        }
        this.renderAtlasRegionIf(sb, this.s1, this.renderS1);
        this.renderAtlasRegionIf(sb, this.s2, this.renderS2);
        this.renderAtlasRegionIf(sb, this.s3, this.renderS3);
        this.renderAtlasRegionIf(sb, this.s4, this.renderS4);
        this.renderAtlasRegionIf(sb, this.s5, this.renderS5);
        sb.setColor(this.overlayColor);
        sb.setBlendFunction(770, 1);
        this.renderAtlasRegionIf(sb, this.bg1, true);
        this.renderAtlasRegionIf(sb, this.bg2, this.renderAltBg);
        this.renderAtlasRegionIf(sb, this.mg2, this.renderM2);
        this.renderAtlasRegionIf(sb, this.mg1, this.renderM1);
        this.renderAtlasRegionIf(sb, this.mg3, this.renderM3);
        this.renderAtlasRegionIf(sb, this.mg4, this.renderM4);
        switch (this.columnConfig) {
            case OPEN: {
                break;
            }
            case SMALL_ONLY: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
                break;
            }
            case SMALL_PLUS_LEFT: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c2, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
                break;
            }
            case SMALL_PLUS_RIGHT: {
                this.renderAtlasRegionIf(sb, this.c1, true);
                this.renderAtlasRegionIf(sb, this.c3, true);
                this.renderAtlasRegionIf(sb, this.c4, true);
            }
        }
        this.renderAtlasRegionIf(sb, this.s1, this.renderS1);
        this.renderAtlasRegionIf(sb, this.s2, this.renderS2);
        this.renderAtlasRegionIf(sb, this.s3, this.renderS3);
        this.renderAtlasRegionIf(sb, this.s4, this.renderS4);
        this.renderAtlasRegionIf(sb, this.s5, this.renderS5);
        sb.setBlendFunction(770, 771);
        this.overlayColor.a = 1.0f;
        sb.setColor(this.overlayColor);
        this.overlayColor.a = prevAlpha;
        this.renderAtlasRegionIf(sb, this.i1, this.renderI1);
        this.renderAtlasRegionIf(sb, this.i2, this.renderI2);
        this.renderAtlasRegionIf(sb, this.i3, this.renderI3);
        this.renderAtlasRegionIf(sb, this.i4, this.renderI4);
        this.renderAtlasRegionIf(sb, this.i5, this.renderI5);
        this.tmpColor.r = (1.0f + this.overlayColor.r) / 2.0f;
        this.tmpColor.g = (1.0f + this.overlayColor.g) / 2.0f;
        this.tmpColor.b = (1.0f + this.overlayColor.b) / 2.0f;
        sb.setColor(this.tmpColor);
        this.renderAtlasRegionIf(sb, MathUtils.cosDeg((System.currentTimeMillis() + 180L) / 180L % 360L) * 40.0f * Settings.xScale, MathUtils.cosDeg((System.currentTimeMillis() + 500L) / 72L % 360L) * 20.0f * Settings.scale, MathUtils.cosDeg((System.currentTimeMillis() + 180L) / 180L % 360L), this.f1, this.renderF1);
        this.renderAtlasRegionIf(sb, MathUtils.cosDeg((System.currentTimeMillis() + 91723L) / 72L % 360L) * 20.0f, 0.0f, System.currentTimeMillis() / 120L % 360L, this.f2, this.renderF2);
        this.renderAtlasRegionIf(sb, -80.0f * Settings.scale, MathUtils.cosDeg(System.currentTimeMillis() + 73L) * 10.0f - 90.0f * Settings.scale, (float)(System.currentTimeMillis() / 1000L % 360L) * 2.0f, this.f3, this.renderF3);
        this.renderAtlasRegionIf(sb, 0.0f, MathUtils.cosDeg((System.currentTimeMillis() + 4442L) / 20L % 360L) * 30.0f * Settings.scale, MathUtils.cosDeg((System.currentTimeMillis() + 4442L) / 10L % 360L) * 20.0f, this.f4, this.renderF4);
        this.renderAtlasRegionIf(sb, 0.0f, MathUtils.cosDeg(System.currentTimeMillis() / 48L % 360L) * 20.0f, 0.0f, this.f5, this.renderF5);
    }

    @Override
    public void renderCombatRoomFg(SpriteBatch sb) {
        sb.setColor(this.tmpColor);
        this.renderAtlasRegionIf(sb, this.fg, true);
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

    private static enum ColumnConfig {
        OPEN,
        SMALL_ONLY,
        SMALL_PLUS_LEFT,
        SMALL_PLUS_RIGHT;

    }
}

