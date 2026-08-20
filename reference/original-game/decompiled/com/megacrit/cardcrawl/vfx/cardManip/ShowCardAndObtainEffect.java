/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.cardManip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Omamori;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.CardPoofEffect;

public class ShowCardAndObtainEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 2.0f;
    private static final float FAST_DUR = 0.5f;
    private AbstractCard card;
    private static final float PADDING = 30.0f * Settings.scale;
    private boolean converge;

    public ShowCardAndObtainEffect(AbstractCard card, float x, float y, boolean convergeCards) {
        if (card.color == AbstractCard.CardColor.CURSE && AbstractDungeon.player.hasRelic("Omamori") && AbstractDungeon.player.getRelic((String)"Omamori").counter != 0) {
            ((Omamori)AbstractDungeon.player.getRelic("Omamori")).use();
            this.duration = 0.0f;
            this.isDone = true;
            this.converge = convergeCards;
        }
        UnlockTracker.markCardAsSeen(card.cardID);
        CardHelper.obtain(card.cardID, card.rarity, card.color);
        this.card = card;
        this.duration = Settings.FAST_MODE ? 0.5f : 2.0f;
        this.identifySpawnLocation(x, y);
        AbstractDungeon.effectsQueue.add(new CardPoofEffect(card.target_x, card.target_y));
        card.drawScale = 0.01f;
        card.targetDrawScale = 1.0f;
    }

    public ShowCardAndObtainEffect(AbstractCard card, float x, float y) {
        this(card, x, y, true);
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ShowCardAndObtainEffect)) continue;
            ++effectCount;
        }
        this.card.current_x = x;
        this.card.current_y = y;
        if (this.converge) {
            this.card.target_y = (float)Settings.HEIGHT * 0.5f;
            switch (effectCount) {
                case 0: {
                    this.card.target_x = (float)Settings.WIDTH * 0.5f;
                    break;
                }
                case 1: {
                    this.card.target_x = (float)Settings.WIDTH * 0.5f - PADDING - AbstractCard.IMG_WIDTH;
                    break;
                }
                case 2: {
                    this.card.target_x = (float)Settings.WIDTH * 0.5f + PADDING + AbstractCard.IMG_WIDTH;
                    break;
                }
                case 3: {
                    this.card.target_x = (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                    break;
                }
                case 4: {
                    this.card.target_x = (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                    break;
                }
                default: {
                    this.card.target_x = MathUtils.random((float)Settings.WIDTH * 0.1f, (float)Settings.WIDTH * 0.9f);
                    this.card.target_y = MathUtils.random((float)Settings.HEIGHT * 0.2f, (float)Settings.HEIGHT * 0.8f);
                    break;
                }
            }
        } else {
            this.card.target_x = this.card.current_x;
            this.card.target_y = this.card.current_y;
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.card.update();
        if (this.duration < 0.0f) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onObtainCard(this.card);
            }
            this.isDone = true;
            this.card.shrink();
            AbstractDungeon.getCurrRoom().souls.obtain(this.card, true);
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onMasterDeckChange();
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            this.card.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

