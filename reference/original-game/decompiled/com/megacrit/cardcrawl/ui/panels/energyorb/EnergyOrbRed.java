/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels.energyorb;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbInterface;

public class EnergyOrbRed
implements EnergyOrbInterface {
    private static final int ORB_W = 128;
    public static float fontScale = 1.0f;
    private static final float ORB_IMG_SCALE = 1.15f * Settings.scale;
    private float angle5;
    private float angle4;
    private float angle3;
    private float angle2;
    private float angle1;

    @Override
    public void updateOrb(int orbCount) {
        if (orbCount == 0) {
            this.angle5 += Gdx.graphics.getDeltaTime() * -5.0f;
            this.angle4 += Gdx.graphics.getDeltaTime() * 5.0f;
            this.angle3 += Gdx.graphics.getDeltaTime() * -8.0f;
            this.angle2 += Gdx.graphics.getDeltaTime() * 8.0f;
            this.angle1 += Gdx.graphics.getDeltaTime() * 72.0f;
        } else {
            this.angle5 += Gdx.graphics.getDeltaTime() * -20.0f;
            this.angle4 += Gdx.graphics.getDeltaTime() * 20.0f;
            this.angle3 += Gdx.graphics.getDeltaTime() * -40.0f;
            this.angle2 += Gdx.graphics.getDeltaTime() * 40.0f;
            this.angle1 += Gdx.graphics.getDeltaTime() * 360.0f;
        }
    }

    @Override
    public void renderOrb(SpriteBatch sb, boolean enabled, float current_x, float current_y) {
        if (enabled) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.ENERGY_RED_LAYER1, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle1, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER2, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle2, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER3, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle3, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER4, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle4, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER5, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle5, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER6, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, 0.0f, 0, 0, 128, 128, false, false);
        } else {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.ENERGY_RED_LAYER1D, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle1, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER2D, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle2, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER3D, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle3, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER4D, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle4, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER5D, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, this.angle5, 0, 0, 128, 128, false, false);
            sb.draw(ImageMaster.ENERGY_RED_LAYER6, current_x - 64.0f, current_y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, ORB_IMG_SCALE, ORB_IMG_SCALE, 0.0f, 0, 0, 128, 128, false, false);
        }
    }
}

