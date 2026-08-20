/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class NumericPanel
extends EditorPanel {
    private final ParticleEmitter.NumericValue value;
    JSpinner valueSpinner;

    public NumericPanel(final ParticleEmitter.NumericValue value, String name, String description) {
        super(value, name, description);
        this.value = value;
        this.initializeComponents();
        this.valueSpinner.setValue(Float.valueOf(value.getValue()));
        this.valueSpinner.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setValue(((Float)NumericPanel.this.valueSpinner.getValue()).floatValue());
            }
        });
    }

    private void initializeComponents() {
        JPanel contentPanel = this.getContentPanel();
        JLabel label = new JLabel("Value:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.valueSpinner = new JSpinner(new SpinnerNumberModel(new Float(0.0f), new Float(-99999.0f), new Float(99999.0f), new Float(0.1f)));
        contentPanel.add((Component)this.valueSpinner, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
    }
}

