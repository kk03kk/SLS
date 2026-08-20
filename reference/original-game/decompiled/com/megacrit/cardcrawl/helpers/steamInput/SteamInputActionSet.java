/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.steamInput;

import com.codedisaster.steamworks.SteamController;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.steamInput.SteamInputAction;
import com.megacrit.cardcrawl.helpers.steamInput.SteamInputHelper;

public class SteamInputActionSet {
    public static SteamInputAction select;
    public static SteamInputAction cancel;
    public static SteamInputAction topPanel;
    public static SteamInputAction proceed;
    public static SteamInputAction peek;
    public static SteamInputAction pageLeftViewDeck;
    public static SteamInputAction pageRightViewExhaust;
    public static SteamInputAction map;
    public static SteamInputAction settings;
    public static SteamInputAction drawPile;
    public static SteamInputAction discardPile;
    public static SteamInputAction up;
    public static SteamInputAction down;
    public static SteamInputAction left;
    public static SteamInputAction right;
    public static SteamInputAction inspectUp;
    public static SteamInputAction inspectDown;
    public static SteamInputAction inspectLeft;
    public static SteamInputAction inspectRight;
    public static SteamInputAction altUp;
    public static SteamInputAction altDown;
    public static SteamInputAction altLeft;
    public static SteamInputAction altRight;
    public static SteamController controller;

    public static void load(SteamController setController) {
        controller = setController;
        controller.activateActionSet(SteamInputHelper.controllerHandles[0], controller.getActionSetHandle("AllControls"));
        select = SteamInputActionSet.SetAction("select", CInputActionSet.select);
        cancel = SteamInputActionSet.SetAction("cancel", CInputActionSet.cancel);
        topPanel = SteamInputActionSet.SetAction("top_panel", CInputActionSet.topPanel);
        proceed = SteamInputActionSet.SetAction("proceed", CInputActionSet.proceed);
        peek = SteamInputActionSet.SetAction("peek", CInputActionSet.peek);
        pageLeftViewDeck = SteamInputActionSet.SetAction("page_left", CInputActionSet.pageLeftViewDeck);
        pageRightViewExhaust = SteamInputActionSet.SetAction("page_right", CInputActionSet.pageRightViewExhaust);
        map = SteamInputActionSet.SetAction("map", CInputActionSet.map);
        settings = SteamInputActionSet.SetAction("settings", CInputActionSet.settings);
        up = SteamInputActionSet.SetAction("up", CInputActionSet.up);
        down = SteamInputActionSet.SetAction("down", CInputActionSet.down);
        left = SteamInputActionSet.SetAction("left", CInputActionSet.left);
        right = SteamInputActionSet.SetAction("right", CInputActionSet.right);
        inspectUp = SteamInputActionSet.SetAction("scroll_up", CInputActionSet.inspectUp);
        inspectDown = SteamInputActionSet.SetAction("scroll_down", CInputActionSet.inspectDown);
        inspectLeft = SteamInputActionSet.SetAction("scroll_left", CInputActionSet.inspectLeft);
        inspectRight = SteamInputActionSet.SetAction("scroll_right", CInputActionSet.inspectRight);
        altUp = SteamInputActionSet.SetAction("alt_up", CInputActionSet.altUp);
        altDown = SteamInputActionSet.SetAction("alt_down", CInputActionSet.altDown);
        altLeft = SteamInputActionSet.SetAction("alt_left", CInputActionSet.altLeft);
        altRight = SteamInputActionSet.SetAction("alt_right", CInputActionSet.altRight);
        drawPile = SteamInputActionSet.SetAction("draw_pile", CInputActionSet.drawPile);
        discardPile = SteamInputActionSet.SetAction("discard_pile", CInputActionSet.discardPile);
        SteamInputHelper.actions.clear();
        SteamInputHelper.actions.add(select);
        SteamInputHelper.actions.add(cancel);
        SteamInputHelper.actions.add(topPanel);
        SteamInputHelper.actions.add(proceed);
        SteamInputHelper.actions.add(peek);
        SteamInputHelper.actions.add(pageLeftViewDeck);
        SteamInputHelper.actions.add(pageRightViewExhaust);
        SteamInputHelper.actions.add(map);
        SteamInputHelper.actions.add(settings);
        SteamInputHelper.actions.add(up);
        SteamInputHelper.actions.add(down);
        SteamInputHelper.actions.add(left);
        SteamInputHelper.actions.add(right);
        SteamInputHelper.actions.add(inspectUp);
        SteamInputHelper.actions.add(inspectDown);
        SteamInputHelper.actions.add(inspectLeft);
        SteamInputHelper.actions.add(inspectRight);
        SteamInputHelper.actions.add(altUp);
        SteamInputHelper.actions.add(altDown);
        SteamInputHelper.actions.add(altLeft);
        SteamInputHelper.actions.add(altRight);
        SteamInputHelper.actions.add(drawPile);
        SteamInputHelper.actions.add(discardPile);
    }

    private static SteamInputAction SetAction(String name, CInputAction ref) {
        return new SteamInputAction(controller.getDigitalActionHandle(name), ref);
    }
}

