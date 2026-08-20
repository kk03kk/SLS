/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.vfx.WarningSignEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class DeleteSaveConfirmPopup
extends ConfirmPopup {
    protected static final String[] D_TEXT = CardCrawlGame.languagePack.getUIString((String)"DeletePopup").TEXT;
    private ArrayList<WarningSignEffect> effects = new ArrayList();

    public DeleteSaveConfirmPopup() {
        super(D_TEXT[0], D_TEXT[3], ConfirmPopup.ConfirmType.DELETE_SAVE);
    }

    @Override
    public void update() {
        super.update();
        if (this.shown && this.effects.isEmpty()) {
            this.effects.add(new WarningSignEffect((float)Settings.WIDTH / 2.0f, Settings.OPTION_Y + 275.0f * Settings.scale));
        }
        Iterator<WarningSignEffect> i = this.effects.iterator();
        while (i.hasNext()) {
            WarningSignEffect e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    public void open(int slot) {
        this.slot = slot;
        this.shown = true;
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        this.renderWarning(sb);
    }

    private void renderWarning(SpriteBatch sb) {
        for (WarningSignEffect e : this.effects) {
            e.render(sb);
        }
    }
}

