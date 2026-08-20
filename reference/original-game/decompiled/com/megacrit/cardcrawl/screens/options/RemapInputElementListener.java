/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.megacrit.cardcrawl.screens.options.RemapInputElement;

public interface RemapInputElementListener {
    public void didStartRemapping(RemapInputElement var1);

    public boolean willRemap(RemapInputElement var1, int var2, int var3);

    public boolean willRemapController(RemapInputElement var1, int var2, int var3);
}

