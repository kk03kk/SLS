/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

public class Slider
extends JPanel {
    public JSpinner spinner;

    public Slider(float initialValue, float min, float max, float stepSize) {
        this.spinner = new JSpinner(new SpinnerNumberModel(initialValue, min, max, stepSize));
        this.setLayout(new BorderLayout());
        this.add(this.spinner);
    }

    public void setValue(float value) {
        this.spinner.setValue(value);
    }

    public float getValue() {
        return ((Double)this.spinner.getValue()).floatValue();
    }

    public void addChangeListener(ChangeListener listener) {
        this.spinner.addChangeListener(listener);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = 75;
        size.height = 26;
        return size;
    }
}

