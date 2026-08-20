/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.imagepacker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

public class ImagePacker {
    BufferedImage image;
    int padding;
    boolean duplicateBorder;
    Node root;
    Map<String, Rectangle> rects;

    public ImagePacker(int width, int height, int padding, boolean duplicateBorder) {
        this.image = new BufferedImage(width, height, 6);
        this.padding = padding;
        this.duplicateBorder = duplicateBorder;
        this.root = new Node(0, 0, width, height, null, null, null);
        this.rects = new HashMap<String, Rectangle>();
    }

    public void insertImage(String name, BufferedImage image) {
        if (this.rects.containsKey(name)) {
            throw new RuntimeException("Key with name '" + name + "' is already in map");
        }
        int borderPixels = this.padding + (this.duplicateBorder ? 1 : 0);
        Rectangle rect = new Rectangle(0, 0, image.getWidth() + (borderPixels <<= 1), image.getHeight() + borderPixels);
        Node node = this.insert(this.root, rect);
        if (node == null) {
            throw new RuntimeException("Image didn't fit");
        }
        node.leaveName = name;
        rect = new Rectangle(node.rect);
        rect.width -= borderPixels;
        rect.height -= borderPixels;
        rect.x += (borderPixels >>= 1);
        rect.y += borderPixels;
        this.rects.put(name, rect);
        Graphics2D g = this.image.createGraphics();
        g.drawImage((Image)image, rect.x, rect.y, null);
        if (this.duplicateBorder) {
            g.drawImage(image, rect.x, rect.y - 1, rect.x + rect.width, rect.y, 0, 0, image.getWidth(), 1, null);
            g.drawImage(image, rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height + 1, 0, image.getHeight() - 1, image.getWidth(), image.getHeight(), null);
            g.drawImage(image, rect.x - 1, rect.y, rect.x, rect.y + rect.height, 0, 0, 1, image.getHeight(), null);
            g.drawImage(image, rect.x + rect.width, rect.y, rect.x + rect.width + 1, rect.y + rect.height, image.getWidth() - 1, 0, image.getWidth(), image.getHeight(), null);
            g.drawImage(image, rect.x - 1, rect.y - 1, rect.x, rect.y, 0, 0, 1, 1, null);
            g.drawImage(image, rect.x + rect.width, rect.y - 1, rect.x + rect.width + 1, rect.y, image.getWidth() - 1, 0, image.getWidth(), 1, null);
            g.drawImage(image, rect.x - 1, rect.y + rect.height, rect.x, rect.y + rect.height + 1, 0, image.getHeight() - 1, 1, image.getHeight(), null);
            g.drawImage(image, rect.x + rect.width, rect.y + rect.height, rect.x + rect.width + 1, rect.y + rect.height + 1, image.getWidth() - 1, image.getHeight() - 1, image.getWidth(), image.getHeight(), null);
        }
        g.dispose();
    }

    private Node insert(Node node, Rectangle rect) {
        if (node.leaveName == null && node.leftChild != null && node.rightChild != null) {
            Node newNode = null;
            newNode = this.insert(node.leftChild, rect);
            if (newNode == null) {
                newNode = this.insert(node.rightChild, rect);
            }
            return newNode;
        }
        if (node.leaveName != null) {
            return null;
        }
        if (node.rect.width == rect.width && node.rect.height == rect.height) {
            return node;
        }
        if (node.rect.width < rect.width || node.rect.height < rect.height) {
            return null;
        }
        node.leftChild = new Node();
        node.rightChild = new Node();
        int deltaWidth = node.rect.width - rect.width;
        int deltaHeight = node.rect.height - rect.height;
        if (deltaWidth > deltaHeight) {
            node.leftChild.rect.x = node.rect.x;
            node.leftChild.rect.y = node.rect.y;
            node.leftChild.rect.width = rect.width;
            node.leftChild.rect.height = node.rect.height;
            node.rightChild.rect.x = node.rect.x + rect.width;
            node.rightChild.rect.y = node.rect.y;
            node.rightChild.rect.width = node.rect.width - rect.width;
            node.rightChild.rect.height = node.rect.height;
        } else {
            node.leftChild.rect.x = node.rect.x;
            node.leftChild.rect.y = node.rect.y;
            node.leftChild.rect.width = node.rect.width;
            node.leftChild.rect.height = rect.height;
            node.rightChild.rect.x = node.rect.x;
            node.rightChild.rect.y = node.rect.y + rect.height;
            node.rightChild.rect.width = node.rect.width;
            node.rightChild.rect.height = node.rect.height - rect.height;
        }
        return this.insert(node.leftChild, rect);
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public Map<String, Rectangle> getRects() {
        return this.rects;
    }

    public static void main(String[] argv) throws IOException {
        int i;
        Random rand = new Random(0L);
        ImagePacker packer = new ImagePacker(512, 512, 1, true);
        BufferedImage[] images = new BufferedImage[100];
        for (i = 0; i < images.length; ++i) {
            Color color = new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1.0f);
            images[i] = ImagePacker.createImage(rand.nextInt(50) + 10, rand.nextInt(50) + 10, color);
        }
        Arrays.sort(images, new Comparator<BufferedImage>(){

            @Override
            public int compare(BufferedImage o1, BufferedImage o2) {
                return o2.getWidth() * o2.getHeight() - o1.getWidth() * o1.getHeight();
            }
        });
        for (i = 0; i < images.length; ++i) {
            packer.insertImage("" + i, images[i]);
        }
        ImageIO.write((RenderedImage)packer.getImage(), "png", new File("packed.png"));
    }

    private static BufferedImage createImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, 6);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    static final class Node {
        public Node leftChild;
        public Node rightChild;
        public Rectangle rect;
        public String leaveName;

        public Node(int x, int y, int width, int height, Node leftChild, Node rightChild, String leaveName) {
            this.rect = new Rectangle(x, y, width, height);
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.leaveName = leaveName;
        }

        public Node() {
            this.rect = new Rectangle();
        }
    }
}

