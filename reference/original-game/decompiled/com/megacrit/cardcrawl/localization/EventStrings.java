/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class EventStrings {
    public String NAME;
    public String[] DESCRIPTIONS;
    public String[] OPTIONS;

    public static EventStrings getMockEventString() {
        EventStrings retVal = new EventStrings();
        retVal.NAME = "[MISSING_NAME]";
        retVal.DESCRIPTIONS = LocalizedStrings.createMockStringArray(12);
        retVal.OPTIONS = LocalizedStrings.createMockStringArray(12);
        return retVal;
    }
}

