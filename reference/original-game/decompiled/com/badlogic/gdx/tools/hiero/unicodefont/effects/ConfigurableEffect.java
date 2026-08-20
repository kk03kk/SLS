/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont.effects;

import com.badlogic.gdx.tools.hiero.unicodefont.effects.Effect;
import java.util.List;

public interface ConfigurableEffect
extends Effect {
    public List getValues();

    public void setValues(List var1);

    public static interface Value {
        public String getName();

        public void setString(String var1);

        public String getString();

        public Object getObject();

        public void showDialog();
    }
}

