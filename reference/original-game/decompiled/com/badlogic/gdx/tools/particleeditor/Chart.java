/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import javax.swing.JPanel;

public class Chart
extends JPanel {
    private static final int POINT_SIZE = 6;
    private static final int POINT_SIZE_EXPANDED = 10;
    ArrayList<Point> points = new ArrayList();
    private int numberHeight;
    int chartX;
    int chartY;
    int chartWidth;
    int chartHeight;
    int maxX;
    int maxY;
    int overIndex = -1;
    int movingIndex = -1;
    boolean isExpanded;
    String title;
    boolean moveAll = false;
    boolean moveAllProportionally = false;
    int moveAllPrevY;

    public Chart(String title) {
        this.title = title;
        this.setLayout(new GridBagLayout());
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent event) {
                Chart.this.movingIndex = Chart.this.overIndex;
                Chart.this.moveAll = event.isControlDown();
                if (Chart.this.moveAll) {
                    Chart.this.moveAllProportionally = event.isShiftDown();
                    Chart.this.moveAllPrevY = event.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Chart.this.movingIndex = -1;
                Chart.this.moveAll = false;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    if (Chart.this.overIndex <= 0 || Chart.this.overIndex >= Chart.this.points.size()) {
                        return;
                    }
                    Chart.this.points.remove(Chart.this.overIndex);
                    Chart.this.pointsChanged();
                    Chart.this.repaint();
                    return;
                }
                if (Chart.this.movingIndex != -1) {
                    return;
                }
                if (Chart.this.overIndex != -1) {
                    return;
                }
                int mouseX = event.getX();
                int mouseY = event.getY();
                if (mouseX < Chart.this.chartX || mouseX > Chart.this.chartX + Chart.this.chartWidth) {
                    return;
                }
                if (mouseY < Chart.this.chartY || mouseY > Chart.this.chartY + Chart.this.chartHeight) {
                    return;
                }
                Point newPoint = Chart.this.pixelToPoint(mouseX, mouseY);
                int i = 0;
                Point lastPoint = null;
                for (Point point : Chart.this.points) {
                    if (point.x > newPoint.x) {
                        if (Math.abs(point.x - newPoint.x) < 0.001f) {
                            return;
                        }
                        if (lastPoint != null && Math.abs(lastPoint.x - newPoint.x) < 0.001f) {
                            return;
                        }
                        Chart.this.points.add(i, newPoint);
                        Chart.this.overIndex = i;
                        Chart.this.pointsChanged();
                        Chart.this.repaint();
                        return;
                    }
                    lastPoint = point;
                    ++i;
                }
                Chart.this.overIndex = Chart.this.points.size();
                Chart.this.points.add(newPoint);
                Chart.this.pointsChanged();
                Chart.this.repaint();
            }
        });
        this.addMouseMotionListener(new MouseMotionListener(){

            @Override
            public void mouseDragged(MouseEvent event) {
                if (Chart.this.movingIndex == -1 || Chart.this.movingIndex >= Chart.this.points.size()) {
                    return;
                }
                if (Chart.this.moveAll) {
                    int newY = event.getY();
                    float deltaY = (float)(Chart.this.moveAllPrevY - newY) / (float)Chart.this.chartHeight * (float)Chart.this.maxY;
                    for (Point point : Chart.this.points) {
                        point.y = Math.min((float)Chart.this.maxY, Math.max(0.0f, point.y + (Chart.this.moveAllProportionally ? deltaY * point.y : deltaY)));
                    }
                    Chart.this.moveAllPrevY = newY;
                } else {
                    float nextX;
                    float f = nextX = Chart.this.movingIndex == Chart.this.points.size() - 1 ? (float)Chart.this.maxX : Chart.this.points.get((int)(Chart.this.movingIndex + 1)).x - 0.001f;
                    if (Chart.this.movingIndex == 0) {
                        nextX = 0.0f;
                    }
                    float prevX = Chart.this.movingIndex == 0 ? 0.0f : Chart.this.points.get((int)(Chart.this.movingIndex - 1)).x + 0.001f;
                    Point point = Chart.this.points.get(Chart.this.movingIndex);
                    point.x = Math.min(nextX, Math.max(prevX, (float)(event.getX() - Chart.this.chartX) / (float)Chart.this.chartWidth * (float)Chart.this.maxX));
                    point.y = Math.min((float)Chart.this.maxY, (float)Math.max(0, Chart.this.chartHeight - (event.getY() - Chart.this.chartY)) / (float)Chart.this.chartHeight * (float)Chart.this.maxY);
                }
                Chart.this.pointsChanged();
                Chart.this.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                int mouseX = event.getX();
                int mouseY = event.getY();
                int oldIndex = Chart.this.overIndex;
                Chart.this.overIndex = -1;
                int pointSize = Chart.this.isExpanded ? 10 : 6;
                int i = 0;
                for (Point point : Chart.this.points) {
                    int x = Chart.this.chartX + (int)((float)Chart.this.chartWidth * (point.x / (float)Chart.this.maxX));
                    int y = Chart.this.chartY + Chart.this.chartHeight - (int)((float)Chart.this.chartHeight * (point.y / (float)Chart.this.maxY));
                    if (Math.abs(x - mouseX) <= pointSize && Math.abs(y - mouseY) <= pointSize) {
                        Chart.this.overIndex = i;
                        break;
                    }
                    ++i;
                }
                if (Chart.this.overIndex != oldIndex) {
                    Chart.this.repaint();
                }
            }
        });
    }

    public void addPoint(float x, float y) {
        this.points.add(new Point(x, y));
    }

    public void pointsChanged() {
    }

    public float[] getValuesX() {
        float[] values = new float[this.points.size()];
        int i = 0;
        for (Point point : this.points) {
            values[i++] = point.x;
        }
        return values;
    }

    public float[] getValuesY() {
        float[] values = new float[this.points.size()];
        int i = 0;
        for (Point point : this.points) {
            values[i++] = point.y;
        }
        return values;
    }

    public void setValues(float[] x, float[] y) {
        this.points.clear();
        for (int i = 0; i < x.length; ++i) {
            this.points.add(new Point(x[i], y[i]));
        }
    }

    Point pixelToPoint(float x, float y) {
        Point point = new Point();
        point.x = Math.min((float)this.maxX, Math.max(0.0f, x - (float)this.chartX) / (float)this.chartWidth * (float)this.maxX);
        point.y = Math.min((float)this.maxY, Math.max(0.0f, (float)this.chartHeight - (y - (float)this.chartY)) / (float)this.chartHeight * (float)this.maxY);
        return point;
    }

    Point pointToPixel(Point point) {
        Point pixel = new Point();
        pixel.x = this.chartX + (int)((float)this.chartWidth * (point.x / (float)this.maxX));
        pixel.y = this.chartY + this.chartHeight - (int)((float)this.chartHeight * (point.y / (float)this.maxY));
        return pixel;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int yAxisWidth;
        int maxAxisLabelWidth;
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics = g.getFontMetrics();
        if (this.numberHeight == 0) {
            this.numberHeight = this.getFont().layoutGlyphVector((FontRenderContext)g.getFontRenderContext(), (char[])new char[]{'0'}, (int)0, (int)1, (int)0).getGlyphPixelBounds((int)0, (FontRenderContext)g.getFontRenderContext(), (float)0.0f, (float)0.0f).height;
        }
        int width = this.getWidth();
        if (!this.isExpanded) {
            width = Math.min(150, width);
        }
        width = Math.max(100, width);
        int height = this.getHeight();
        if (this.isExpanded) {
            maxAxisLabelWidth = metrics.stringWidth("100%");
            this.chartX = yAxisWidth = maxAxisLabelWidth + 8;
            this.chartY = this.numberHeight / 2 + 1;
            this.chartWidth = width - yAxisWidth - 2;
            this.chartHeight = height - this.chartY - this.numberHeight - 8;
        } else {
            maxAxisLabelWidth = 0;
            this.chartX = yAxisWidth = 2;
            this.chartY = 2;
            this.chartWidth = width - yAxisWidth - 2;
            this.chartHeight = height - this.chartY - 3;
        }
        g.setColor(Color.white);
        g.fillRect(this.chartX, this.chartY, this.chartWidth, this.chartHeight);
        g.setColor(Color.black);
        g.drawRect(this.chartX, this.chartY, this.chartWidth, this.chartHeight);
        this.maxX = 1;
        int y = height;
        y = this.isExpanded ? (y -= this.numberHeight) : (y += 5);
        int xSplit = (int)Math.min(10.0f, (float)this.chartWidth / ((float)maxAxisLabelWidth * 1.5f));
        for (int i = 0; i <= xSplit; ++i) {
            float percent = (float)i / (float)xSplit;
            String label = this.axisLabel((float)this.maxX * percent);
            int labelWidth = metrics.stringWidth(label);
            int x = (int)((float)yAxisWidth + (float)this.chartWidth * percent);
            if (i != 0 && i != xSplit) {
                g.setColor(Color.lightGray);
                g.drawLine(x, this.chartY + 1, x, this.chartY + this.chartHeight);
                g.setColor(Color.black);
            }
            g.drawLine(x, y - 4, x, y - 8);
            if (!this.isExpanded) continue;
            x -= labelWidth / 2;
            if (i == xSplit) {
                x = Math.min(x, width - labelWidth);
            }
            g.drawString(label, x, y + this.numberHeight);
        }
        this.maxY = 1;
        int ySplit = this.isExpanded ? Math.min(10, this.chartHeight / (this.numberHeight * 3)) : 4;
        for (int i = 0; i <= ySplit; ++i) {
            float percent = (float)i / (float)ySplit;
            String label = this.axisLabel((float)this.maxY * percent);
            int labelWidth = metrics.stringWidth(label);
            int y2 = (int)((float)(this.chartY + this.chartHeight) - (float)this.chartHeight * percent);
            if (this.isExpanded) {
                g.drawString(label, yAxisWidth - 6 - labelWidth, y2 + this.numberHeight / 2);
            }
            if (i != 0 && i != ySplit) {
                g.setColor(Color.lightGray);
                g.drawLine(this.chartX, y2, this.chartX + this.chartWidth - 1, y2);
                g.setColor(Color.black);
            }
            g.drawLine(yAxisWidth - 4, y2, yAxisWidth, y2);
        }
        int titleWidth = metrics.stringWidth(this.title);
        int x = yAxisWidth + this.chartWidth / 2 - titleWidth / 2;
        int y3 = this.chartY + this.chartHeight / 2 - this.numberHeight / 2;
        g.setColor(Color.white);
        g.fillRect(x - 2, y3 - 2, titleWidth + 4, this.numberHeight + 4);
        g.setColor(Color.lightGray);
        g.drawString(this.title, x, y3 + this.numberHeight);
        g.setColor(Color.blue);
        g.setStroke(new BasicStroke(this.isExpanded ? 3.0f : 2.0f));
        int lastX = -1;
        int lastY = -1;
        for (Point point : this.points) {
            Point pixel = this.pointToPixel(point);
            if (lastX != -1) {
                g.drawLine(lastX, lastY, (int)pixel.x, (int)pixel.y);
            }
            lastX = (int)pixel.x;
            lastY = (int)pixel.y;
        }
        g.drawLine(lastX, lastY, this.chartX + this.chartWidth - 1, lastY);
        int n = this.points.size();
        for (int i = 0; i < n; ++i) {
            Point point = this.points.get(i);
            Point pixel = this.pointToPixel(point);
            if (this.overIndex == i) {
                g.setColor(Color.red);
            } else {
                g.setColor(Color.black);
            }
            String label = this.valueLabel(point.y);
            int labelWidth = metrics.stringWidth(label);
            int pointSize = this.isExpanded ? 10 : 6;
            int x2 = (int)pixel.x - pointSize / 2;
            int y4 = (int)pixel.y - pointSize / 2;
            g.fillOval(x2, y4, pointSize, pointSize);
            if (!this.isExpanded) continue;
            g.setColor(Color.black);
            x2 = Math.max(this.chartX + 2, Math.min(this.chartX + this.chartWidth - labelWidth, x2));
            if ((y4 -= 3) < this.chartY + this.numberHeight + 3) {
                y4 += 27;
            } else if (n > 1) {
                Point comparePoint;
                Point point2 = comparePoint = i == n - 1 ? this.points.get(i - 1) : this.points.get(i + 1);
                if (y4 < this.chartY + this.chartHeight - 27 && comparePoint.y > point.y) {
                    y4 += 27;
                }
            }
            g.drawString(label, x2, y4);
        }
    }

    private String valueLabel(float value) {
        if ((value = (float)((int)(value * 1000.0f)) / 10.0f) % 1.0f == 0.0f) {
            return String.valueOf((int)value) + '%';
        }
        return String.valueOf(value) + '%';
    }

    private String axisLabel(float value) {
        if ((value = (float)((int)(value * 100.0f))) % 1.0f == 0.0f) {
            return String.valueOf((int)value) + '%';
        }
        return String.valueOf(value) + '%';
    }

    public boolean isExpanded() {
        return this.isExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static class Point {
        public float x;
        public float y;

        public Point() {
        }

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}

