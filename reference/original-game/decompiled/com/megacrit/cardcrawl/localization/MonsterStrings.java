/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class MonsterStrings {
    public String NAME;
    public String[] DIALOG;
    public String[] MOVES;

    public static MonsterStrings getMockMonsterString() {
        MonsterStrings retVal = new MonsterStrings();
        retVal.NAME = "[MISSING_NAME]";
        retVal.DIALOG = LocalizedStrings.createMockStringArray(5);
        retVal.MOVES = LocalizedStrings.createMockStringArray(5);
        return retVal;
    }
}

