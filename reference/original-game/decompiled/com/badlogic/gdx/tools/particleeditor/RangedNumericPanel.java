/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.Slider;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class RangedNumericPanel
extends EditorPanel {
    private final ParticleEmitter.RangedNumericValue value;
    Slider minSlider;
    Slider maxSlider;
    JButton rangeButton;
    JLabel label;

    public RangedNumericPanel(final ParticleEmitter.RangedNumericValue value, String name, String description) {
        super(value, name, description);
        this.value = value;
        this.initializeComponents();
        this.minSlider.setValue(value.getLowMin());
        this.maxSlider.setValue(value.getLowMax());
        this.minSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setLowMin(Float.valueOf(RangedNumericPanel.this.minSlider.getValue()).floatValue());
                if (!RangedNumericPanel.this.maxSlider.isVisible()) {
                    value.setLowMax(Float.valueOf(RangedNumericPanel.this.minSlider.getValue()).floatValue());
                }
            }
        });
        this.maxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setLowMax(Float.valueOf(RangedNumericPanel.this.maxSlider.getValue()).floatValue());
            }
        });
        this.rangeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                boolean visible = !RangedNumericPanel.this.maxSlider.isVisible();
                RangedNumericPanel.this.maxSlider.setVisible(visible);
                RangedNumericPanel.this.rangeButton.setText(visible ? "<" : ">");
                Slider slider = visible ? RangedNumericPanel.this.maxSlider : RangedNumericPanel.this.minSlider;
                value.setLowMax(Float.valueOf(slider.getValue()).floatValue());
            }
        });
        if (value.getLowMin() == value.getLowMax()) {
            this.rangeButton.doClick(0);
        }
    }

    private void initializeComponents() {
        JPanel contentPanel = this.getContentPanel();
        this.label = new JLabel("Value:");
        contentPanel.add((Component)this.label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.minSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        contentPanel.add((Component)this.minSlider, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.maxSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        contentPanel.add((Component)this.maxSlider, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.rangeButton = new JButton("<");
        this.rangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        contentPanel.add((Component)this.rangeButton, new GridBagConstraints(5, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
    }
}

