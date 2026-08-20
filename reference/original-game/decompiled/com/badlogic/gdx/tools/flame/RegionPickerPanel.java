/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.tools.flame.CustomCardLayout;
import com.badlogic.gdx.tools.flame.Slider;
import com.badlogic.gdx.tools.flame.TextureAtlasPanel;
import com.badlogic.gdx.tools.flame.TexturePanel;
import com.badlogic.gdx.utils.Array;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class RegionPickerPanel
extends JPanel {
    TextureAtlasPanel atlasPanel;
    TexturePanel texturePanel;
    JButton selectButton;
    JButton selectAllButton;
    JButton clearButton;
    JButton generateButton;
    JButton reverseButton;
    JComboBox generateBox;
    Slider rowSlider;
    Slider columnSlider;
    JPanel generationPanel;
    JPanel content;
    Listener listener;

    public RegionPickerPanel(Listener listener) {
        this.initializeComponents();
        this.listener = listener;
    }

    private void initializeComponents() {
        this.setLayout(new GridBagLayout());
        this.content = new JPanel();
        this.atlasPanel = new TextureAtlasPanel();
        this.texturePanel = new TexturePanel();
        CustomCardLayout cardLayout = new CustomCardLayout();
        this.content.setLayout(cardLayout);
        this.content.add((Component)this.atlasPanel, "atlas");
        this.content.add((Component)this.texturePanel, "texture");
        this.add((Component)this.content, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        JPanel controls = new JPanel(new GridBagLayout());
        this.selectButton = new JButton("Select");
        controls.add((Component)this.selectButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        controls.add((Component)new JSeparator(0), new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        JPanel pickPanel = new JPanel(new GridBagLayout());
        this.selectAllButton = new JButton("Pick All");
        pickPanel.add((Component)this.selectAllButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.clearButton = new JButton("Clear Selection");
        pickPanel.add((Component)this.clearButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.reverseButton = new JButton("Reverse Selection");
        pickPanel.add((Component)this.reverseButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        controls.add((Component)pickPanel, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.generationPanel = new JPanel(new GridBagLayout());
        this.generationPanel.add((Component)new JLabel("Rows"), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.rowSlider = new Slider(1.0f, 1.0f, 9999.0f, 1.0f);
        this.generationPanel.add((Component)this.rowSlider, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.generationPanel.add((Component)new JLabel("Columns"), new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.columnSlider = new Slider(1.0f, 1.0f, 9999.0f, 1.0f);
        this.generationPanel.add((Component)this.columnSlider, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.generateBox = new JComboBox<GenerationMode>(new DefaultComboBoxModel<GenerationMode>(GenerationMode.values()));
        this.generationPanel.add(this.generateBox, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.generateButton = new JButton("Generate");
        this.generationPanel.add((Component)this.generateButton, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        controls.add((Component)new JSeparator(0), new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        controls.add((Component)this.generationPanel, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.add((Component)controls, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.selectButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JPanel panel = (JPanel)((CustomCardLayout)RegionPickerPanel.this.content.getLayout()).getCurrentCard(RegionPickerPanel.this.content);
                TexturePanel currentTexturePanel = panel == RegionPickerPanel.this.atlasPanel ? RegionPickerPanel.this.atlasPanel.getCurrentRegionPanel() : RegionPickerPanel.this.texturePanel;
                RegionPickerPanel.this.listener.onRegionsSelected(currentTexturePanel.selectedRegions);
            }
        });
        this.selectAllButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JPanel panel = (JPanel)((CustomCardLayout)RegionPickerPanel.this.content.getLayout()).getCurrentCard(RegionPickerPanel.this.content);
                TexturePanel currentTexturePanel = panel == RegionPickerPanel.this.atlasPanel ? RegionPickerPanel.this.atlasPanel.getCurrentRegionPanel() : RegionPickerPanel.this.texturePanel;
                currentTexturePanel.selectAll();
            }
        });
        this.reverseButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JPanel panel = (JPanel)((CustomCardLayout)RegionPickerPanel.this.content.getLayout()).getCurrentCard(RegionPickerPanel.this.content);
                TexturePanel currentTexturePanel = panel == RegionPickerPanel.this.atlasPanel ? RegionPickerPanel.this.atlasPanel.getCurrentRegionPanel() : RegionPickerPanel.this.texturePanel;
                currentTexturePanel.selectedRegions.reverse();
                currentTexturePanel.revalidate();
                currentTexturePanel.repaint();
            }
        });
        this.clearButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                JPanel panel = (JPanel)((CustomCardLayout)RegionPickerPanel.this.content.getLayout()).getCurrentCard(RegionPickerPanel.this.content);
                TexturePanel currentPanel = panel == RegionPickerPanel.this.atlasPanel ? RegionPickerPanel.this.atlasPanel.getCurrentRegionPanel() : RegionPickerPanel.this.texturePanel;
                currentPanel.clearSelection();
            }
        });
        this.generateButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                RegionPickerPanel.this.generateRegions((GenerationMode)((Object)RegionPickerPanel.this.generateBox.getSelectedItem()));
                RegionPickerPanel.this.texturePanel.revalidate();
                RegionPickerPanel.this.texturePanel.repaint();
            }
        });
    }

    void generateRegions(GenerationMode mode) {
        block5: {
            int xOffset;
            int yOffset;
            int columns;
            int rows;
            Texture texture;
            block4: {
                this.texturePanel.clear();
                texture = this.texturePanel.getTexture();
                rows = (int)this.rowSlider.getValue();
                columns = (int)this.columnSlider.getValue();
                yOffset = texture.getHeight() / rows;
                xOffset = texture.getWidth() / columns;
                if (mode != GenerationMode.ByRows) break block4;
                for (int j = 0; j < rows; ++j) {
                    int rowOffset = j * yOffset;
                    for (int i = 0; i < columns; ++i) {
                        this.texturePanel.unselectedRegions.add(new TextureRegion(texture, i * xOffset, rowOffset, xOffset, yOffset));
                    }
                }
                break block5;
            }
            if (mode != GenerationMode.ByColumns) break block5;
            for (int i = 0; i < columns; ++i) {
                int columnOffset = i * xOffset;
                for (int j = 0; j < rows; ++j) {
                    this.texturePanel.unselectedRegions.add(new TextureRegion(texture, columnOffset, j * yOffset, xOffset, yOffset));
                }
            }
        }
    }

    public void setAtlas(TextureAtlas atlas) {
        this.atlasPanel.clearSelection();
        this.atlasPanel.setAtlas(atlas);
        CustomCardLayout cardLayout = (CustomCardLayout)this.content.getLayout();
        cardLayout.show(this.content, "atlas");
        this.showGenerationPanel(false);
        this.content.revalidate();
        this.content.repaint();
        this.revalidate();
        this.repaint();
    }

    public void setTexture(Texture texture) {
        this.texturePanel.clearSelection();
        this.texturePanel.setTexture(texture);
        CustomCardLayout cardLayout = (CustomCardLayout)this.content.getLayout();
        cardLayout.show(this.content, "texture");
        this.showGenerationPanel(true);
        this.content.revalidate();
        this.content.repaint();
        this.revalidate();
        this.repaint();
    }

    private void showGenerationPanel(boolean isShown) {
        this.generationPanel.setVisible(isShown);
    }

    public static interface Listener {
        public void onRegionsSelected(Array<TextureRegion> var1);
    }

    private static enum GenerationMode {
        ByRows("Generate By Rows"),
        ByColumns("Generate By Columns");

        String string;

        private GenerationMode(String string2) {
            this.string = string2;
        }

        public String toString() {
            return this.string;
        }
    }
}

