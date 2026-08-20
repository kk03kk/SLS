/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.scene.EventBgParticle;

public abstract class AbstractImageEvent
extends AbstractEvent {
    protected String title;
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Proceed Screen");
    private static final String[] DESCRIPTIONS = AbstractImageEvent.eventStrings.DESCRIPTIONS;

    public AbstractImageEvent(String title, String body, String imgUrl) {
        this.imageEventText.clear();
        this.roomEventText.clear();
        this.title = title;
        this.body = body;
        this.imageEventText.loadImage(imgUrl);
        type = AbstractEvent.EventType.IMAGE;
        this.noCardsInRewards = false;
    }

    @Override
    public void update() {
        if (!this.combatTime) {
            this.hasFocus = true;
            if (MathUtils.randomBoolean(0.1f)) {
                AbstractDungeon.effectList.add(new EventBgParticle());
            }
            if (this.waitTimer > 0.0f) {
                this.waitTimer -= Gdx.graphics.getDeltaTime();
                if (this.waitTimer < 0.0f) {
                    this.imageEventText.show(this.title, this.body);
                    this.waitTimer = 0.0f;
                }
            }
            if (!GenericEventDialog.waitForInput) {
                this.buttonEffect(GenericEventDialog.getSelectedOption());
            }
        }
    }

    @Override
    public void showProceedScreen(String bodyText) {
        this.imageEventText.updateBodyText(bodyText);
        this.imageEventText.updateDialogOption(0, DESCRIPTIONS[0]);
        this.imageEventText.clearRemainingOptions();
        this.screenNum = 99;
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    protected void openMap() {
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
        AbstractDungeon.dungeonMapScreen.open(false);
    }

    public void enterCombatFromImage() {
        AbstractDungeon.getCurrRoom().smoked = false;
        AbstractDungeon.player.isEscaping = false;
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
        AbstractDungeon.getCurrRoom().monsters.init();
        AbstractRoom.waitTimer = 0.1f;
        AbstractDungeon.player.preBattlePrep();
        this.hasFocus = false;
        GenericEventDialog.hide();
        CardCrawlGame.fadeIn(1.5f);
        AbstractDungeon.rs = AbstractDungeon.RenderScene.NORMAL;
        this.combatTime = true;
    }

    public void enterImageFromCombat() {
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.EVENT;
        AbstractDungeon.getCurrRoom().isBattleOver = false;
        AbstractDungeon.getCurrRoom().monsters.monsters.clear();
        AbstractDungeon.getCurrRoom().rewards.clear();
        this.hasDialog = true;
        this.hasFocus = true;
        this.combatTime = false;
        GenericEventDialog.show();
        CardCrawlGame.fadeIn(1.5f);
        AbstractDungeon.rs = AbstractDungeon.RenderScene.EVENT;
    }
}

