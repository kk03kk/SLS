/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.controller;

import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;

public class CInputActionSet {
    public static Prefs prefs = SaveHelper.getPrefs("STSInputSettings_Controller");
    public static CInputAction select;
    public static CInputAction cancel;
    public static CInputAction topPanel;
    public static CInputAction proceed;
    public static CInputAction peek;
    public static CInputAction pageLeftViewDeck;
    public static CInputAction pageRightViewExhaust;
    public static CInputAction map;
    public static CInputAction settings;
    public static CInputAction drawPile;
    public static CInputAction discardPile;
    public static CInputAction up;
    public static CInputAction down;
    public static CInputAction left;
    public static CInputAction right;
    public static CInputAction inspectUp;
    public static CInputAction inspectDown;
    public static CInputAction inspectLeft;
    public static CInputAction inspectRight;
    public static CInputAction altUp;
    public static CInputAction altDown;
    public static CInputAction altLeft;
    public static CInputAction altRight;
    private static final String LS_Y_POSITIVE = "LS_Y_POSITIVE";
    private static final String LS_Y_NEGATIVE = "LS_Y_NEGATIVE";
    private static final String LS_X_POSITIVE = "LS_X_POSITIVE";
    private static final String LS_X_NEGATIVE = "LS_X_NEGATIVE";
    private static final String RS_Y_POSITIVE = "RS_Y_POSITIVE";
    private static final String RS_Y_NEGATIVE = "RS_Y_NEGATIVE";
    private static final String RS_X_POSITIVE = "RS_X_POSITIVE";
    private static final String RS_X_NEGATIVE = "RS_X_NEGATIVE";
    private static final String D_NORTH = "D_NORTH";
    private static final String D_SOUTH = "D_SOUTH";
    private static final String D_WEST = "D_WEST";
    private static final String D_EAST = "D_EAST";
    private static final String SELECT_KEY = "SELECT";
    private static final String CANCEL_KEY = "CANCEL";
    private static final String TOP_PANEL_KEY = "VIEW";
    private static final String PROCEED_KEY = "PROCEED";
    private static final String PEEK_KEY = "PEEK";
    private static final String PAGE_LEFT_KEY = "PAGE_LEFT_KEY";
    private static final String PAGE_RIGHT_KEY = "PAGE_RIGHT_KEY";
    private static final String MAP_KEY = "MAP";
    private static final String SETTINGS_KEY = "SETTINGS";
    private static final String DRAW_PILE_KEY = "DRAW_PILE";
    private static final String DISCARD_PILE_KEY = "DISCARD_PILE";

    public static void load() {
        select = new CInputAction(prefs.getInteger(SELECT_KEY, 0));
        cancel = new CInputAction(prefs.getInteger(CANCEL_KEY, 1));
        topPanel = new CInputAction(prefs.getInteger(TOP_PANEL_KEY, 2));
        proceed = new CInputAction(prefs.getInteger(PROCEED_KEY, 3));
        peek = new CInputAction(prefs.getInteger(PEEK_KEY, 8));
        pageLeftViewDeck = new CInputAction(prefs.getInteger(PAGE_LEFT_KEY, 4));
        pageRightViewExhaust = new CInputAction(prefs.getInteger(PAGE_RIGHT_KEY, 5));
        map = new CInputAction(prefs.getInteger(MAP_KEY, 6));
        settings = new CInputAction(prefs.getInteger(SETTINGS_KEY, 7));
        up = new CInputAction(prefs.getInteger(LS_Y_POSITIVE, -1000));
        down = new CInputAction(prefs.getInteger(LS_Y_NEGATIVE, 1000));
        left = new CInputAction(prefs.getInteger(LS_X_NEGATIVE, -1001));
        right = new CInputAction(prefs.getInteger(LS_X_POSITIVE, 1001));
        inspectUp = new CInputAction(prefs.getInteger(RS_Y_POSITIVE, -1002));
        inspectDown = new CInputAction(prefs.getInteger(RS_Y_NEGATIVE, 1002));
        inspectLeft = new CInputAction(prefs.getInteger(RS_X_POSITIVE, -1003));
        inspectRight = new CInputAction(prefs.getInteger(RS_X_NEGATIVE, 1003));
        altUp = new CInputAction(prefs.getInteger(D_NORTH, -2000));
        altDown = new CInputAction(prefs.getInteger(D_SOUTH, 2000));
        altLeft = new CInputAction(prefs.getInteger(D_WEST, -2001));
        altRight = new CInputAction(prefs.getInteger(D_EAST, 2001));
        drawPile = new CInputAction(prefs.getInteger(DRAW_PILE_KEY, 1004));
        discardPile = new CInputAction(prefs.getInteger(DISCARD_PILE_KEY, -1004));
        CInputHelper.actions.clear();
        CInputHelper.actions.add(select);
        CInputHelper.actions.add(cancel);
        CInputHelper.actions.add(topPanel);
        CInputHelper.actions.add(proceed);
        CInputHelper.actions.add(peek);
        CInputHelper.actions.add(pageLeftViewDeck);
        CInputHelper.actions.add(pageRightViewExhaust);
        CInputHelper.actions.add(map);
        CInputHelper.actions.add(settings);
        CInputHelper.actions.add(up);
        CInputHelper.actions.add(down);
        CInputHelper.actions.add(left);
        CInputHelper.actions.add(right);
        CInputHelper.actions.add(inspectUp);
        CInputHelper.actions.add(inspectDown);
        CInputHelper.actions.add(inspectLeft);
        CInputHelper.actions.add(inspectRight);
        CInputHelper.actions.add(altUp);
        CInputHelper.actions.add(altDown);
        CInputHelper.actions.add(altLeft);
        CInputHelper.actions.add(altRight);
        CInputHelper.actions.add(drawPile);
        CInputHelper.actions.add(discardPile);
    }

    public static void save() {
        prefs.putInteger(SELECT_KEY, select.getKey());
        prefs.putInteger(CANCEL_KEY, cancel.getKey());
        prefs.putInteger(TOP_PANEL_KEY, topPanel.getKey());
        prefs.putInteger(PROCEED_KEY, proceed.getKey());
        prefs.putInteger(PEEK_KEY, peek.getKey());
        prefs.putInteger(PAGE_LEFT_KEY, pageLeftViewDeck.getKey());
        prefs.putInteger(PAGE_RIGHT_KEY, pageRightViewExhaust.getKey());
        prefs.putInteger(MAP_KEY, map.getKey());
        prefs.putInteger(SETTINGS_KEY, settings.getKey());
        prefs.putInteger(LS_Y_POSITIVE, up.getKey());
        prefs.putInteger(LS_Y_NEGATIVE, down.getKey());
        prefs.putInteger(LS_X_NEGATIVE, left.getKey());
        prefs.putInteger(LS_X_POSITIVE, right.getKey());
        prefs.putInteger(RS_Y_POSITIVE, inspectUp.getKey());
        prefs.putInteger(RS_Y_NEGATIVE, inspectDown.getKey());
        prefs.putInteger(RS_X_POSITIVE, inspectLeft.getKey());
        prefs.putInteger(RS_X_NEGATIVE, inspectRight.getKey());
        prefs.putInteger(DRAW_PILE_KEY, drawPile.getKey());
        prefs.putInteger(DISCARD_PILE_KEY, discardPile.getKey());
        prefs.flush();
    }

    public static void resetToDefaults() {
        select.remap(0);
        cancel.remap(1);
        topPanel.remap(2);
        proceed.remap(3);
        peek.remap(8);
        pageLeftViewDeck.remap(4);
        pageRightViewExhaust.remap(5);
        map.remap(6);
        settings.remap(7);
        up.remap(-1000);
        down.remap(1000);
        left.remap(-1001);
        right.remap(1001);
        inspectUp.remap(1002);
        inspectDown.remap(-1002);
        inspectLeft.remap(-1003);
        inspectRight.remap(1003);
        altUp.remap(-2000);
        altDown.remap(2000);
        altLeft.remap(-2001);
        altRight.remap(2001);
        drawPile.remap(1004);
        discardPile.remap(-1004);
    }
}

