/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.localization;

import com.megacrit.cardcrawl.localization.LocalizedStrings;

public class TutorialStrings {
    public String[] TEXT;
    public String[] LABEL;

    public static TutorialStrings getMockTutorialString() {
        TutorialStrings retVal = new TutorialStrings();
        retVal.TEXT = LocalizedStrings.createMockStringArray(25);
        retVal.LABEL = LocalizedStrings.createMockStringArray(8);
        return retVal;
    }
}

