/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

public class CustomCardLayout
extends CardLayout {
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Component component = (Component)this.getCurrentCard(parent);
        return component != null ? component.getPreferredSize() : super.preferredLayoutSize(parent);
    }

    public <K> K getCurrentCard(Container container) {
        Component[] c = container.getComponents();
        int j = c.length;
        for (int i = 0; i < j; ++i) {
            if (!c[i].isVisible()) continue;
            return (K)c[i];
        }
        return null;
    }
}

