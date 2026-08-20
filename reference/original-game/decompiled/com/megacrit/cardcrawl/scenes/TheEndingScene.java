/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.TrueVictoryRoom;
import com.megacrit.cardcrawl.scenes.AbstractScene;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.scene.ShinySparkleEffect;
import com.megacrit.cardcrawl.vfx.scene.WobblyCircleEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class TheEndingScene
extends AbstractScene {
    private ArrayList<AbstractGameEffect> circles = new ArrayList();

    public TheEndingScene() {
        super("endingScene/scene.atlas");
        this.ambianceName = "AMBIANCE_BEYOND";
        this.fadeInAmbiance();
    }

    @Override
    public void update() {
        super.update();
        this.updateParticles();
    }

    @Override
    public void randomizeScene() {
    }

    private void updateParticles() {
        Iterator<AbstractGameEffect> e = this.circles.iterator();
        while (e.hasNext()) {
            AbstractGameEffect effect = e.next();
            effect.update();
            if (!effect.isDone) continue;
            e.remove();
        }
        if (!(AbstractDungeon.getCurrRoom() instanceof TrueVictoryRoom) && this.circles.size() < 72 && !Settings.DISABLE_EFFECTS) {
            if (MathUtils.randomBoolean(0.2f)) {
                this.circles.add(new ShinySparkleEffect());
            } else {
                this.circles.add(new WobblyCircleEffect());
            }
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
        sb.setColor(Color.WHITE);
        this.renderAtlasRegionIf(sb, this.bg, true);
    }

    @Override
    public void renderCombatRoomFg(SpriteBatch sb) {
        if (!this.isCamp) {
            sb.setBlendFunction(770, 1);
            for (AbstractGameEffect e : this.circles) {
                e.render(sb);
            }
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void renderCampfireRoom(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        this.renderAtlasRegionIf(sb, this.campfireBg, true);
    }
}

