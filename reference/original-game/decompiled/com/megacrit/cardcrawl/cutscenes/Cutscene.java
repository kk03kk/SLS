/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cutscenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.CutscenePanel;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.VictoryScreen;
import java.util.ArrayList;

public class Cutscene
implements Disposable {
    private int currentScene = 0;
    private float darkenTimer = 1.0f;
    private float fadeTimer = 1.0f;
    private float switchTimer = 1.0f;
    private Color screenColor;
    private Color bgColor;
    private ArrayList<CutscenePanel> panels = new ArrayList();
    private Texture bgImg;
    private boolean isDone = false;

    public Cutscene(AbstractPlayer.PlayerClass chosenClass) {
        switch (chosenClass) {
            case IRONCLAD: {
                this.bgImg = ImageMaster.loadImage("images/scenes/redBg.jpg");
                this.panels.add(new CutscenePanel("images/scenes/ironclad1.png", "ATTACK_HEAVY"));
                this.panels.add(new CutscenePanel("images/scenes/ironclad2.png"));
                this.panels.add(new CutscenePanel("images/scenes/ironclad3.png"));
                break;
            }
            case THE_SILENT: {
                this.bgImg = ImageMaster.loadImage("images/scenes/greenBg.jpg");
                this.panels.add(new CutscenePanel("images/scenes/silent1.png", "ATTACK_POISON2"));
                this.panels.add(new CutscenePanel("images/scenes/silent2.png"));
                this.panels.add(new CutscenePanel("images/scenes/silent3.png"));
                break;
            }
            case DEFECT: {
                this.bgImg = ImageMaster.loadImage("images/scenes/blueBg.jpg");
                this.panels.add(new CutscenePanel("images/scenes/defect1.png", "ATTACK_MAGIC_BEAM_SHORT"));
                this.panels.add(new CutscenePanel("images/scenes/defect2.png"));
                this.panels.add(new CutscenePanel("images/scenes/defect3.png"));
                break;
            }
            case WATCHER: {
                this.bgImg = ImageMaster.loadImage("images/scenes/purpleBg.jpg");
                this.panels.add(new CutscenePanel("images/scenes/watcher1.png", "WATCHER_HEART_PUNCH"));
                this.panels.add(new CutscenePanel("images/scenes/watcher2.png"));
                this.panels.add(new CutscenePanel("images/scenes/watcher3.png"));
                break;
            }
            default: {
                this.bgImg = ImageMaster.loadImage("images/scenes/redBg.jpg");
                this.panels.add(new CutscenePanel("images/scenes/ironclad1.png", "ATTACK_HEAVY"));
                this.panels.add(new CutscenePanel("images/scenes/ironclad2.png"));
                this.panels.add(new CutscenePanel("images/scenes/ironclad3.png"));
            }
        }
        this.bgColor = Color.WHITE.cpy();
        this.screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void update() {
        this.updateFadeOut();
        this.updateFadeIn();
        for (CutscenePanel p : this.panels) {
            p.update();
        }
        this.updateIfDone();
        this.updateSceneChange();
    }

    private void updateIfDone() {
        if (this.isDone) {
            this.bgColor.a -= Gdx.graphics.getDeltaTime();
            for (CutscenePanel p : this.panels) {
                if (p.finished) continue;
                return;
            }
            this.dispose();
            this.bgColor.a = 0.0f;
            this.openVictoryScreen();
        }
    }

    private void updateSceneChange() {
        this.switchTimer -= Gdx.graphics.getDeltaTime();
        if ((InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) && this.switchTimer > 1.0f) {
            this.switchTimer = 1.0f;
        }
        if (this.switchTimer < 0.0f) {
            for (CutscenePanel p : this.panels) {
                if (p.activated) continue;
                p.activate();
                this.switchTimer = 5.0f;
                return;
            }
            for (CutscenePanel p : this.panels) {
                p.fadeOut();
            }
            this.isDone = true;
        }
    }

    private void openVictoryScreen() {
        GameCursor.hidden = false;
        AbstractDungeon.victoryScreen = new VictoryScreen(null);
    }

    private void updateFadeIn() {
        if (this.darkenTimer == 0.0f) {
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeTimer < 0.0f) {
                this.fadeTimer = 0.0f;
            }
            this.screenColor.a = this.fadeTimer;
        }
    }

    private void updateFadeOut() {
        if (this.darkenTimer != 0.0f) {
            this.darkenTimer -= Gdx.graphics.getDeltaTime();
            if (this.darkenTimer < 0.0f) {
                this.darkenTimer = 0.0f;
                this.fadeTimer = 1.0f;
                this.switchTimer = 1.0f;
            }
            this.screenColor.a = 1.0f - this.darkenTimer;
        }
    }

    public void render(SpriteBatch sb) {
        if (this.currentScene <= 1) {
            sb.setColor(Color.BLACK);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        }
    }

    public void renderAbove(SpriteBatch sb) {
        if (this.bgImg != null) {
            sb.setColor(this.bgColor);
            this.renderImg(sb, this.bgImg);
        }
        this.renderPanels(sb);
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    private void renderPanels(SpriteBatch sb) {
        for (CutscenePanel p : this.panels) {
            p.render(sb);
        }
    }

    private void renderImg(SpriteBatch sb, Texture img) {
        if (Settings.isSixteenByTen) {
            sb.draw(img, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        } else {
            sb.draw(img, 0.0f, -50.0f * Settings.scale, (float)Settings.WIDTH, (float)Settings.HEIGHT + 110.0f * Settings.scale);
        }
    }

    @Override
    public void dispose() {
        if (this.bgImg != null) {
            this.bgImg.dispose();
            this.bgImg = null;
        }
        for (CutscenePanel p : this.panels) {
            p.dispose();
        }
    }
}

