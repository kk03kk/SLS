/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.values.GradientColorValue;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.ParticleValuePanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class GradientPanel
extends ParticleValuePanel<GradientColorValue> {
    private GradientEditor gradientEditor;
    ColorSlider saturationSlider;
    ColorSlider lightnessSlider;
    JPanel colorPanel;
    private ColorSlider hueSlider;

    public GradientPanel(FlameMain editor, GradientColorValue value, String name, String description, boolean hideGradientEditor) {
        super(editor, name, description);
        this.setValue(value);
        if (hideGradientEditor) {
            this.gradientEditor.setVisible(false);
        }
        this.gradientEditor.percentages.clear();
        for (float percent : value.getTimeline()) {
            this.gradientEditor.percentages.add(Float.valueOf(percent));
        }
        this.gradientEditor.colors.clear();
        float[] colors = value.getColors();
        int i = 0;
        while (i < colors.length) {
            float r = colors[i++];
            float g = colors[i++];
            float b = colors[i++];
            this.gradientEditor.colors.add(new Color(r, g, b));
        }
        if (this.gradientEditor.colors.isEmpty() || this.gradientEditor.percentages.isEmpty()) {
            this.gradientEditor.percentages.clear();
            this.gradientEditor.percentages.add(Float.valueOf(0.0f));
            this.gradientEditor.percentages.add(Float.valueOf(1.0f));
            this.gradientEditor.colors.clear();
            this.gradientEditor.colors.add(Color.white);
        }
        this.setColor(this.gradientEditor.colors.get(0));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = 10;
        return size;
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        JPanel contentPanel = this.getContentPanel();
        this.gradientEditor = new GradientEditor(){

            @Override
            public void handleSelected(Color color) {
                GradientPanel.this.setColor(color);
            }
        };
        contentPanel.add((Component)this.gradientEditor, new GridBagConstraints(0, 1, 3, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 10));
        this.hueSlider = new ColorSlider(new Color[]{Color.red, Color.yellow, Color.green, Color.cyan, Color.blue, Color.magenta, Color.red}){

            @Override
            protected void colorPicked() {
                GradientPanel.this.saturationSlider.setColors(new Color[]{new Color(Color.HSBtoRGB(this.getPercentage(), 1.0f, 1.0f)), Color.white});
                GradientPanel.this.updateColor();
            }
        };
        contentPanel.add((Component)this.hueSlider, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        this.saturationSlider = new ColorSlider(new Color[]{Color.red, Color.white}){

            @Override
            protected void colorPicked() {
                GradientPanel.this.updateColor();
            }
        };
        contentPanel.add((Component)this.saturationSlider, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 6), 0, 0));
        this.lightnessSlider = new ColorSlider(new Color[0]){

            @Override
            protected void colorPicked() {
                GradientPanel.this.updateColor();
            }
        };
        contentPanel.add((Component)this.lightnessSlider, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.colorPanel = new JPanel(){

            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = 52;
                return size;
            }
        };
        contentPanel.add((Component)this.colorPanel, new GridBagConstraints(0, 2, 1, 2, 0.0, 0.0, 10, 1, new Insets(3, 0, 0, 6), 0, 0));
        this.colorPanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                Color color = JColorChooser.showDialog(GradientPanel.this.colorPanel, "Set Color", GradientPanel.this.colorPanel.getBackground());
                if (color != null) {
                    GradientPanel.this.setColor(color);
                }
            }
        });
        this.colorPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
    }

    public void setColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hueSlider.setPercentage(hsb[0]);
        this.saturationSlider.setPercentage(1.0f - hsb[1]);
        this.lightnessSlider.setPercentage(1.0f - hsb[2]);
        this.colorPanel.setBackground(color);
    }

    void updateColor() {
        Color color = new Color(Color.HSBtoRGB(this.hueSlider.getPercentage(), 1.0f - this.saturationSlider.getPercentage(), 1.0f));
        this.lightnessSlider.setColors(new Color[]{color, Color.black});
        color = new Color(Color.HSBtoRGB(this.hueSlider.getPercentage(), 1.0f - this.saturationSlider.getPercentage(), 1.0f - this.lightnessSlider.getPercentage()));
        this.colorPanel.setBackground(color);
        this.gradientEditor.setColor(color);
        float[] colors = new float[this.gradientEditor.colors.size() * 3];
        int i = 0;
        for (Color c : this.gradientEditor.colors) {
            colors[i++] = (float)c.getRed() / 255.0f;
            colors[i++] = (float)c.getGreen() / 255.0f;
            colors[i++] = (float)c.getBlue() / 255.0f;
        }
        float[] percentages = new float[this.gradientEditor.percentages.size()];
        i = 0;
        for (Float percent : this.gradientEditor.percentages) {
            percentages[i++] = percent.floatValue();
        }
        ((GradientColorValue)this.value).setColors(colors);
        ((GradientColorValue)this.value).setTimeline(percentages);
    }

    public static class ColorSlider
    extends JPanel {
        Color[] paletteColors;
        JSlider slider;
        private ColorPicker colorPicker;

        public ColorSlider(Color[] paletteColors) {
            this.paletteColors = paletteColors;
            this.setLayout(new GridBagLayout());
            this.slider = new JSlider(0, 1000, 0);
            this.slider.setPaintTrack(false);
            this.add((Component)this.slider, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 6, 0, 6), 0, 0));
            this.colorPicker = new ColorPicker();
            this.add((Component)this.colorPicker, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 6, 0, 6), 0, 0));
            this.slider.addChangeListener(new ChangeListener(){

                @Override
                public void stateChanged(ChangeEvent event) {
                    ColorSlider.this.colorPicked();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width = 10;
            return size;
        }

        public void setPercentage(float percent) {
            this.slider.setValue((int)(1000.0f * percent));
        }

        public float getPercentage() {
            return (float)this.slider.getValue() / 1000.0f;
        }

        protected void colorPicked() {
        }

        public void setColors(Color[] colors) {
            this.paletteColors = colors;
            this.repaint();
        }

        public class ColorPicker
        extends JPanel {
            public ColorPicker() {
                this.addMouseListener(new MouseAdapter(){

                    @Override
                    public void mouseClicked(MouseEvent event) {
                        ColorSlider.this.slider.setValue((int)((float)event.getX() / (float)ColorPicker.this.getWidth() * 1000.0f));
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics;
                int width = this.getWidth() - 1;
                int height = this.getHeight() - 1;
                int n = ColorSlider.this.paletteColors.length - 1;
                for (int i = 0; i < n; ++i) {
                    Color color1 = ColorSlider.this.paletteColors[i];
                    Color color2 = ColorSlider.this.paletteColors[i + 1];
                    float point1 = (float)i / (float)n * (float)width;
                    float point2 = (float)(i + 1) / (float)n * (float)width;
                    g.setPaint(new GradientPaint(point1, 0.0f, color1, point2, 0.0f, color2, false));
                    g.fillRect((int)point1, 0, (int)Math.ceil(point2 - point1), height);
                }
                g.setPaint(null);
                g.setColor(Color.black);
                g.drawRect(0, 0, width, height);
            }
        }
    }

    public class GradientEditor
    extends JPanel {
        ArrayList<Color> colors = new ArrayList();
        ArrayList<Float> percentages = new ArrayList();
        int handleWidth = 12;
        int handleHeight = 12;
        int gradientX = this.handleWidth / 2;
        int gradientY = 0;
        int gradientWidth;
        int gradientHeight;
        int dragIndex = -1;
        int selectedIndex;

        public GradientEditor() {
            this.setPreferredSize(new Dimension(100, 30));
            this.addMouseListener(new MouseAdapter(){

                @Override
                public void mousePressed(MouseEvent event) {
                    GradientEditor.this.dragIndex = -1;
                    int mouseX = event.getX();
                    int mouseY = event.getY();
                    int y = GradientEditor.this.gradientY + GradientEditor.this.gradientHeight;
                    int n = GradientEditor.this.colors.size();
                    for (int i = 0; i < n; ++i) {
                        int x = GradientEditor.this.gradientX + (int)(GradientEditor.this.percentages.get(i).floatValue() * (float)GradientEditor.this.gradientWidth) - GradientEditor.this.handleWidth / 2;
                        if (mouseX < x || mouseX > x + GradientEditor.this.handleWidth || mouseY < GradientEditor.this.gradientY || mouseY > y + GradientEditor.this.handleHeight) continue;
                        GradientEditor.this.dragIndex = GradientEditor.this.selectedIndex = i;
                        GradientEditor.this.handleSelected(GradientEditor.this.colors.get(GradientEditor.this.selectedIndex));
                        GradientEditor.this.repaint();
                        break;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (GradientEditor.this.dragIndex != -1) {
                        GradientEditor.this.dragIndex = -1;
                        GradientEditor.this.repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    int mouseX = event.getX();
                    int mouseY = event.getY();
                    if (event.getClickCount() == 2) {
                        if (GradientEditor.this.percentages.size() <= 1) {
                            return;
                        }
                        if (GradientEditor.this.selectedIndex == -1 || GradientEditor.this.selectedIndex == 0) {
                            return;
                        }
                        int y = GradientEditor.this.gradientY + GradientEditor.this.gradientHeight;
                        int x = GradientEditor.this.gradientX + (int)(GradientEditor.this.percentages.get(GradientEditor.this.selectedIndex).floatValue() * (float)GradientEditor.this.gradientWidth) - GradientEditor.this.handleWidth / 2;
                        if (mouseX >= x && mouseX <= x + GradientEditor.this.handleWidth && mouseY >= GradientEditor.this.gradientY && mouseY <= y + GradientEditor.this.handleHeight) {
                            GradientEditor.this.percentages.remove(GradientEditor.this.selectedIndex);
                            GradientEditor.this.colors.remove(GradientEditor.this.selectedIndex);
                            --GradientEditor.this.selectedIndex;
                            GradientEditor.this.dragIndex = GradientEditor.this.selectedIndex;
                            if (GradientEditor.this.percentages.size() == 2) {
                                GradientEditor.this.percentages.set(1, Float.valueOf(1.0f));
                            }
                            GradientEditor.this.handleSelected(GradientEditor.this.colors.get(GradientEditor.this.selectedIndex));
                            GradientEditor.this.repaint();
                        }
                        return;
                    }
                    if (mouseX < GradientEditor.this.gradientX || mouseX > GradientEditor.this.gradientX + GradientEditor.this.gradientWidth) {
                        return;
                    }
                    if (mouseY < GradientEditor.this.gradientY || mouseY > GradientEditor.this.gradientY + GradientEditor.this.gradientHeight) {
                        return;
                    }
                    float percent = (float)(event.getX() - GradientEditor.this.gradientX) / (float)GradientEditor.this.gradientWidth;
                    if (GradientEditor.this.percentages.size() == 1) {
                        percent = 1.0f;
                    }
                    int n = GradientEditor.this.percentages.size();
                    for (int i = 0; i <= n; ++i) {
                        if (i != n && !(percent < GradientEditor.this.percentages.get(i).floatValue())) continue;
                        GradientEditor.this.percentages.add(i, Float.valueOf(percent));
                        GradientEditor.this.colors.add(i, GradientEditor.this.colors.get(i - 1));
                        GradientEditor.this.dragIndex = GradientEditor.this.selectedIndex = i;
                        GradientEditor.this.handleSelected(GradientEditor.this.colors.get(GradientEditor.this.selectedIndex));
                        GradientEditor.this.repaint();
                        break;
                    }
                }
            });
            this.addMouseMotionListener(new MouseMotionAdapter(){

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (GradientEditor.this.dragIndex == -1 || GradientEditor.this.dragIndex == 0 || GradientEditor.this.dragIndex == GradientEditor.this.percentages.size() - 1) {
                        return;
                    }
                    float percent = (float)(event.getX() - GradientEditor.this.gradientX) / (float)GradientEditor.this.gradientWidth;
                    percent = Math.max(percent, GradientEditor.this.percentages.get(GradientEditor.this.dragIndex - 1).floatValue() + 0.01f);
                    percent = Math.min(percent, GradientEditor.this.percentages.get(GradientEditor.this.dragIndex + 1).floatValue() - 0.01f);
                    GradientEditor.this.percentages.set(GradientEditor.this.dragIndex, Float.valueOf(percent));
                    GradientEditor.this.repaint();
                }
            });
        }

        public void setColor(Color color) {
            if (this.selectedIndex == -1) {
                return;
            }
            this.colors.set(this.selectedIndex, color);
            this.repaint();
        }

        public void handleSelected(Color color) {
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int n;
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D)graphics;
            int width = this.getWidth() - 1;
            int height = this.getHeight();
            this.gradientWidth = width - this.handleWidth;
            this.gradientHeight = height - 16;
            g.translate(this.gradientX, this.gradientY);
            int n2 = n = this.colors.size() == 1 ? 1 : this.colors.size() - 1;
            for (int i = 0; i < n; ++i) {
                Color color1 = this.colors.get(i);
                Color color2 = this.colors.size() == 1 ? color1 : this.colors.get(i + 1);
                float percent1 = this.percentages.get(i).floatValue();
                float percent2 = this.colors.size() == 1 ? 1.0f : this.percentages.get(i + 1).floatValue();
                int point1 = (int)(percent1 * (float)this.gradientWidth);
                int point2 = (int)Math.ceil(percent2 * (float)this.gradientWidth);
                g.setPaint(new GradientPaint(point1, 0.0f, color1, point2, 0.0f, color2, false));
                g.fillRect(point1, 0, point2 - point1, this.gradientHeight);
            }
            g.setPaint(null);
            g.setColor(Color.black);
            g.drawRect(0, 0, this.gradientWidth, this.gradientHeight);
            int y = this.gradientHeight;
            int[] yPoints = new int[]{y, y + this.handleHeight, y + this.handleHeight};
            int[] xPoints = new int[3];
            int n3 = this.colors.size();
            for (int i = 0; i < n3; ++i) {
                int x;
                xPoints[0] = x = (int)(this.percentages.get(i).floatValue() * (float)this.gradientWidth);
                xPoints[1] = x - this.handleWidth / 2;
                xPoints[2] = x + this.handleWidth / 2;
                if (i == this.selectedIndex) {
                    g.setColor(this.colors.get(i));
                    g.fillPolygon(xPoints, yPoints, 3);
                    g.fillRect(xPoints[1], yPoints[1] + 2, this.handleWidth + 1, 2);
                    g.setColor(Color.black);
                }
                g.drawPolygon(xPoints, yPoints, 3);
            }
            g.translate(-this.gradientX, -this.gradientY);
        }
    }
}

