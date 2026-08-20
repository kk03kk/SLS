/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.ScaledNumericPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AngularVelocityPanel
extends EditorPanel<DynamicsModifier.Angular> {
    JCheckBox isGlobalCheckBox;
    ScaledNumericPanel thetaPanel;
    ScaledNumericPanel phiPanel;
    ScaledNumericPanel magnitudePanel;

    public AngularVelocityPanel(FlameMain editor, DynamicsModifier.Angular aValue, String charTitle, String name, String description) {
        super(editor, name, description);
        this.initializeComponents(aValue, charTitle);
        this.setValue((DynamicsModifier.Angular)this.value);
    }

    @Override
    public void setValue(DynamicsModifier.Angular value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        AngularVelocityPanel.setValue(this.isGlobalCheckBox, ((DynamicsModifier.Angular)this.value).isGlobal);
        this.magnitudePanel.setValue(((DynamicsModifier.Angular)this.value).strengthValue);
        this.thetaPanel.setValue(((DynamicsModifier.Angular)this.value).thetaValue);
        this.phiPanel.setValue(((DynamicsModifier.Angular)this.value).phiValue);
    }

    private void initializeComponents(DynamicsModifier.Angular aValue, String charTitle) {
        JPanel contentPanel = this.getContentPanel();
        JPanel panel = new JPanel();
        panel.add((Component)new JLabel("Global"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.isGlobalCheckBox = new JCheckBox();
        panel.add((Component)this.isGlobalCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        contentPanel.add((Component)panel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.magnitudePanel = new ScaledNumericPanel(this.editor, aValue == null ? null : aValue.strengthValue, charTitle, "Strength", "In world units per second.", true);
        contentPanel.add((Component)this.magnitudePanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.phiPanel = new ScaledNumericPanel(this.editor, aValue == null ? null : aValue.phiValue, charTitle, "Azimuth", "Rotation starting on Y", true);
        contentPanel.add((Component)this.phiPanel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.thetaPanel = new ScaledNumericPanel(this.editor, aValue == null ? null : aValue.thetaValue, charTitle, "Polar angle", "around Y axis on XZ plane", true);
        contentPanel.add((Component)this.thetaPanel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 6), 0, 0));
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension());
        contentPanel.add((Component)spacer, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.magnitudePanel.setIsAlwayShown(true);
        this.phiPanel.setIsAlwayShown(true);
        this.thetaPanel.setIsAlwayShown(true);
        this.isGlobalCheckBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ((DynamicsModifier.Angular)AngularVelocityPanel.this.value).isGlobal = AngularVelocityPanel.this.isGlobalCheckBox.isSelected();
            }
        });
    }

    public ScaledNumericPanel getThetaPanel() {
        return this.thetaPanel;
    }

    public ScaledNumericPanel getPhiPanel() {
        return this.phiPanel;
    }

    public ScaledNumericPanel getMagnitudePanel() {
        return this.magnitudePanel;
    }
}

