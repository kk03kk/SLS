/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.input;

import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.input.InputAction;

public class InputActionSet {
    public static Prefs prefs = SaveHelper.getPrefs("STSInputSettings");
    public static InputAction confirm;
    public static InputAction cancel;
    public static InputAction topPanel;
    public static InputAction proceed;
    public static InputAction settings;
    public static InputAction map;
    public static InputAction masterDeck;
    public static InputAction drawPile;
    public static InputAction discardPile;
    public static InputAction exhaustPile;
    public static InputAction endTurn;
    public static InputAction peek;
    public static InputAction up;
    public static InputAction down;
    public static InputAction left;
    public static InputAction right;
    public static InputAction releaseCard;
    public static InputAction selectCard_1;
    public static InputAction selectCard_2;
    public static InputAction selectCard_3;
    public static InputAction selectCard_4;
    public static InputAction selectCard_5;
    public static InputAction selectCard_6;
    public static InputAction selectCard_7;
    public static InputAction selectCard_8;
    public static InputAction selectCard_9;
    public static InputAction selectCard_10;
    public static InputAction[] selectCardActions;
    private static final String CONFIRM_KEY = "CONFIRM";
    private static final String CANCEL_KEY = "CANCEL";
    private static final String MAP_KEY = "MAP";
    private static final String DECK_KEY = "DECK";
    private static final String DRAW_PILE_KEY = "DRAW_PILE";
    private static final String DISCARD_PILE_KEY = "DISCARD_PILE";
    private static final String EXHAUST_PILE_KEY = "EXHAUST_PILE";
    private static final String END_TURN_KEY = "END_TURN";
    private static final String PEEK_KEY = "PEEK";
    private static final String UP_KEY = "UP";
    private static final String DOWN_KEY = "DOWN";
    private static final String LEFT_KEY = "LEFT";
    private static final String RIGHT_KEY = "RIGHT";
    private static final String DROP_CARD = "DROP_CARD";
    private static final String CARD_1_KEY = "CARD_1";
    private static final String CARD_2_KEY = "CARD_2";
    private static final String CARD_3_KEY = "CARD_3";
    private static final String CARD_4_KEY = "CARD_4";
    private static final String CARD_5_KEY = "CARD_5";
    private static final String CARD_6_KEY = "CARD_6";
    private static final String CARD_7_KEY = "CARD_7";
    private static final String CARD_8_KEY = "CARD_8";
    private static final String CARD_9_KEY = "CARD_9";
    private static final String CARD_10_KEY = "CARD_10";

    public static void load() {
        confirm = new InputAction(prefs.getInteger(CONFIRM_KEY, 66));
        cancel = new InputAction(prefs.getInteger(CANCEL_KEY, 131));
        map = new InputAction(prefs.getInteger(MAP_KEY, 41));
        masterDeck = new InputAction(prefs.getInteger(DECK_KEY, 32));
        drawPile = new InputAction(prefs.getInteger(DRAW_PILE_KEY, 29));
        discardPile = new InputAction(prefs.getInteger(DISCARD_PILE_KEY, 47));
        exhaustPile = new InputAction(prefs.getInteger(EXHAUST_PILE_KEY, 52));
        endTurn = new InputAction(prefs.getInteger(END_TURN_KEY, 33));
        peek = new InputAction(prefs.getInteger(PEEK_KEY, 62));
        up = new InputAction(prefs.getInteger(UP_KEY, 19));
        down = new InputAction(prefs.getInteger(DOWN_KEY, 20));
        left = new InputAction(prefs.getInteger(LEFT_KEY, 21));
        right = new InputAction(prefs.getInteger(RIGHT_KEY, 22));
        releaseCard = new InputAction(prefs.getInteger(DROP_CARD, 20));
        selectCard_1 = new InputAction(prefs.getInteger(CARD_1_KEY, 8));
        selectCard_2 = new InputAction(prefs.getInteger(CARD_2_KEY, 9));
        selectCard_3 = new InputAction(prefs.getInteger(CARD_3_KEY, 10));
        selectCard_4 = new InputAction(prefs.getInteger(CARD_4_KEY, 11));
        selectCard_5 = new InputAction(prefs.getInteger(CARD_5_KEY, 12));
        selectCard_6 = new InputAction(prefs.getInteger(CARD_6_KEY, 13));
        selectCard_7 = new InputAction(prefs.getInteger(CARD_7_KEY, 14));
        selectCard_8 = new InputAction(prefs.getInteger(CARD_8_KEY, 15));
        selectCard_9 = new InputAction(prefs.getInteger(CARD_9_KEY, 16));
        selectCard_10 = new InputAction(prefs.getInteger(CARD_10_KEY, 7));
        selectCardActions = new InputAction[]{selectCard_1, selectCard_2, selectCard_3, selectCard_4, selectCard_5, selectCard_6, selectCard_7, selectCard_8, selectCard_9, selectCard_10};
    }

    public static void save() {
        prefs.putInteger(CONFIRM_KEY, confirm.getKey());
        prefs.putInteger(CANCEL_KEY, cancel.getKey());
        prefs.putInteger(MAP_KEY, map.getKey());
        prefs.putInteger(DECK_KEY, masterDeck.getKey());
        prefs.putInteger(DRAW_PILE_KEY, drawPile.getKey());
        prefs.putInteger(DISCARD_PILE_KEY, discardPile.getKey());
        prefs.putInteger(EXHAUST_PILE_KEY, exhaustPile.getKey());
        prefs.putInteger(END_TURN_KEY, endTurn.getKey());
        prefs.putInteger(PEEK_KEY, peek.getKey());
        prefs.putInteger(UP_KEY, up.getKey());
        prefs.putInteger(DOWN_KEY, down.getKey());
        prefs.putInteger(LEFT_KEY, left.getKey());
        prefs.putInteger(RIGHT_KEY, right.getKey());
        prefs.putInteger(DROP_CARD, releaseCard.getKey());
        prefs.putInteger(CARD_1_KEY, selectCard_1.getKey());
        prefs.putInteger(CARD_2_KEY, selectCard_2.getKey());
        prefs.putInteger(CARD_3_KEY, selectCard_3.getKey());
        prefs.putInteger(CARD_4_KEY, selectCard_4.getKey());
        prefs.putInteger(CARD_5_KEY, selectCard_5.getKey());
        prefs.putInteger(CARD_6_KEY, selectCard_6.getKey());
        prefs.putInteger(CARD_7_KEY, selectCard_7.getKey());
        prefs.putInteger(CARD_8_KEY, selectCard_8.getKey());
        prefs.putInteger(CARD_9_KEY, selectCard_9.getKey());
        prefs.putInteger(CARD_10_KEY, selectCard_10.getKey());
        prefs.flush();
    }

    public static void resetToDefaults() {
        confirm.remap(66);
        cancel.remap(131);
        map.remap(41);
        masterDeck.remap(32);
        drawPile.remap(29);
        discardPile.remap(47);
        exhaustPile.remap(52);
        endTurn.remap(33);
        peek.remap(62);
        up.remap(19);
        down.remap(20);
        left.remap(21);
        right.remap(22);
        releaseCard.remap(20);
        selectCard_1.remap(8);
        selectCard_2.remap(9);
        selectCard_3.remap(10);
        selectCard_4.remap(11);
        selectCard_5.remap(12);
        selectCard_6.remap(13);
        selectCard_7.remap(14);
        selectCard_8.remap(15);
        selectCard_9.remap(16);
        selectCard_10.remap(7);
    }
}

