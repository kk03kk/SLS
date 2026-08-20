/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractPanel {
    private static final Logger logger = LogManager.getLogger(AbstractPanel.class.getName());
    private static final float SNAP_THRESHOLD = 0.5f;
    private static final float LERP_SPEED = 7.0f;
    public float hide_x;
    public float hide_y;
    public float show_x;
    public float show_y;
    protected Texture img;
    protected float img_width;
    protected float img_height;
    public boolean isHidden = false;
    public float target_x;
    public float target_y;
    public float current_x;
    public float current_y;
    public boolean doneAnimating = true;

    public AbstractPanel(float show_x, float show_y, float hide_x, float hide_y, float shadow_offset_x, float shadow_offset_y, Texture img, boolean startHidden) {
        this.img = img;
        this.show_x = show_x;
        this.show_y = show_y;
        this.hide_x = hide_x;
        this.hide_y = hide_y;
        if (img != null) {
            this.img_width = (float)img.getWidth() * Settings.scale;
            this.img_height = (float)img.getHeight() * Settings.scale;
        }
        if (startHidden) {
            this.current_x = hide_x;
            this.current_y = hide_y;
            this.target_x = hide_x;
            this.target_y = hide_y;
            this.isHidden = true;
        } else {
            this.current_x = show_x;
            this.current_y = show_y;
            this.target_x = show_x;
            this.target_y = show_y;
            this.isHidden = false;
        }
    }

    public AbstractPanel(float show_x, float show_y, float hide_x, float hide_y, Texture img, boolean startHidden) {
        this(show_x, show_y, hide_x, hide_y, 0.0f, 0.0f, img, startHidden);
    }

    public void show() {
        if (this.isHidden) {
            this.target_x = this.show_x;
            this.target_y = this.show_y;
            this.isHidden = false;
            this.doneAnimating = false;
        } else if (Settings.isDebug) {
            logger.info("Attempting to show panel through already shown");
        }
    }

    public void hide() {
        if (!this.isHidden) {
            this.target_x = this.hide_x;
            this.target_y = this.hide_y;
            this.isHidden = true;
            this.doneAnimating = false;
        }
    }

    public void updatePositions() {
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 7.0f);
            if (Math.abs(this.current_x - this.target_x) < 0.5f) {
                this.current_x = this.target_x;
                this.doneAnimating = true;
            } else {
                this.doneAnimating = false;
            }
        }
        if (this.current_y != this.target_y) {
            this.current_y = MathUtils.lerp(this.current_y, this.target_y, Gdx.graphics.getDeltaTime() * 7.0f);
            if (Math.abs(this.current_y - this.target_y) < 0.5f) {
                this.current_y = this.target_y;
                this.doneAnimating = true;
            } else {
                this.doneAnimating = false;
            }
        }
    }

    public void render(SpriteBatch sb) {
        if (this.img != null) {
            sb.setColor(Color.WHITE);
            sb.draw(this.img, this.current_x, this.current_y, this.img_width, this.img_height, 0, 0, this.img.getWidth(), this.img.getHeight(), false, false);
        }
    }
}

