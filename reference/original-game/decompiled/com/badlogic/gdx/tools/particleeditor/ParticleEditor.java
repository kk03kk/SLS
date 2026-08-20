/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.tools.particleeditor.CountPanel;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.EffectPanel;
import com.badlogic.gdx.tools.particleeditor.GradientPanel;
import com.badlogic.gdx.tools.particleeditor.ImagePanel;
import com.badlogic.gdx.tools.particleeditor.NumericPanel;
import com.badlogic.gdx.tools.particleeditor.OptionsPanel;
import com.badlogic.gdx.tools.particleeditor.PercentagePanel;
import com.badlogic.gdx.tools.particleeditor.RangedNumericPanel;
import com.badlogic.gdx.tools.particleeditor.ScaledNumericPanel;
import com.badlogic.gdx.tools.particleeditor.SpawnPanel;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ParticleEditor
extends JFrame {
    public static final String DEFAULT_PARTICLE = "particle.png";
    public static final String DEFAULT_PREMULT_PARTICLE = "pre_particle.png";
    LwjglCanvas lwjglCanvas;
    JPanel rowsPanel;
    JPanel editRowsPanel;
    EffectPanel effectPanel;
    private JSplitPane splitPane;
    OrthographicCamera worldCamera;
    OrthographicCamera textCamera;
    ParticleEmitter.NumericValue pixelsPerMeter;
    ParticleEmitter.NumericValue zoomLevel;
    ParticleEmitter.NumericValue deltaMultiplier;
    ParticleEmitter.GradientColorValue backgroundColor;
    float pixelsPerMeterPrev;
    float zoomLevelPrev;
    ParticleEffect effect = new ParticleEffect();
    File effectFile;
    final HashMap<ParticleEmitter, ParticleData> particleData = new HashMap();

    public ParticleEditor() {
        super("Particle Editor");
        this.lwjglCanvas = new LwjglCanvas(new Renderer());
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosed(WindowEvent event) {
                System.exit(0);
            }
        });
        this.initializeComponents();
        this.setSize(1000, 950);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(2);
        this.setVisible(true);
    }

    void reloadRows() {
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                ParticleEditor.this.editRowsPanel.removeAll();
                ParticleEditor.this.addEditorRow(new NumericPanel(ParticleEditor.this.pixelsPerMeter, "Pixels per meter", ""));
                ParticleEditor.this.addEditorRow(new NumericPanel(ParticleEditor.this.zoomLevel, "Zoom level", ""));
                ParticleEditor.this.addEditorRow(new NumericPanel(ParticleEditor.this.deltaMultiplier, "Delta multiplier", ""));
                ParticleEditor.this.addEditorRow(new GradientPanel(ParticleEditor.this.backgroundColor, "Background color", "", true));
                ParticleEditor.this.rowsPanel.removeAll();
                ParticleEmitter emitter = ParticleEditor.this.getEmitter();
                ParticleEditor.this.addRow(new ImagePanel(ParticleEditor.this, "Image", ""));
                ParticleEditor.this.addRow(new CountPanel(ParticleEditor.this, "Count", "Min number of particles at all times, max number of particles allowed."));
                ParticleEditor.this.addRow(new RangedNumericPanel(emitter.getDelay(), "Delay", "Time from beginning of effect to emission start, in milliseconds."));
                ParticleEditor.this.addRow(new RangedNumericPanel(emitter.getDuration(), "Duration", "Time particles will be emitted, in milliseconds."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getEmission(), "Duration", "Emission", "Number of particles emitted per second."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getLife(), "Duration", "Life", "Time particles will live, in milliseconds."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getLifeOffset(), "Duration", "Life Offset", "Particle starting life consumed, in milliseconds."));
                ParticleEditor.this.addRow(new RangedNumericPanel(emitter.getXOffsetValue(), "X Offset", "Amount to offset a particle's starting X location, in world units."));
                ParticleEditor.this.addRow(new RangedNumericPanel(emitter.getYOffsetValue(), "Y Offset", "Amount to offset a particle's starting Y location, in world units."));
                ParticleEditor.this.addRow(new SpawnPanel(ParticleEditor.this, emitter.getSpawnShape(), "Spawn", "Shape used to spawn particles."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getSpawnWidth(), "Duration", "Spawn Width", "Width of the spawn shape, in world units."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getSpawnHeight(), "Duration", "Spawn Height", "Height of the spawn shape, in world units."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getScale(), "Life", "Size", "Particle size, in world units."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getVelocity(), "Life", "Velocity", "Particle speed, in world units per second."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getAngle(), "Life", "Angle", "Particle emission angle, in degrees."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getRotation(), "Life", "Rotation", "Particle rotation, in degrees."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getWind(), "Life", "Wind", "Wind strength, in world units per second."));
                ParticleEditor.this.addRow(new ScaledNumericPanel(emitter.getGravity(), "Life", "Gravity", "Gravity strength, in world units per second."));
                ParticleEditor.this.addRow(new GradientPanel(emitter.getTint(), "Tint", "", false));
                ParticleEditor.this.addRow(new PercentagePanel(emitter.getTransparency(), "Life", "Transparency", ""));
                ParticleEditor.this.addRow(new OptionsPanel(ParticleEditor.this, "Options", ""));
                for (Component component : ParticleEditor.this.rowsPanel.getComponents()) {
                    if (!(component instanceof EditorPanel)) continue;
                    ((EditorPanel)component).update(ParticleEditor.this);
                }
                ParticleEditor.this.rowsPanel.repaint();
            }
        });
    }

    void addEditorRow(JPanel row) {
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
        this.editRowsPanel.add((Component)row, new GridBagConstraints(0, -1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
    }

    void addRow(JPanel row) {
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
        this.rowsPanel.add((Component)row, new GridBagConstraints(0, -1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
    }

    public void setVisible(String name, boolean visible) {
        for (Component component : this.rowsPanel.getComponents()) {
            if (!(component instanceof EditorPanel) || !((EditorPanel)component).getName().equals(name)) continue;
            component.setVisible(visible);
        }
    }

    public ParticleEmitter getEmitter() {
        return this.effect.getEmitters().get(this.effectPanel.editIndex);
    }

    public ImageIcon getIcon(ParticleEmitter emitter) {
        ParticleData data = this.particleData.get(emitter);
        if (data == null) {
            data = new ParticleData();
            this.particleData.put(emitter, data);
        }
        String imagePath = emitter.getImagePath();
        if (data.icon == null && imagePath != null) {
            try {
                URL url;
                File file = new File(imagePath);
                if (file.exists()) {
                    url = file.toURI().toURL();
                } else {
                    url = ParticleEditor.class.getResource(imagePath);
                    if (url == null) {
                        return null;
                    }
                }
                data.icon = new ImageIcon(url);
            }
            catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }
        return data.icon;
    }

    public void setIcon(ParticleEmitter emitters, ImageIcon icon) {
        ParticleData data = this.particleData.get(emitters);
        if (data == null) {
            data = new ParticleData();
            this.particleData.put(emitters, data);
        }
        data.icon = icon;
    }

    public void setEnabled(ParticleEmitter emitter, boolean enabled) {
        ParticleData data = this.particleData.get(emitter);
        if (data == null) {
            data = new ParticleData();
            this.particleData.put(emitter, data);
        }
        data.enabled = enabled;
        emitter.reset();
    }

    public boolean isEnabled(ParticleEmitter emitter) {
        ParticleData data = this.particleData.get(emitter);
        if (data == null) {
            return true;
        }
        return data.enabled;
    }

    private void initializeComponents() {
        this.splitPane = new JSplitPane();
        this.splitPane.setUI(new BasicSplitPaneUI(){

            @Override
            public void paint(Graphics g, JComponent jc) {
            }
        });
        this.splitPane.setDividerSize(4);
        this.getContentPane().add((Component)this.splitPane, "Center");
        JSplitPane rightSplit = new JSplitPane(0);
        rightSplit.setUI(new BasicSplitPaneUI(){

            @Override
            public void paint(Graphics g, JComponent jc) {
            }
        });
        rightSplit.setDividerSize(4);
        this.splitPane.add((Component)rightSplit, "right");
        JPanel propertiesPanel = new JPanel(new GridBagLayout());
        rightSplit.add((Component)propertiesPanel, "top");
        propertiesPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Editor Properties")));
        JScrollPane scroll = new JScrollPane();
        propertiesPanel.add((Component)scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.editRowsPanel = new JPanel(new GridBagLayout());
        scroll.setViewportView(this.editRowsPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(70);
        propertiesPanel = new JPanel(new GridBagLayout());
        rightSplit.add((Component)propertiesPanel, "bottom");
        propertiesPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Emitter Properties")));
        scroll = new JScrollPane();
        propertiesPanel.add((Component)scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.rowsPanel = new JPanel(new GridBagLayout());
        scroll.setViewportView(this.rowsPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(70);
        rightSplit.setDividerLocation(200);
        JSplitPane leftSplit = new JSplitPane(0);
        leftSplit.setUI(new BasicSplitPaneUI(){

            @Override
            public void paint(Graphics g, JComponent jc) {
            }
        });
        leftSplit.setDividerSize(4);
        this.splitPane.add((Component)leftSplit, "left");
        JPanel spacer = new JPanel(new BorderLayout());
        leftSplit.add((Component)spacer, "bottom");
        spacer.add(this.lwjglCanvas.getCanvas());
        spacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JPanel emittersPanel = new JPanel(new BorderLayout());
        leftSplit.add((Component)emittersPanel, "top");
        emittersPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(0, 6, 6, 0), BorderFactory.createTitledBorder("Effect Emitters")));
        this.effectPanel = new EffectPanel(this);
        emittersPanel.add(this.effectPanel);
        leftSplit.setDividerLocation(575);
        this.splitPane.setDividerLocation(325);
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
                new ParticleEditor();
            }
        });
    }

    static class ParticleData {
        public ImageIcon icon;
        public String imagePath;
        public boolean enabled = true;

        ParticleData() {
        }
    }

    class Renderer
    implements ApplicationListener,
    InputProcessor {
        private float maxActiveTimer;
        private int maxActive;
        private int lastMaxActive;
        private boolean mouseDown;
        private int activeCount;
        private int mouseX;
        private int mouseY;
        private BitmapFont font;
        private SpriteBatch spriteBatch;
        private Sprite bgImage;

        Renderer() {
        }

        @Override
        public void create() {
            if (this.spriteBatch != null) {
                return;
            }
            this.spriteBatch = new SpriteBatch();
            ParticleEditor.this.worldCamera = new OrthographicCamera();
            ParticleEditor.this.textCamera = new OrthographicCamera();
            ParticleEditor.this.pixelsPerMeter = new ParticleEmitter.NumericValue();
            ParticleEditor.this.pixelsPerMeter.setValue(1.0f);
            ParticleEditor.this.pixelsPerMeter.setAlwaysActive(true);
            ParticleEditor.this.zoomLevel = new ParticleEmitter.NumericValue();
            ParticleEditor.this.zoomLevel.setValue(1.0f);
            ParticleEditor.this.zoomLevel.setAlwaysActive(true);
            ParticleEditor.this.deltaMultiplier = new ParticleEmitter.NumericValue();
            ParticleEditor.this.deltaMultiplier.setValue(1.0f);
            ParticleEditor.this.deltaMultiplier.setAlwaysActive(true);
            ParticleEditor.this.backgroundColor = new ParticleEmitter.GradientColorValue();
            ParticleEditor.this.backgroundColor.setColors(new float[]{0.0f, 0.0f, 0.0f});
            this.font = new BitmapFont(Gdx.files.getFileHandle("default.fnt", Files.FileType.Internal), Gdx.files.getFileHandle("default.png", Files.FileType.Internal), true);
            ParticleEditor.this.effectPanel.newExampleEmitter("Untitled", true);
            Gdx.input.setInputProcessor(this);
        }

        @Override
        public void resize(int width, int height) {
            Gdx.gl.glViewport(0, 0, width, height);
            if (ParticleEditor.this.pixelsPerMeter.getValue() <= 0.0f) {
                ParticleEditor.this.pixelsPerMeter.setValue(1.0f);
            }
            ParticleEditor.this.worldCamera.setToOrtho(false, (float)width / ParticleEditor.this.pixelsPerMeter.getValue(), (float)height / ParticleEditor.this.pixelsPerMeter.getValue());
            ParticleEditor.this.worldCamera.update();
            ParticleEditor.this.textCamera.setToOrtho(true, width, height);
            ParticleEditor.this.textCamera.update();
            ParticleEditor.this.effect.setPosition(ParticleEditor.this.worldCamera.viewportWidth / 2.0f, ParticleEditor.this.worldCamera.viewportHeight / 2.0f);
        }

        @Override
        public void render() {
            int viewWidth = Gdx.graphics.getWidth();
            int viewHeight = Gdx.graphics.getHeight();
            float delta = Math.max(0.0f, Gdx.graphics.getDeltaTime() * ParticleEditor.this.deltaMultiplier.getValue());
            float[] colors = ParticleEditor.this.backgroundColor.getColors();
            Gdx.gl.glClearColor(colors[0], colors[1], colors[2], 1.0f);
            Gdx.gl.glClear(16384);
            if (ParticleEditor.this.pixelsPerMeter.getValue() != ParticleEditor.this.pixelsPerMeterPrev || ParticleEditor.this.zoomLevel.getValue() != ParticleEditor.this.zoomLevelPrev) {
                if (ParticleEditor.this.pixelsPerMeter.getValue() <= 0.0f) {
                    ParticleEditor.this.pixelsPerMeter.setValue(1.0f);
                }
                ParticleEditor.this.worldCamera.setToOrtho(false, (float)viewWidth / ParticleEditor.this.pixelsPerMeter.getValue(), (float)viewHeight / ParticleEditor.this.pixelsPerMeter.getValue());
                ParticleEditor.this.worldCamera.zoom = ParticleEditor.this.zoomLevel.getValue();
                ParticleEditor.this.worldCamera.update();
                ParticleEditor.this.effect.setPosition(ParticleEditor.this.worldCamera.viewportWidth / 2.0f, ParticleEditor.this.worldCamera.viewportHeight / 2.0f);
                ParticleEditor.this.zoomLevelPrev = ParticleEditor.this.zoomLevel.getValue();
                ParticleEditor.this.pixelsPerMeterPrev = ParticleEditor.this.pixelsPerMeter.getValue();
            }
            this.spriteBatch.setProjectionMatrix(ParticleEditor.this.worldCamera.combined);
            this.spriteBatch.begin();
            this.spriteBatch.enableBlending();
            this.spriteBatch.setBlendFunction(770, 771);
            if (this.bgImage != null) {
                this.bgImage.setPosition((float)(viewWidth / 2) - this.bgImage.getWidth() / 2.0f, (float)(viewHeight / 2) - this.bgImage.getHeight() / 2.0f);
                this.bgImage.draw(this.spriteBatch);
            }
            this.activeCount = 0;
            boolean complete = true;
            for (ParticleEmitter emitter : ParticleEditor.this.effect.getEmitters()) {
                boolean enabled;
                if (emitter.getSprite() == null && emitter.getImagePath() != null) {
                    this.loadImage(emitter);
                }
                if (!(enabled = ParticleEditor.this.isEnabled(emitter))) continue;
                if (emitter.getSprite() != null) {
                    emitter.draw(this.spriteBatch, delta);
                }
                this.activeCount += emitter.getActiveCount();
                if (emitter.isComplete()) continue;
                complete = false;
            }
            if (complete) {
                ParticleEditor.this.effect.start();
            }
            this.maxActive = Math.max(this.maxActive, this.activeCount);
            this.maxActiveTimer += delta;
            if (this.maxActiveTimer > 3.0f) {
                this.maxActiveTimer = 0.0f;
                this.lastMaxActive = this.maxActive;
                this.maxActive = 0;
            }
            if (this.mouseDown) {
                // empty if block
            }
            this.spriteBatch.setProjectionMatrix(ParticleEditor.this.textCamera.combined);
            this.font.draw((Batch)this.spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 5.0f, 15.0f);
            this.font.draw((Batch)this.spriteBatch, "Count: " + this.activeCount, 5.0f, 35.0f);
            this.font.draw((Batch)this.spriteBatch, "Max: " + this.lastMaxActive, 5.0f, 55.0f);
            this.font.draw((Batch)this.spriteBatch, (int)(ParticleEditor.this.getEmitter().getPercentComplete() * 100.0f) + "%", 5.0f, 75.0f);
            this.spriteBatch.end();
        }

        private void loadImage(ParticleEmitter emitter) {
            final String imagePath = emitter.getImagePath();
            String imageName = new File(imagePath.replace('\\', '/')).getName();
            try {
                FileHandle file;
                if (imagePath.equals(ParticleEditor.DEFAULT_PARTICLE) || imagePath.equals(ParticleEditor.DEFAULT_PREMULT_PARTICLE)) {
                    file = Gdx.files.classpath(imagePath);
                } else if ((imagePath.contains("/") || imagePath.contains("\\")) && !imageName.contains("..")) {
                    file = Gdx.files.absolute(imagePath);
                    if (!file.exists()) {
                        file = Gdx.files.absolute(new File(ParticleEditor.this.effectFile.getParentFile(), imageName).getAbsolutePath());
                    }
                } else {
                    file = Gdx.files.absolute(new File(ParticleEditor.this.effectFile.getParentFile(), imagePath).getAbsolutePath());
                }
                emitter.setSprite(new Sprite(new Texture(file)));
                if (ParticleEditor.this.effectFile != null) {
                    URI relativeUri = ParticleEditor.this.effectFile.getParentFile().toURI().relativize(file.file().toURI());
                    emitter.setImagePath(relativeUri.getPath());
                }
            }
            catch (GdxRuntimeException ex) {
                ex.printStackTrace();
                EventQueue.invokeLater(new Runnable(){

                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(ParticleEditor.this, "Error loading image:\n" + imagePath);
                    }
                });
                emitter.setImagePath(null);
            }
        }

        @Override
        public boolean keyDown(int keycode) {
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            return false;
        }

        @Override
        public boolean touchDown(int x, int y, int pointer, int newParam) {
            Vector3 touchPoint = new Vector3(x, y, 0.0f);
            ParticleEditor.this.worldCamera.unproject(touchPoint);
            ParticleEditor.this.effect.setPosition(touchPoint.x, touchPoint.y);
            return false;
        }

        @Override
        public boolean touchUp(int x, int y, int pointer, int button) {
            ParticleEditor.this.dispatchEvent(new WindowEvent(ParticleEditor.this, 208));
            ParticleEditor.this.dispatchEvent(new WindowEvent(ParticleEditor.this, 207));
            ParticleEditor.this.requestFocusInWindow();
            return false;
        }

        @Override
        public boolean touchDragged(int x, int y, int pointer) {
            Vector3 touchPoint = new Vector3(x, y, 0.0f);
            ParticleEditor.this.worldCamera.unproject(touchPoint);
            ParticleEditor.this.effect.setPosition(touchPoint.x, touchPoint.y);
            return false;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }

        @Override
        public boolean mouseMoved(int x, int y) {
            return false;
        }

        @Override
        public boolean scrolled(int amount) {
            return false;
        }
    }
}

