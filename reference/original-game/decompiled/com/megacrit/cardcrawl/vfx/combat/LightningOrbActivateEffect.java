/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;

public class LightningOrbActivateEffect
extends AbstractGameEffect {
    private static ArrayList<TextureAtlas.AtlasRegion> regions = null;
    private TextureAtlas.AtlasRegion img = null;
    private int index = 0;
    private float x;
    private float y;

    public LightningOrbActivateEffect(float x, float y) {
        this.renderBehind = false;
        this.color = Settings.LIGHT_YELLOW_COLOR.cpy();
        if (regions == null) {
            regions = new ArrayList();
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb1"));
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb2"));
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb3"));
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb4"));
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb5"));
            regions.add(ImageMaster.vfxAtlas.findRegion("combat/defect/l_orb6"));
        }
        this.img = regions.get(this.index);
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.scale = 2.0f * Settings.scale;
        this.duration = 0.03f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            ++this.index;
            if (this.index > regions.size() - 1) {
                this.isDone = true;
                return;
            }
            this.img = regions.get(this.index);
            this.duration = 0.03f;
        }
        this.color.a -= Gdx.graphics.getDeltaTime() * 2.0f;
        if (this.color.a < 0.0f) {
            this.color.a = 0.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

