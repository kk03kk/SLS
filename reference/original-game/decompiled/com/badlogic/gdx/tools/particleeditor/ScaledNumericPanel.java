/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.Chart;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.Slider;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class ScaledNumericPanel
extends EditorPanel {
    final ParticleEmitter.ScaledNumericValue value;
    Slider lowMinSlider;
    Slider lowMaxSlider;
    Slider highMinSlider;
    Slider highMaxSlider;
    JCheckBox relativeCheckBox;
    Chart chart;
    JPanel formPanel;
    JButton expandButton;
    JButton lowRangeButton;
    JButton highRangeButton;

    public ScaledNumericPanel(final ParticleEmitter.ScaledNumericValue value, String chartTitle, String name, String description) {
        super(value, name, description);
        this.value = value;
        this.initializeComponents(chartTitle);
        this.lowMinSlider.setValue(value.getLowMin());
        this.lowMaxSlider.setValue(value.getLowMax());
        this.highMinSlider.setValue(value.getHighMin());
        this.highMaxSlider.setValue(value.getHighMax());
        this.chart.setValues(value.getTimeline(), value.getScaling());
        this.relativeCheckBox.setSelected(value.isRelative());
        this.lowMinSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setLowMin(Float.valueOf(ScaledNumericPanel.this.lowMinSlider.getValue()).floatValue());
                if (!ScaledNumericPanel.this.lowMaxSlider.isVisible()) {
                    value.setLowMax(Float.valueOf(ScaledNumericPanel.this.lowMinSlider.getValue()).floatValue());
                }
            }
        });
        this.lowMaxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setLowMax(Float.valueOf(ScaledNumericPanel.this.lowMaxSlider.getValue()).floatValue());
            }
        });
        this.highMinSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setHighMin(Float.valueOf(ScaledNumericPanel.this.highMinSlider.getValue()).floatValue());
                if (!ScaledNumericPanel.this.highMaxSlider.isVisible()) {
                    value.setHighMax(Float.valueOf(ScaledNumericPanel.this.highMinSlider.getValue()).floatValue());
                }
            }
        });
        this.highMaxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                value.setHighMax(Float.valueOf(ScaledNumericPanel.this.highMaxSlider.getValue()).floatValue());
            }
        });
        this.relativeCheckBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                value.setRelative(ScaledNumericPanel.this.relativeCheckBox.isSelected());
            }
        });
        this.lowRangeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                boolean visible = !ScaledNumericPanel.this.lowMaxSlider.isVisible();
                ScaledNumericPanel.this.lowMaxSlider.setVisible(visible);
                ScaledNumericPanel.this.lowRangeButton.setText(visible ? "<" : ">");
                GridBagLayout layout = (GridBagLayout)ScaledNumericPanel.this.formPanel.getLayout();
                GridBagConstraints constraints = layout.getConstraints(ScaledNumericPanel.this.lowRangeButton);
                constraints.gridx = visible ? 5 : 4;
                layout.setConstraints(ScaledNumericPanel.this.lowRangeButton, constraints);
                Slider slider = visible ? ScaledNumericPanel.this.lowMaxSlider : ScaledNumericPanel.this.lowMinSlider;
                value.setLowMax(Float.valueOf(slider.getValue()).floatValue());
            }
        });
        this.highRangeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                boolean visible = !ScaledNumericPanel.this.highMaxSlider.isVisible();
                ScaledNumericPanel.this.highMaxSlider.setVisible(visible);
                ScaledNumericPanel.this.highRangeButton.setText(visible ? "<" : ">");
                GridBagLayout layout = (GridBagLayout)ScaledNumericPanel.this.formPanel.getLayout();
                GridBagConstraints constraints = layout.getConstraints(ScaledNumericPanel.this.highRangeButton);
                constraints.gridx = visible ? 5 : 4;
                layout.setConstraints(ScaledNumericPanel.this.highRangeButton, constraints);
                Slider slider = visible ? ScaledNumericPanel.this.highMaxSlider : ScaledNumericPanel.this.highMinSlider;
                value.setHighMax(Float.valueOf(slider.getValue()).floatValue());
            }
        });
        this.expandButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                ScaledNumericPanel.this.chart.setExpanded(!ScaledNumericPanel.this.chart.isExpanded());
                boolean expanded = ScaledNumericPanel.this.chart.isExpanded();
                GridBagLayout layout = (GridBagLayout)ScaledNumericPanel.this.getContentPanel().getLayout();
                GridBagConstraints chartConstraints = layout.getConstraints(ScaledNumericPanel.this.chart);
                GridBagConstraints expandButtonConstraints = layout.getConstraints(ScaledNumericPanel.this.expandButton);
                if (expanded) {
                    ScaledNumericPanel.this.chart.setPreferredSize(new Dimension(150, 200));
                    ScaledNumericPanel.this.expandButton.setText("-");
                    chartConstraints.weightx = 1.0;
                    expandButtonConstraints.weightx = 0.0;
                } else {
                    ScaledNumericPanel.this.chart.setPreferredSize(new Dimension(150, 30));
                    ScaledNumericPanel.this.expandButton.setText("+");
                    chartConstraints.weightx = 0.0;
                    expandButtonConstraints.weightx = 1.0;
                }
                layout.setConstraints(ScaledNumericPanel.this.chart, chartConstraints);
                layout.setConstraints(ScaledNumericPanel.this.expandButton, expandButtonConstraints);
                ScaledNumericPanel.this.relativeCheckBox.setVisible(!expanded);
                ScaledNumericPanel.this.formPanel.setVisible(!expanded);
                ScaledNumericPanel.this.chart.revalidate();
            }
        });
        if (value.getLowMin() == value.getLowMax()) {
            this.lowRangeButton.doClick(0);
        }
        if (value.getHighMin() == value.getHighMax()) {
            this.highRangeButton.doClick(0);
        }
    }

    public JPanel getFormPanel() {
        return this.formPanel;
    }

    private void initializeComponents(String chartTitle) {
        JPanel contentPanel = this.getContentPanel();
        this.formPanel = new JPanel(new GridBagLayout());
        contentPanel.add((Component)this.formPanel, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        JLabel label = new JLabel("High:");
        this.formPanel.add((Component)label, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.highMinSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        this.formPanel.add((Component)this.highMinSlider, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.highMaxSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        this.formPanel.add((Component)this.highMaxSlider, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.highRangeButton = new JButton("<");
        this.highRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.formPanel.add((Component)this.highRangeButton, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
        label = new JLabel("Low:");
        this.formPanel.add((Component)label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.lowMinSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        this.formPanel.add((Component)this.lowMinSlider, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.lowMaxSlider = new Slider(0.0f, -99999.0f, 99999.0f, 1.0f, -400.0f, 400.0f);
        this.formPanel.add((Component)this.lowMaxSlider, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.lowRangeButton = new JButton("<");
        this.lowRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.formPanel.add((Component)this.lowRangeButton, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
        this.chart = new Chart(chartTitle){

            @Override
            public void pointsChanged() {
                ScaledNumericPanel.this.value.setTimeline(ScaledNumericPanel.this.chart.getValuesX());
                ScaledNumericPanel.this.value.setScaling(ScaledNumericPanel.this.chart.getValuesY());
            }
        };
        contentPanel.add((Component)this.chart, new GridBagConstraints(6, 5, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.chart.setPreferredSize(new Dimension(150, 30));
        this.expandButton = new JButton("+");
        contentPanel.add((Component)this.expandButton, new GridBagConstraints(7, 5, 1, 1, 1.0, 0.0, 16, 0, new Insets(0, 5, 0, 0), 0, 0));
        this.expandButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        this.relativeCheckBox = new JCheckBox("Relative");
        contentPanel.add((Component)this.relativeCheckBox, new GridBagConstraints(7, 5, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
    }
}

