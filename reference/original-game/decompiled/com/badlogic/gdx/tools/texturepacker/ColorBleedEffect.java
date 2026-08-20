/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.texturepacker;

import java.awt.image.BufferedImage;
import java.util.NoSuchElementException;

public class ColorBleedEffect {
    static int TO_PROCESS = 0;
    static int IN_PROCESS = 1;
    static int REALDATA = 2;
    static int[][] offsets = new int[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
    ARGBColor color = new ARGBColor();

    public BufferedImage processImage(BufferedImage image, int maxIterations) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage processedImage = new BufferedImage(width, height, 2);
        int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
        Mask mask = new Mask(rgb);
        int lastPending = -1;
        for (int iterations = 0; mask.pendingSize > 0 && mask.pendingSize != lastPending && iterations < maxIterations; ++iterations) {
            lastPending = mask.pendingSize;
            this.executeIteration(rgb, mask, width, height);
        }
        processedImage.setRGB(0, 0, width, height, rgb, 0, width);
        return processedImage;
    }

    private void executeIteration(int[] rgb, Mask mask, int width, int height) {
        Mask.MaskIterator iterator = mask.new Mask.MaskIterator();
        while (iterator.hasNext()) {
            int pixelIndex = iterator.next();
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            int r = 0;
            int g = 0;
            int b = 0;
            int count = 0;
            for (int[] offset : offsets) {
                int currentPixelIndex;
                int column = x + offset[0];
                int row = y + offset[1];
                if (column < 0 || column >= width || row < 0 || row >= height || mask.getMask(currentPixelIndex = this.getPixelIndex(width, column, row)) != REALDATA) continue;
                this.color.argb = rgb[currentPixelIndex];
                r += this.color.red();
                g += this.color.green();
                b += this.color.blue();
                ++count;
            }
            if (count == 0) continue;
            this.color.setARGBA(0, r / count, g / count, b / count);
            rgb[pixelIndex] = this.color.argb;
            iterator.markAsInProgress();
        }
        iterator.reset();
    }

    private int getPixelIndex(int width, int x, int y) {
        return y * width + x;
    }

    static class ARGBColor {
        int argb = -16777216;

        ARGBColor() {
        }

        public int red() {
            return this.argb >> 16 & 0xFF;
        }

        public int green() {
            return this.argb >> 8 & 0xFF;
        }

        public int blue() {
            return this.argb >> 0 & 0xFF;
        }

        public int alpha() {
            return this.argb >> 24 & 0xFF;
        }

        public void setARGBA(int a, int r, int g, int b) {
            if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                throw new IllegalArgumentException("Invalid RGBA: " + r + ", " + g + "," + b + "," + a);
            }
            this.argb = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF) << 0;
        }
    }

    static class Mask {
        int[] data;
        int[] pending;
        int[] changing;
        int pendingSize;
        int changingSize;

        Mask(int[] rgb) {
            this.data = new int[rgb.length];
            this.pending = new int[rgb.length];
            this.changing = new int[rgb.length];
            ARGBColor color = new ARGBColor();
            for (int i = 0; i < rgb.length; ++i) {
                color.argb = rgb[i];
                if (color.alpha() == 0) {
                    this.data[i] = TO_PROCESS;
                    this.pending[this.pendingSize] = i;
                    ++this.pendingSize;
                    continue;
                }
                this.data[i] = REALDATA;
            }
        }

        int getMask(int index) {
            return this.data[index];
        }

        int removeIndex(int index) {
            if (index >= this.pendingSize) {
                throw new IndexOutOfBoundsException(String.valueOf(index));
            }
            int value = this.pending[index];
            --this.pendingSize;
            this.pending[index] = this.pending[this.pendingSize];
            return value;
        }

        class MaskIterator {
            private int index;

            MaskIterator() {
            }

            boolean hasNext() {
                return this.index < Mask.this.pendingSize;
            }

            int next() {
                if (this.index >= Mask.this.pendingSize) {
                    throw new NoSuchElementException(String.valueOf(this.index));
                }
                return Mask.this.pending[this.index++];
            }

            void markAsInProgress() {
                --this.index;
                Mask.this.changing[Mask.this.changingSize] = Mask.this.removeIndex(this.index);
                ++Mask.this.changingSize;
            }

            void reset() {
                this.index = 0;
                for (int i = 0; i < Mask.this.changingSize; ++i) {
                    int index = Mask.this.changing[i];
                    Mask.this.data[index] = REALDATA;
                }
                Mask.this.changingSize = 0;
            }
        }
    }
}

