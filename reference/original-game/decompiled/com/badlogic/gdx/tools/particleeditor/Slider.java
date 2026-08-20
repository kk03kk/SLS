/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

class Slider
extends JPanel {
    private JSpinner spinner;

    public Slider(float initialValue, float min, float max, float stepSize, float sliderMin, float sliderMax) {
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

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(2);
                frame.setSize(480, 320);
                frame.setLocationRelativeTo(null);
                JPanel panel = new JPanel();
                frame.getContentPane().add(panel);
                panel.add(new Slider(200.0f, 100.0f, 500.0f, 0.1f, 150.0f, 300.0f));
                frame.setVisible(true);
            }
        });
    }
}

