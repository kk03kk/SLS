/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.tools.flame.ImagePanel;
import com.badlogic.gdx.utils.Array;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TexturePanel
extends ImagePanel {
    private Color selectedColor = Color.GREEN;
    private Color unselectedColor = Color.BLUE;
    private Color indexBackgroundColor = Color.BLACK;
    private Color indexColor = Color.WHITE;
    Array<TextureRegion> selectedRegions = new Array();
    Array<TextureRegion> unselectedRegions = new Array();
    Texture texture;

    public TexturePanel() {
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent event) {
                float x = event.getX();
                float y = event.getY();
                for (TextureRegion region : TexturePanel.this.unselectedRegions) {
                    if (!TexturePanel.this.isInsideRegion(region, x, y)) continue;
                    TexturePanel.this.select(region);
                    return;
                }
                for (TextureRegion region : TexturePanel.this.selectedRegions) {
                    if (!TexturePanel.this.isInsideRegion(region, x, y)) continue;
                    TexturePanel.this.unselect(region);
                    return;
                }
            }
        });
    }

    protected boolean isInsideRegion(TextureRegion region, float x, float y) {
        float rx = region.getRegionX();
        float ry = region.getRegionY();
        return rx <= x && x <= rx + (float)region.getRegionWidth() && ry <= y && y <= ry + (float)region.getRegionHeight();
    }

    public TexturePanel(Texture texture, Array<TextureRegion> regions) {
        this();
        this.setTexture(texture);
        this.setRegions(regions);
    }

    public void setTexture(Texture texture) {
        if (this.texture == texture) {
            return;
        }
        this.texture = texture;
        FileTextureData data = (FileTextureData)texture.getTextureData();
        this.setImage(data.getFileHandle().file().getAbsolutePath());
    }

    public Texture getTexture() {
        return this.texture;
    }

    public void clear() {
        this.selectedRegions.clear();
        this.unselectedRegions.clear();
    }

    public void clearSelection() {
        this.unselectedRegions.addAll(this.selectedRegions);
        this.selectedRegions.clear();
        this.repaint();
    }

    public void setRegions(Array<TextureRegion> regions) {
        this.unselectedRegions.clear();
        this.selectedRegions.clear();
        this.unselectedRegions.addAll(regions);
    }

    private void swap(TextureRegion region, Array<TextureRegion> src, Array<TextureRegion> dst) {
        int index = src.indexOf(region, true);
        if (index > -1) {
            src.removeIndex(index);
            dst.add(region);
            this.repaint();
        }
    }

    public void select(TextureRegion region) {
        this.swap(region, this.unselectedRegions, this.selectedRegions);
    }

    public void unselect(TextureRegion region) {
        this.swap(region, this.selectedRegions, this.unselectedRegions);
    }

    public void selectAll() {
        this.selectedRegions.addAll(this.unselectedRegions);
        this.unselectedRegions.clear();
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.draw(g, this.unselectedRegions, this.unselectedColor, false);
        this.draw(g, this.selectedRegions, this.selectedColor, true);
    }

    private void draw(Graphics g, Array<TextureRegion> regions, Color color, boolean drawIndex) {
        int i = 0;
        for (TextureRegion region : regions) {
            int x = region.getRegionX();
            int y = region.getRegionY();
            int h = region.getRegionHeight();
            if (drawIndex) {
                String indexString = "" + i;
                Rectangle bounds = g.getFontMetrics().getStringBounds(indexString, g).getBounds();
                g.setColor(this.indexBackgroundColor);
                g.fillRect(x, y + h - bounds.height, bounds.width, bounds.height);
                g.setColor(this.indexColor);
                g.drawString(indexString, x, y + h);
                ++i;
            }
            g.setColor(color);
            g.drawRect(x, y, region.getRegionWidth(), h);
        }
    }
}

