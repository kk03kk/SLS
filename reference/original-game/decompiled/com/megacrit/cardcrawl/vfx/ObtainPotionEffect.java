/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ObtainPotionEffect
extends AbstractGameEffect {
    private AbstractPotion potion;

    public ObtainPotionEffect(AbstractPotion potion) {
        this.potion = potion;
    }

    @Override
    public void update() {
        if (!AbstractDungeon.player.hasRelic("Sozu")) {
            AbstractDungeon.player.obtainPotion(this.potion);
        }
        this.isDone = true;
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
    }

    @Override
    public void dispose() {
    }
}

