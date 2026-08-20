/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.ParticleValuePanel;
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
import javax.swing.JPanel;

class PercentagePanel
extends ParticleValuePanel<ScaledNumericValue> {
    JButton expandButton;
    Chart chart;

    public PercentagePanel(FlameMain editor, ScaledNumericValue value, String chartTitle, String name, String description) {
        super(editor, name, description);
        this.initializeComponents(chartTitle);
        this.setValue(value);
    }

    @Override
    public void setValue(ScaledNumericValue value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        this.chart.setValues(((ScaledNumericValue)this.value).getTimeline(), ((ScaledNumericValue)this.value).getScaling());
    }

    private void initializeComponents(String chartTitle) {
        JPanel contentPanel = this.getContentPanel();
        this.chart = new Chart(chartTitle){

            @Override
            public void pointsChanged() {
                ((ScaledNumericValue)PercentagePanel.this.value).setTimeline(PercentagePanel.this.chart.getValuesX());
                ((ScaledNumericValue)PercentagePanel.this.value).setScaling(PercentagePanel.this.chart.getValuesY());
            }
        };
        this.chart.setPreferredSize(new Dimension(150, 62));
        contentPanel.add((Component)this.chart, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.expandButton = new JButton("+");
        this.expandButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        contentPanel.add((Component)this.expandButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.expandButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                PercentagePanel.this.chart.setExpanded(!PercentagePanel.this.chart.isExpanded());
                boolean expanded = PercentagePanel.this.chart.isExpanded();
                GridBagLayout layout = (GridBagLayout)PercentagePanel.this.getContentPanel().getLayout();
                GridBagConstraints chartConstraints = layout.getConstraints(PercentagePanel.this.chart);
                GridBagConstraints expandButtonConstraints = layout.getConstraints(PercentagePanel.this.expandButton);
                if (expanded) {
                    PercentagePanel.this.chart.setPreferredSize(new Dimension(150, 200));
                    PercentagePanel.this.expandButton.setText("-");
                    chartConstraints.weightx = 1.0;
                    expandButtonConstraints.weightx = 0.0;
                } else {
                    PercentagePanel.this.chart.setPreferredSize(new Dimension(150, 62));
                    PercentagePanel.this.expandButton.setText("+");
                    chartConstraints.weightx = 0.0;
                    expandButtonConstraints.weightx = 1.0;
                }
                layout.setConstraints(PercentagePanel.this.chart, chartConstraints);
                layout.setConstraints(PercentagePanel.this.expandButton, expandButtonConstraints);
                PercentagePanel.this.chart.revalidate();
            }
        });
    }
}

