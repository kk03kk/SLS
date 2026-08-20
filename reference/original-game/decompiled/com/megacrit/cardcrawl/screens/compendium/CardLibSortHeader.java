/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.compendium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButton;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButtonListener;

public class CardLibSortHeader
implements SortHeaderButtonListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CardLibSortHeader");
    public static final String[] TEXT = CardLibSortHeader.uiStrings.TEXT;
    public CardGroup group;
    public boolean justSorted = false;
    public static final float START_X = 430.0f * Settings.xScale;
    public static final float SPACE_X = 226.0f * Settings.xScale;
    private SortHeaderButton rarityButton;
    private SortHeaderButton typeButton;
    private SortHeaderButton costButton;
    private SortHeaderButton nameButton;
    public SortHeaderButton[] buttons;
    public int selectionIndex = -1;
    private static Texture img;
    private Color selectionColor = new Color(1.0f, 0.95f, 0.5f, 0.0f);

    public CardLibSortHeader(CardGroup group) {
        if (img == null) {
            img = ImageMaster.loadImage("images/ui/cardlibrary/selectBox.png");
        }
        this.group = group;
        float xPosition = START_X;
        this.rarityButton = new SortHeaderButton(TEXT[0], xPosition, 0.0f, this);
        this.typeButton = new SortHeaderButton(TEXT[1], xPosition += SPACE_X, 0.0f, this);
        this.costButton = new SortHeaderButton(TEXT[3], xPosition += SPACE_X, 0.0f, this);
        if (!Settings.removeAtoZSort) {
            this.nameButton = new SortHeaderButton(TEXT[2], xPosition += SPACE_X, 0.0f, this);
            this.buttons = new SortHeaderButton[]{this.rarityButton, this.typeButton, this.costButton, this.nameButton};
        } else {
            this.buttons = new SortHeaderButton[]{this.rarityButton, this.typeButton, this.costButton};
        }
    }

    public void setGroup(CardGroup group) {
        this.group = group;
        group.sortAlphabetically(true);
        group.sortByRarity(true);
        group.sortByStatus(true);
        for (SortHeaderButton button : this.buttons) {
            button.reset();
        }
    }

    public void update() {
        for (SortHeaderButton button : this.buttons) {
            button.update();
        }
    }

    public Hitbox updateControllerInput() {
        for (SortHeaderButton button : this.buttons) {
            if (!button.hb.hovered) continue;
            return button.hb;
        }
        return null;
    }

    public int getHoveredIndex() {
        int retVal = 0;
        for (SortHeaderButton button : this.buttons) {
            if (button.hb.hovered) {
                return retVal;
            }
            ++retVal;
        }
        return 0;
    }

    public void clearActiveButtons() {
        for (SortHeaderButton button : this.buttons) {
            button.setActive(false);
        }
    }

    @Override
    public void didChangeOrder(SortHeaderButton button, boolean isAscending) {
        if (button == this.rarityButton) {
            this.group.sortByRarity(isAscending);
        } else if (button == this.typeButton) {
            this.group.sortByType(isAscending);
        } else if (button == this.nameButton) {
            this.group.sortAlphabetically(isAscending);
        } else if (button == this.costButton) {
            this.group.sortByCost(isAscending);
        } else {
            return;
        }
        this.group.sortByStatus(false);
        this.justSorted = true;
        button.setActive(true);
    }

    public void render(SpriteBatch sb) {
        this.updateScrollPositions();
        this.renderButtons(sb);
        this.renderSelection(sb);
    }

    protected void updateScrollPositions() {
        float scrolledY = this.group.getBottomCard().current_y + 230.0f * Settings.yScale;
        for (SortHeaderButton button : this.buttons) {
            button.updateScrollPosition(scrolledY);
        }
    }

    protected void renderButtons(SpriteBatch sb) {
        for (SortHeaderButton b : this.buttons) {
            b.render(sb);
        }
    }

    protected void renderSelection(SpriteBatch sb) {
        for (int i = 0; i < this.buttons.length; ++i) {
            if (i != this.selectionIndex) continue;
            this.selectionColor.a = 0.7f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f;
            sb.setColor(this.selectionColor);
            float doop = 1.0f + (1.0f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L)) / 50.0f;
            sb.draw(img, this.buttons[this.selectionIndex].hb.cX - 80.0f - this.buttons[this.selectionIndex].textWidth / 2.0f * Settings.scale, this.buttons[this.selectionIndex].hb.cY - 43.0f, 100.0f, 43.0f, 160.0f + this.buttons[this.selectionIndex].textWidth, 86.0f, Settings.scale * doop, Settings.scale * doop, 0.0f, 0, 0, 200, 86, false, false);
        }
    }
}

