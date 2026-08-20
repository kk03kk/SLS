/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

public class TooltipManager {
    private static TooltipManager instance;
    private static Files files;
    public float initialTime = 2.0f;
    public float subsequentTime = 0.0f;
    public float resetTime = 1.5f;
    public boolean enabled = true;
    public boolean animations = true;
    public float maxWidth = 2.1474836E9f;
    public float offsetX = 15.0f;
    public float offsetY = 19.0f;
    public float edgeDistance = 7.0f;
    final Array<Tooltip> shown = new Array();
    float time = this.initialTime;
    final Timer.Task resetTask = new Timer.Task(){

        @Override
        public void run() {
            TooltipManager.this.time = TooltipManager.this.initialTime;
        }
    };
    Tooltip showTooltip;
    final Timer.Task showTask = new Timer.Task(){

        @Override
        public void run() {
            if (TooltipManager.this.showTooltip == null) {
                return;
            }
            Stage stage = TooltipManager.this.showTooltip.targetActor.getStage();
            if (stage == null) {
                return;
            }
            stage.addActor(TooltipManager.this.showTooltip.container);
            TooltipManager.this.showTooltip.container.toFront();
            TooltipManager.this.shown.add(TooltipManager.this.showTooltip);
            TooltipManager.this.showTooltip.container.clearActions();
            TooltipManager.this.showAction(TooltipManager.this.showTooltip);
            if (!TooltipManager.this.showTooltip.instant) {
                TooltipManager.this.time = TooltipManager.this.subsequentTime;
                TooltipManager.this.resetTask.cancel();
            }
        }
    };

    public void touchDown(Tooltip tooltip) {
        this.showTask.cancel();
        if (tooltip.container.remove()) {
            this.resetTask.cancel();
        }
        this.resetTask.run();
        if (this.enabled || tooltip.always) {
            this.showTooltip = tooltip;
            Timer.schedule(this.showTask, this.time);
        }
    }

    public void enter(Tooltip tooltip) {
        this.showTooltip = tooltip;
        this.showTask.cancel();
        if (this.enabled || tooltip.always) {
            if (this.time == 0.0f || tooltip.instant) {
                this.showTask.run();
            } else {
                Timer.schedule(this.showTask, this.time);
            }
        }
    }

    public void hide(Tooltip tooltip) {
        this.showTooltip = null;
        this.showTask.cancel();
        if (tooltip.container.hasParent()) {
            this.shown.removeValue(tooltip, true);
            this.hideAction(tooltip);
            this.resetTask.cancel();
            Timer.schedule(this.resetTask, this.resetTime);
        }
    }

    protected void showAction(Tooltip tooltip) {
        float actionTime = this.animations ? (this.time > 0.0f ? 0.5f : 0.15f) : 0.1f;
        tooltip.container.setTransform(true);
        tooltip.container.getColor().a = 0.2f;
        tooltip.container.setScale(0.05f);
        tooltip.container.addAction(Actions.parallel((Action)Actions.fadeIn(actionTime, Interpolation.fade), (Action)Actions.scaleTo(1.0f, 1.0f, actionTime, Interpolation.fade)));
    }

    protected void hideAction(Tooltip tooltip) {
        tooltip.container.addAction(Actions.sequence((Action)Actions.parallel((Action)Actions.alpha(0.2f, 0.2f, Interpolation.fade), (Action)Actions.scaleTo(0.05f, 0.05f, 0.2f, Interpolation.fade)), (Action)Actions.removeActor()));
    }

    public void hideAll() {
        this.resetTask.cancel();
        this.showTask.cancel();
        this.time = this.initialTime;
        this.showTooltip = null;
        for (Tooltip tooltip : this.shown) {
            tooltip.hide();
        }
        this.shown.clear();
    }

    public void instant() {
        this.time = 0.0f;
        this.showTask.run();
        this.showTask.cancel();
    }

    public static TooltipManager getInstance() {
        if (files == null || files != Gdx.files) {
            files = Gdx.files;
            instance = new TooltipManager();
        }
        return instance;
    }
}

