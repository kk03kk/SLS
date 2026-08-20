/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class NewSlider
extends JPanel {
    private static final int KNOB_WIDTH = 10;
    float value;
    float min;
    float max;
    float stepSize;
    float sliderMin;
    float sliderMax;
    ChangeListener listener;
    int border = 2;
    Color bgColor = new Color(0.6f, 0.6f, 0.6f);
    Color knobColor = Color.lightGray;

    public NewSlider(float initialValue, float min, float max, float stepSize, final float sliderMin, final float sliderMax) {
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.value = Math.max(min, Math.min(max, initialValue));
        this.setLayout(new GridBagLayout());
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent event) {
                float width = NewSlider.this.getWidth() - 10 - NewSlider.this.border * 2;
                float mouseX = event.getX() - 5 - NewSlider.this.border;
                NewSlider.this.setValue(sliderMin + (sliderMax - sliderMin) * Math.max(0.0f, Math.min(width, mouseX)) / width);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                NewSlider.this.repaint();
            }
        });
        this.addMouseMotionListener(new MouseMotionListener(){

            @Override
            public void mouseDragged(MouseEvent event) {
                float width = NewSlider.this.getWidth() - 10 - NewSlider.this.border * 2;
                float mouseX = event.getX() - 5 - NewSlider.this.border;
                NewSlider.this.setValue(sliderMin + (sliderMax - sliderMin) * Math.max(0.0f, Math.min(width, mouseX)) / width);
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                int mouseX = event.getX();
                int mouseY = event.getY();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics;
        int width = this.getWidth();
        int height = this.getHeight();
        g.setColor(this.bgColor);
        g.fillRect(this.border, this.border, width - this.border * 2, height - this.border * 2);
        int maxKnobX = width - this.border - 10;
        int knobX = (int)((float)(width - this.border * 2 - 10) * (this.value - this.sliderMin) / (this.sliderMax - this.sliderMin)) + this.border;
        g.setColor(this.knobColor);
        g.fillRect(Math.max(this.border, Math.min(maxKnobX, knobX)), 0, 10, height);
        float displayValue = (float)((int)(this.value * 10.0f)) / 10.0f;
        String label = displayValue == (float)((int)displayValue) ? String.valueOf((int)displayValue) : String.valueOf(displayValue);
        FontMetrics metrics = g.getFontMetrics();
        int labelWidth = metrics.stringWidth(label);
        g.setColor(Color.white);
        g.drawString(label, width / 2 - labelWidth / 2, height / 2 + metrics.getAscent() / 2);
    }

    public void setValue(float value) {
        this.value = (float)((int)(Math.max(this.min, Math.min(this.max, value)) / this.stepSize)) * this.stepSize;
        this.repaint();
        if (this.listener != null) {
            this.listener.stateChanged(new ChangeEvent(this));
        }
    }

    public float getValue() {
        return this.value;
    }

    public void addChangeListener(ChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = 150;
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
                panel.add(new NewSlider(200.0f, 100.0f, 500.0f, 0.1f, 150.0f, 300.0f));
                frame.setVisible(true);
            }
        });
    }
}

