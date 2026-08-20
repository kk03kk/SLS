/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.ParticleValuePanel;
import com.badlogic.gdx.tools.flame.Slider;
import com.badlogic.gdx.tools.particleeditor.Chart;
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
extends ParticleValuePanel<ScaledNumericValue> {
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

    public ScaledNumericPanel(FlameMain editor, ScaledNumericValue value, String chartTitle, String name, String description) {
        this(editor, value, chartTitle, name, description, true);
    }

    public ScaledNumericPanel(FlameMain editor, ScaledNumericValue value, String chartTitle, String name, String description, boolean isAlwaysActive) {
        super(editor, name, description, isAlwaysActive);
        this.initializeComponents(chartTitle);
        this.setValue(value);
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
        this.highMinSlider = new Slider(0.0f, -999999.0f, 999999.0f, 1.0f);
        this.formPanel.add((Component)this.highMinSlider, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.highMaxSlider = new Slider(0.0f, -999999.0f, 999999.0f, 1.0f);
        this.formPanel.add((Component)this.highMaxSlider, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.highRangeButton = new JButton("<");
        this.highRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.formPanel.add((Component)this.highRangeButton, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
        label = new JLabel("Low:");
        this.formPanel.add((Component)label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.lowMinSlider = new Slider(0.0f, -999999.0f, 999999.0f, 1.0f);
        this.formPanel.add((Component)this.lowMinSlider, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.lowMaxSlider = new Slider(0.0f, -999999.0f, 999999.0f, 1.0f);
        this.formPanel.add((Component)this.lowMaxSlider, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.lowRangeButton = new JButton("<");
        this.lowRangeButton.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.formPanel.add((Component)this.lowRangeButton, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 1, 0, 0), 0, 0));
        this.chart = new Chart(chartTitle){

            @Override
            public void pointsChanged() {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setTimeline(ScaledNumericPanel.this.chart.getValuesX());
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setScaling(ScaledNumericPanel.this.chart.getValuesY());
            }
        };
        contentPanel.add((Component)this.chart, new GridBagConstraints(6, 5, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.chart.setPreferredSize(new Dimension(150, 30));
        this.expandButton = new JButton("+");
        contentPanel.add((Component)this.expandButton, new GridBagConstraints(7, 5, 1, 1, 1.0, 0.0, 16, 0, new Insets(0, 5, 0, 0), 0, 0));
        this.expandButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        this.relativeCheckBox = new JCheckBox("Relative");
        contentPanel.add((Component)this.relativeCheckBox, new GridBagConstraints(7, 5, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.lowMinSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setLowMin(ScaledNumericPanel.this.lowMinSlider.getValue());
                if (!ScaledNumericPanel.this.lowMaxSlider.isVisible()) {
                    ((ScaledNumericValue)ScaledNumericPanel.this.value).setLowMax(ScaledNumericPanel.this.lowMinSlider.getValue());
                }
            }
        });
        this.lowMaxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setLowMax(ScaledNumericPanel.this.lowMaxSlider.getValue());
            }
        });
        this.highMinSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setHighMin(ScaledNumericPanel.this.highMinSlider.getValue());
                if (!ScaledNumericPanel.this.highMaxSlider.isVisible()) {
                    ((ScaledNumericValue)ScaledNumericPanel.this.value).setHighMax(ScaledNumericPanel.this.highMinSlider.getValue());
                }
            }
        });
        this.highMaxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setHighMax(ScaledNumericPanel.this.highMaxSlider.getValue());
            }
        });
        this.relativeCheckBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setRelative(ScaledNumericPanel.this.relativeCheckBox.isSelected());
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
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setLowMax(slider.getValue());
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
                ((ScaledNumericValue)ScaledNumericPanel.this.value).setHighMax(slider.getValue());
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
    }

    @Override
    public void setValue(ScaledNumericValue value) {
        super.setValue(value);
        if (this.value == null) {
            return;
        }
        ScaledNumericPanel.setValue(this.lowMinSlider, ((ScaledNumericValue)this.value).getLowMin());
        ScaledNumericPanel.setValue(this.lowMaxSlider, ((ScaledNumericValue)this.value).getLowMax());
        ScaledNumericPanel.setValue(this.highMinSlider, ((ScaledNumericValue)this.value).getHighMin());
        ScaledNumericPanel.setValue(this.highMaxSlider, ((ScaledNumericValue)this.value).getHighMax());
        this.chart.setValues(((ScaledNumericValue)this.value).getTimeline(), ((ScaledNumericValue)this.value).getScaling());
        ScaledNumericPanel.setValue(this.relativeCheckBox, ((ScaledNumericValue)this.value).isRelative());
        if (((ScaledNumericValue)this.value).getLowMin() == ((ScaledNumericValue)this.value).getLowMax() && this.lowMaxSlider.isVisible() || ((ScaledNumericValue)this.value).getLowMin() != ((ScaledNumericValue)this.value).getLowMax() && !this.lowMaxSlider.isVisible()) {
            this.lowRangeButton.doClick(0);
        }
        if (((ScaledNumericValue)this.value).getHighMin() == ((ScaledNumericValue)this.value).getHighMax() && this.highMaxSlider.isVisible() || ((ScaledNumericValue)this.value).getHighMin() != ((ScaledNumericValue)this.value).getHighMax() && !this.highMaxSlider.isVisible()) {
            this.highRangeButton.doClick(0);
        }
    }

    public Chart getChart() {
        return this.chart;
    }
}

