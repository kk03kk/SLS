/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.scenes.TitleCloud;
import com.megacrit.cardcrawl.vfx.scene.LogoFlameEffect;
import com.megacrit.cardcrawl.vfx.scene.TitleDustEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class TitleBackground {
    protected static TextureAtlas atlas;
    protected final TextureAtlas.AtlasRegion mg3Bot;
    protected final TextureAtlas.AtlasRegion mg3Top;
    protected final TextureAtlas.AtlasRegion topGlow;
    protected final TextureAtlas.AtlasRegion topGlow2;
    protected final TextureAtlas.AtlasRegion botGlow;
    protected final TextureAtlas.AtlasRegion sky;
    private static Texture titleLogoImg;
    private static int W;
    private static int H;
    protected ArrayList<TitleCloud> topClouds = new ArrayList();
    protected ArrayList<TitleCloud> midClouds = new ArrayList();
    public float slider = 1.0f;
    private float timer = 1.0f;
    public boolean activated = false;
    private ArrayList<TitleDustEffect> dust = new ArrayList();
    private ArrayList<TitleDustEffect> dust2 = new ArrayList();
    private ArrayList<LogoFlameEffect> flame = new ArrayList();
    private float dustTimer = 2.0f;
    private float flameTimer = 0.2f;
    private static final float FLAME_INTERVAL = 0.05f;
    private float logoAlpha = 1.0f;
    private Color promptTextColor = Settings.CREAM_COLOR.cpy();

    public TitleBackground() {
        int i;
        this.promptTextColor.a = 0.0f;
        if (atlas == null) {
            atlas = new TextureAtlas(Gdx.files.internal("title/title.atlas"));
        }
        this.sky = atlas.findRegion("jpg/sky");
        this.mg3Bot = atlas.findRegion("mg3Bot");
        this.mg3Top = atlas.findRegion("mg3Top");
        this.topGlow = atlas.findRegion("mg3TopGlow1");
        this.topGlow2 = atlas.findRegion("mg3TopGlow2");
        this.botGlow = atlas.findRegion("mg3BotGlow");
        for (i = 1; i < 7; ++i) {
            this.topClouds.add(new TitleCloud(atlas.findRegion("topCloud" + Integer.toString(i)), MathUtils.random(10.0f, 50.0f) * Settings.scale, MathUtils.random(-1920.0f, 1920.0f) * Settings.scale));
        }
        for (i = 1; i < 13; ++i) {
            this.midClouds.add(new TitleCloud(atlas.findRegion("midCloud" + Integer.toString(i)), MathUtils.random(-50.0f, -10.0f) * Settings.scale, MathUtils.random(-1920.0f, 1920.0f) * Settings.scale));
        }
        if (titleLogoImg == null) {
            switch (Settings.language) {
                default: 
            }
            titleLogoImg = ImageMaster.loadImage("images/ui/title_logo/eng.png");
            W = titleLogoImg.getWidth();
            H = titleLogoImg.getHeight();
        }
    }

    public void slideDownInstantly() {
        this.activated = true;
        this.timer = 0.0f;
        this.slider = 0.0f;
    }

    public void update() {
        this.logoAlpha = CardCrawlGame.mainMenuScreen.darken ? MathHelper.slowColorLerpSnap(this.logoAlpha, 0.25f) : MathHelper.slowColorLerpSnap(this.logoAlpha, 1.0f);
        if (InputHelper.justClickedLeft && !this.activated) {
            this.activated = true;
            this.timer = 1.0f;
        }
        if (this.activated && this.timer != 0.0f) {
            this.timer -= Gdx.graphics.getDeltaTime();
            if (this.timer < 0.0f) {
                this.timer = 0.0f;
            }
            if (this.timer < 1.0f) {
                this.slider = Interpolation.pow4In.apply(0.0f, 1.0f, this.timer);
            }
        }
        for (TitleCloud c : this.topClouds) {
            c.update();
        }
        for (TitleCloud c : this.midClouds) {
            c.update();
        }
        if (!Settings.DISABLE_EFFECTS) {
            this.updateDust();
        }
        if (!CardCrawlGame.mainMenuScreen.isFadingOut) {
            this.updateFlame();
        } else {
            this.flame.clear();
        }
    }

    private void updateFlame() {
        this.flameTimer -= Gdx.graphics.getDeltaTime();
        if (this.flameTimer < 0.0f) {
            this.flameTimer = 0.05f;
            this.flame.add(new LogoFlameEffect());
        }
        Iterator<LogoFlameEffect> e = this.flame.iterator();
        while (e.hasNext()) {
            LogoFlameEffect effect = e.next();
            effect.update();
            if (!effect.isDone) continue;
            e.remove();
        }
    }

    private void updateDust() {
        this.dustTimer -= Gdx.graphics.getDeltaTime();
        if (this.dustTimer < 0.0f) {
            this.dustTimer = 0.05f;
            this.dust.add(new TitleDustEffect());
        }
        Iterator<TitleDustEffect> e = this.dust.iterator();
        while (e.hasNext()) {
            TitleDustEffect effect = e.next();
            effect.update();
            if (!effect.isDone) continue;
            e.remove();
        }
    }

    public void render(SpriteBatch sb) {
        this.renderRegion(sb, this.sky, 0.0f, -100.0f * Settings.scale * this.slider);
        this.renderRegion(sb, this.mg3Bot, 0.0f, MathUtils.round(-45.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 2219.0f * Settings.scale));
        this.renderRegion(sb, this.mg3Top, 0.0f, MathUtils.round(-45.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 1080.0f * Settings.scale));
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(1.0f, 0.2f, 0.1f, 0.1f + (MathUtils.cosDeg(System.currentTimeMillis() / 16L % 360L) + 1.25f) / 5.0f));
        this.renderRegion(sb, this.botGlow, 0.0f, MathUtils.round(-45.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 2220.0f * Settings.scale));
        this.renderRegion(sb, this.topGlow, 0.0f, -45.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 1080.0f * Settings.scale);
        this.renderRegion(sb, this.topGlow2, 0.0f, -45.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 1080.0f * Settings.scale);
        sb.setColor(Color.WHITE);
        sb.setBlendFunction(770, 771);
        for (TitleDustEffect titleDustEffect : this.dust2) {
            titleDustEffect.render(sb, 0.0f, -50.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 1300.0f * Settings.scale);
        }
        for (TitleDustEffect titleDustEffect : this.dust) {
            titleDustEffect.render(sb, 0.0f, -50.0f * Settings.scale * this.slider + (float)Settings.HEIGHT - 1300.0f * Settings.scale);
        }
        sb.setColor(Color.WHITE);
        for (TitleCloud titleCloud : this.midClouds) {
            titleCloud.render(sb, this.slider);
        }
        for (TitleCloud titleCloud : this.topClouds) {
            titleCloud.render(sb, this.slider);
        }
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.logoAlpha));
        sb.draw(titleLogoImg, 930.0f * Settings.xScale - (float)W / 2.0f, -70.0f * Settings.scale * this.slider + (float)Settings.HEIGHT / 2.0f - (float)H / 2.0f + 14.0f * Settings.scale, (float)W / 2.0f, (float)H / 2.0f, W, H, Settings.scale, Settings.scale, 0.0f, 0, 0, W, H, false, false);
        sb.setBlendFunction(770, 1);
        for (LogoFlameEffect logoFlameEffect : this.flame) {
            switch (Settings.language) {
                default: 
            }
            logoFlameEffect.render(sb, (float)Settings.WIDTH / 2.0f, -70.0f * Settings.scale * this.slider + (float)Settings.HEIGHT / 2.0f - 260.0f * Settings.scale);
        }
        sb.setBlendFunction(770, 771);
    }

    private void renderRegion(SpriteBatch sb, TextureAtlas.AtlasRegion region, float x, float y) {
        if (Settings.isLetterbox) {
            sb.draw(region.getTexture(), region.offsetX * Settings.scale + x, region.offsetY * Settings.scale + y, 0.0f, 0.0f, region.packedWidth, region.packedHeight, Settings.xScale, Settings.xScale, 0.0f, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(), false, false);
        } else {
            sb.draw(region.getTexture(), region.offsetX * Settings.scale + x, region.offsetY * Settings.scale + y, 0.0f, 0.0f, region.packedWidth, region.packedHeight, Settings.scale, Settings.scale, 0.0f, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(), false, false);
        }
    }

    static {
        titleLogoImg = null;
        W = 0;
        H = 0;
    }
}

