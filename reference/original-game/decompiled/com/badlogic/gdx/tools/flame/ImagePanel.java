/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ImagePanel
extends JPanel {
    private BufferedImage image;

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public void setImage(String file) {
        try {
            this.image = ImageIO.read(new File(file));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.image, 0, 0, null);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dimension = super.getPreferredSize();
        if (this.image != null) {
            dimension.width = this.image.getWidth();
            dimension.height = this.image.getHeight();
        }
        return dimension;
    }
}

