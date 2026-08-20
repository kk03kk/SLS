/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.trials;

import com.megacrit.cardcrawl.trials.AbstractTrial;
import java.util.ArrayList;

public class AnyColorDraftTrial
extends AbstractTrial {
    @Override
    public boolean keepsStarterCards() {
        return false;
    }

    @Override
    public ArrayList<String> dailyModIDs() {
        ArrayList<String> retVal = new ArrayList<String>();
        retVal.add("Diverse");
        retVal.add("Draft");
        return retVal;
    }
}

