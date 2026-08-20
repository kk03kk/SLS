/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.ui.buttons.UnlockConfirmButton;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.ConeEffect;
import com.megacrit.cardcrawl.vfx.RoomShineEffect;
import com.megacrit.cardcrawl.vfx.RoomShineEffect2;
import java.util.ArrayList;
import java.util.Iterator;

public class UnlockCharacterScreen {
    private Color unlockBgColor = new Color(0.1f, 0.2f, 0.25f, 1.0f);
    public AbstractUnlock unlock;
    private ArrayList<ConeEffect> cones = new ArrayList();
    private float shinyTimer = 0.0f;
    public UnlockConfirmButton button = new UnlockConfirmButton();
    public long id;

    public void open(AbstractUnlock unlock) {
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.UNLOCK;
        this.unlock = unlock;
        this.id = CardCrawlGame.sound.play("UNLOCK_SCREEN");
        this.button.show();
        this.cones.clear();
        for (int i = 0; i < 30; ++i) {
            this.cones.add(new ConeEffect());
        }
        unlock.onUnlockScreenOpen();
        UnlockTracker.hardUnlockOverride(unlock.key);
        UnlockTracker.lockedCharacters.remove(unlock.key);
        AbstractDungeon.dynamicBanner.appearInstantly(CardCrawlGame.languagePack.getUIString((String)"UnlockCharacterScreen").TEXT[3]);
    }

    public void update() {
        if (InputHelper.justClickedRight) {
            this.button.show();
        }
        this.shinyTimer -= Gdx.graphics.getDeltaTime();
        if (this.shinyTimer < 0.0f) {
            this.shinyTimer = 0.2f;
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect());
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect());
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect2());
        }
        this.updateConeEffect();
        this.unlock.player.update();
        this.button.update();
    }

    private void updateConeEffect() {
        Iterator<ConeEffect> e = this.cones.iterator();
        while (e.hasNext()) {
            ConeEffect d = e.next();
            d.update();
            if (!d.isDone) continue;
            e.remove();
        }
        if (this.cones.size() < 30) {
            this.cones.add(new ConeEffect());
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.unlockBgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setBlendFunction(770, 1);
        for (ConeEffect e : this.cones) {
            e.render(sb);
        }
        sb.setBlendFunction(770, 771);
        this.unlock.render(sb);
        this.unlock.player.renderPlayerImage(sb);
        this.button.render(sb);
    }
}

