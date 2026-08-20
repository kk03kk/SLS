/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.screens.compendium.CardLibSortHeader;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButton;
import java.util.Comparator;

public class MasterDeckSortHeader
extends CardLibSortHeader {
    private static final int BAR_W = 1334;
    private static final int BAR_H = 102;
    private static final Color BAR_COLOR = new Color(0.4f, 0.4f, 0.4f, 1.0f);
    private static final Color IRONCLAD_COLOR = new Color(0.5f, 0.1f, 0.1f, 1.0f);
    private static final Color SILENT_COLOR = new Color(0.25f, 0.55f, 0.0f, 1.0f);
    private static final Color DEFECT_COLOR = new Color(0.01f, 0.34f, 0.52f, 1.0f);
    private static final Comparator<AbstractCard> BY_TYPE = (a, b) -> (a.type.name() + a.name).compareTo(b.type.name() + b.name);
    private static final Comparator<AbstractCard> ALPHA = (a, b) -> a.name.compareTo(b.name);
    private static final Comparator<AbstractCard> BY_COST = (a, b) -> ("" + a.cost + a.name).compareTo("" + b.cost + b.name);
    private static final Comparator<AbstractCard> PURE_REVERSE = (a, b) -> a.cardID.equals(b.cardID) ? 0 : -1;
    private MasterDeckViewScreen masterDeckView;
    private float scrollY;

    public MasterDeckSortHeader(MasterDeckViewScreen masterDeckView) {
        super(null);
        this.masterDeckView = masterDeckView;
        this.buttons[0] = new SortHeaderButton(TEXT[5], START_X, 0.0f, this);
        this.buttons[0].setActive(true);
        float HB_W = this.buttons[0].hb.width;
        float leftSideOffset = (float)Settings.WIDTH / 2.0f - HB_W * (float)this.buttons.length / 2.0f;
        for (int i = 0; i < this.buttons.length; ++i) {
            SortHeaderButton button = this.buttons[i];
            button.hb.move(leftSideOffset + HB_W * (float)i + HB_W / 2.0f, button.hb.cY);
        }
    }

    @Override
    public void didChangeOrder(SortHeaderButton button, boolean isAscending) {
        Comparator<AbstractCard> order;
        button.setActive(true);
        if (button == this.buttons[0]) {
            if (isAscending) {
                this.masterDeckView.setSortOrder(null);
            } else {
                this.masterDeckView.setSortOrder(PURE_REVERSE);
            }
            return;
        }
        if (button == this.buttons[1]) {
            order = BY_TYPE;
        } else if (button == this.buttons[2]) {
            order = BY_COST;
        } else if (button == this.buttons[3]) {
            order = ALPHA;
        } else {
            return;
        }
        if (!isAscending) {
            order = order.reversed();
        }
        this.masterDeckView.setSortOrder(order);
    }

    @Override
    protected void updateScrollPositions() {
    }

    @Override
    public void render(SpriteBatch sb) {
        switch (AbstractDungeon.player.chosenClass) {
            case IRONCLAD: {
                sb.setColor(IRONCLAD_COLOR);
                break;
            }
            case THE_SILENT: {
                sb.setColor(SILENT_COLOR);
                break;
            }
            case DEFECT: {
                sb.setColor(DEFECT_COLOR);
                break;
            }
            default: {
                sb.setColor(BAR_COLOR);
            }
        }
        sb.draw(ImageMaster.COLOR_TAB_BAR, (float)Settings.WIDTH / 2.0f - 667.0f, this.scrollY - 51.0f, 667.0f, 51.0f, 1334.0f, 102.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1334, 102, false, false);
        super.render(sb);
    }

    public void updateScrollPosition(float y) {
        this.scrollY = y + 240.0f * Settings.scale;
        for (SortHeaderButton button : this.buttons) {
            button.updateScrollPosition(this.scrollY);
        }
    }
}

