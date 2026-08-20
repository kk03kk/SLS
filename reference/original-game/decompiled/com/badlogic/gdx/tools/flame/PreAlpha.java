/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.tools.flame.ImagePanel;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class PreAlpha
extends JFrame {
    BufferedImage image;
    ImagePanel imagePanel;
    String lastDir;

    public PreAlpha() {
        super("Premultiply alpha converter");
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosed(WindowEvent event) {
                System.exit(0);
            }
        });
        this.initializeComponents();
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(2);
        this.setVisible(true);
    }

    private void initializeComponents() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem menuItem = new JMenuItem("Open");
        menuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                PreAlpha.this.open();
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Save");
        menuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                PreAlpha.this.save();
            }
        });
        menu.add(menuItem);
        this.setJMenuBar(menuBar);
        this.imagePanel = new ImagePanel();
        this.getContentPane().add(this.imagePanel);
    }

    protected void save() {
        FileDialog dialog = new FileDialog((Frame)this, "Save Image", 1);
        if (this.lastDir != null) {
            dialog.setDirectory(this.lastDir);
        }
        dialog.setVisible(true);
        String file = dialog.getFile();
        String dir = dialog.getDirectory();
        if (dir == null || file == null || file.trim().length() == 0) {
            return;
        }
        this.lastDir = dir;
        try {
            this.generatePremultiplyAlpha(new File(dir, file));
            JOptionPane.showMessageDialog(this, "Conversion complete!");
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving image.");
            return;
        }
    }

    protected void open() {
        FileDialog dialog = new FileDialog((Frame)this, "Open Image", 0);
        if (this.lastDir != null) {
            dialog.setDirectory(this.lastDir);
        }
        dialog.setVisible(true);
        String file = dialog.getFile();
        String dir = dialog.getDirectory();
        if (dir == null || file == null || file.trim().length() == 0) {
            return;
        }
        this.lastDir = dir;
        try {
            this.image = ImageIO.read(new File(dir, file));
            this.imagePanel.setImage(this.image);
            this.imagePanel.revalidate();
            this.imagePanel.repaint();
            this.pack();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening image.");
            return;
        }
    }

    private void generatePremultiplyAlpha(File out) {
        try {
            BufferedImage outImage = new BufferedImage(this.image.getWidth(), this.image.getHeight(), 2);
            float[] color = new float[4];
            WritableRaster raster = this.image.getRaster();
            WritableRaster outRaster = outImage.getRaster();
            int w = this.image.getWidth();
            for (int x = 0; x < w; ++x) {
                int h = this.image.getHeight();
                for (int y = 0; y < h; ++y) {
                    raster.getPixel(x, y, color);
                    float alpha = color[3] / 255.0f;
                    int i = 0;
                    while (i < 3) {
                        int n = i++;
                        color[n] = color[n] * alpha;
                    }
                    outRaster.setPixel(x, y, color);
                }
            }
            ImageIO.write((RenderedImage)outImage, "png", out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (!"Nimbus".equals(info.getName())) continue;
            try {
                UIManager.setLookAndFeel(info.getClassName());
            }
            catch (Throwable throwable) {}
            break;
        }
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                new PreAlpha();
            }
        });
    }
}

