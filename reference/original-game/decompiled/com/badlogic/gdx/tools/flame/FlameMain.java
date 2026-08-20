/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetErrorListener;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.ModelInstanceParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.emitters.Emitter;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerControllerRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteRenderer;
import com.badlogic.gdx.graphics.g3d.particles.values.GradientColorValue;
import com.badlogic.gdx.graphics.g3d.particles.values.NumericValue;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.tools.flame.BillboardBatchPanel;
import com.badlogic.gdx.tools.flame.ColorInfluencerPanel;
import com.badlogic.gdx.tools.flame.DrawPanel;
import com.badlogic.gdx.tools.flame.DynamicsInfluencerPanel;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.EffectPanel;
import com.badlogic.gdx.tools.flame.EmptyPanel;
import com.badlogic.gdx.tools.flame.EventManager;
import com.badlogic.gdx.tools.flame.GradientPanel;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.ModelInfluencerPanel;
import com.badlogic.gdx.tools.flame.NumericPanel;
import com.badlogic.gdx.tools.flame.ParticleControllerInfluencerPanel;
import com.badlogic.gdx.tools.flame.RegionInfluencerPanel;
import com.badlogic.gdx.tools.flame.RegularEmitterPanel;
import com.badlogic.gdx.tools.flame.ScaleInfluencerPanel;
import com.badlogic.gdx.tools.flame.SpawnInfluencerPanel;
import com.badlogic.gdx.tools.flame.TextureLoaderPanel;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.StringBuilder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class FlameMain
extends JFrame
implements AssetErrorListener {
    public static final String DEFAULT_FONT = "default.fnt";
    public static final String DEFAULT_BILLBOARD_PARTICLE = "pre_particle.png";
    public static final String DEFAULT_MODEL_PARTICLE = "monkey.g3db";
    public static final String DEFAULT_TEMPLATE_PFX = "defaultTemplate.pfx";
    public static final String DEFAULT_SKIN = "uiskin.json";
    public static final int EVT_ASSET_RELOADED = 0;
    LwjglCanvas lwjglCanvas;
    JPanel controllerPropertiesPanel;
    JPanel editorPropertiesPanel;
    EffectPanel effectPanel;
    JSplitPane splitPane;
    NumericValue fovValue;
    NumericValue deltaMultiplier;
    GradientColorValue backgroundColor;
    AppRenderer renderer;
    AssetManager assetManager;
    JComboBox influencerBox;
    private ParticleEffect effect;
    public Array<ControllerData> controllersData;
    ParticleSystem particleSystem;
    String lastDir;

    public FlameMain() {
        super("Flame");
        MathUtils.random = new RandomXS128();
        this.particleSystem = ParticleSystem.get();
        this.effect = new ParticleEffect();
        this.particleSystem.add(this.effect);
        this.assetManager = new AssetManager();
        this.assetManager.setErrorListener(this);
        this.assetManager.setLoader(ParticleEffect.class, new ParticleEffectLoader(new InternalFileHandleResolver()));
        this.controllersData = new Array();
        this.renderer = new AppRenderer();
        this.lwjglCanvas = new LwjglCanvas(this.renderer);
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosed(WindowEvent event) {
                Gdx.app.exit();
            }
        });
        this.initializeComponents();
        this.setSize(1280, 950);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(2);
        this.setVisible(true);
    }

    public ControllerType getControllerType() {
        ParticleController controller = this.getEmitter();
        ControllerType type = null;
        if (controller.renderer instanceof BillboardRenderer) {
            type = ControllerType.Billboard;
        } else if (controller.renderer instanceof PointSpriteRenderer) {
            type = ControllerType.PointSprite;
        } else if (controller.renderer instanceof ModelInstanceRenderer) {
            type = ControllerType.ModelInstance;
        } else if (controller.renderer instanceof ParticleControllerControllerRenderer) {
            type = ControllerType.ParticleController;
        }
        return type;
    }

    void reloadRows() {
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                EventManager.get().clear();
                FlameMain.this.editorPropertiesPanel.removeAll();
                FlameMain.this.influencerBox.removeAllItems();
                FlameMain.this.controllerPropertiesPanel.removeAll();
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new NumericPanel(FlameMain.this, FlameMain.this.fovValue, "Field of View", ""));
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new NumericPanel(FlameMain.this, FlameMain.this.deltaMultiplier, "Delta multiplier", ""));
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new GradientPanel(FlameMain.this, FlameMain.this.backgroundColor, "Background color", "", true));
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new DrawPanel(FlameMain.this, "Draw", ""));
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new TextureLoaderPanel(FlameMain.this, "Texture", ""));
                FlameMain.this.addRow(FlameMain.this.editorPropertiesPanel, new BillboardBatchPanel(FlameMain.this, FlameMain.this.renderer.billboardBatch), 1.0f, 1.0f);
                FlameMain.this.editorPropertiesPanel.repaint();
                ParticleController controller = FlameMain.this.getEmitter();
                if (controller != null) {
                    DefaultComboBoxModel model = (DefaultComboBoxModel)FlameMain.this.influencerBox.getModel();
                    ControllerType type = FlameMain.this.getControllerType();
                    if (type != null) {
                        for (InfluencerWrapper value : type.wrappers) {
                            model.addElement(value);
                        }
                    }
                    JPanel panel = null;
                    FlameMain.this.addRow(FlameMain.this.controllerPropertiesPanel, FlameMain.this.getPanel(controller.emitter));
                    int c = controller.influencers.size;
                    for (int i = 0; i < c; ++i) {
                        Influencer influencer = controller.influencers.get(i);
                        panel = FlameMain.this.getPanel(influencer);
                        if (panel == null) continue;
                        FlameMain.this.addRow(FlameMain.this.controllerPropertiesPanel, panel, 1.0f, i == c - 1 ? 1.0f : 0.0f);
                    }
                    for (Component component : FlameMain.this.controllerPropertiesPanel.getComponents()) {
                        if (!(component instanceof EditorPanel)) continue;
                        ((EditorPanel)component).update(FlameMain.this);
                    }
                }
                FlameMain.this.controllerPropertiesPanel.repaint();
            }
        });
    }

    protected JPanel getPanel(Emitter emitter) {
        if (emitter instanceof RegularEmitter) {
            return new RegularEmitterPanel(this, (RegularEmitter)emitter);
        }
        return null;
    }

    protected JPanel getPanel(Influencer influencer) {
        if (influencer instanceof ColorInfluencer.Single) {
            return new ColorInfluencerPanel(this, (ColorInfluencer.Single)influencer);
        }
        if (influencer instanceof ColorInfluencer.Random) {
            return new InfluencerPanel<ColorInfluencer.Random>(this, (ColorInfluencer.Random)influencer, "Random Color Influencer", "Assign a random color to the particles"){};
        }
        if (influencer instanceof ScaleInfluencer) {
            return new ScaleInfluencerPanel(this, (ScaleInfluencer)influencer);
        }
        if (influencer instanceof SpawnInfluencer) {
            return new SpawnInfluencerPanel(this, (SpawnInfluencer)influencer);
        }
        if (influencer instanceof DynamicsInfluencer) {
            return new DynamicsInfluencerPanel(this, (DynamicsInfluencer)influencer);
        }
        if (influencer instanceof ModelInfluencer) {
            boolean single = influencer instanceof ModelInfluencer.Single;
            String name = single ? "Model Single Influencer" : "Model Random Influencer";
            return new ModelInfluencerPanel(this, (ModelInfluencer)influencer, single, name, "Defines what model will be used for the particles");
        }
        if (influencer instanceof ParticleControllerInfluencer) {
            boolean single = influencer instanceof ParticleControllerInfluencer.Single;
            String name = single ? "Particle Controller Single Influencer" : "Particle Controller Random Influencer";
            return new ParticleControllerInfluencerPanel(this, (ParticleControllerInfluencer)influencer, single, name, "Defines what controller will be used for the particles");
        }
        if (influencer instanceof RegionInfluencer.Single) {
            return new RegionInfluencerPanel(this, "Billboard Single Region Influencer", "Assign the chosen region to the particles", (RegionInfluencer.Single)influencer);
        }
        if (influencer instanceof RegionInfluencer.Animated) {
            return new RegionInfluencerPanel(this, "Billboard Animated Region Influencer", "Animates the region of the particles", (RegionInfluencer.Animated)influencer);
        }
        if (influencer instanceof RegionInfluencer.Random) {
            return new RegionInfluencerPanel(this, "Billboard Random Region Influencer", "Assigns a randomly picked (among those selected) region to the particles", (RegionInfluencer.Random)influencer);
        }
        if (influencer instanceof ParticleControllerFinalizerInfluencer) {
            return new InfluencerPanel<ParticleControllerFinalizerInfluencer>(this, (ParticleControllerFinalizerInfluencer)influencer, "ParticleControllerFinalizer Influencer", "This is required when dealing with a controller of controllers, it will update the controller assigned to each particle, it MUST be the last influencer always.", true, false){};
        }
        return null;
    }

    protected JPanel getPanel(ParticleBatch renderer) {
        if (renderer instanceof PointSpriteParticleBatch) {
            return new EmptyPanel(this, "Point Sprite Batch", "It renders particles as point sprites.");
        }
        if (renderer instanceof BillboardParticleBatch) {
            return new BillboardBatchPanel(this, (BillboardParticleBatch)renderer);
        }
        if (renderer instanceof ModelInstanceParticleBatch) {
            return new EmptyPanel(this, "Model Instance Batch", "It renders particles as model instances.");
        }
        return null;
    }

    void addRow(JPanel panel, JPanel row) {
        this.addRow(panel, row, 1.0f, 0.0f);
    }

    void addRow(JPanel panel, JPanel row, float wx, float wy) {
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.black));
        panel.add((Component)row, new GridBagConstraints(0, -1, 1, 1, wx, wy, 11, 2, new Insets(0, 0, 0, 0), 0, 0));
    }

    public void setVisible(String name, boolean visible) {
        for (Component component : this.controllerPropertiesPanel.getComponents()) {
            if (!(component instanceof EditorPanel) || !((EditorPanel)component).getName().equals(name)) continue;
            component.setVisible(visible);
        }
    }

    private void rebuildActiveControllers() {
        Array<ParticleController> effectControllers = this.effect.getControllers();
        effectControllers.clear();
        for (ControllerData controllerData : this.controllersData) {
            if (!controllerData.enabled) continue;
            effectControllers.add(controllerData.controller);
        }
        this.effect.init();
        this.effect.start();
    }

    public ParticleController getEmitter() {
        return this.effectPanel.editIndex >= 0 ? this.controllersData.get((int)this.effectPanel.editIndex).controller : null;
    }

    public void addEmitter(ParticleController emitter) {
        this.controllersData.add(new ControllerData(emitter));
        this.rebuildActiveControllers();
    }

    public void removeEmitter(int row) {
        this.controllersData.removeIndex((int)row).controller.dispose();
        this.rebuildActiveControllers();
    }

    public void setEnabled(int emitterIndex, boolean enabled) {
        ControllerData data = this.controllersData.get(emitterIndex);
        data.enabled = enabled;
        this.rebuildActiveControllers();
    }

    public boolean isEnabled(int emitterIndex) {
        return this.controllersData.get((int)emitterIndex).enabled;
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
        this.editorPropertiesPanel = new JPanel(new GridBagLayout());
        scroll.setViewportView(this.editorPropertiesPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(70);
        JSplitPane rightSplitPane = new JSplitPane(0);
        rightSplitPane.setUI(new BasicSplitPaneUI(){

            @Override
            public void paint(Graphics g, JComponent jc) {
            }
        });
        rightSplitPane.setDividerSize(4);
        rightSplitPane.setDividerLocation(100);
        rightSplit.add((Component)rightSplitPane, "bottom");
        JPanel propertiesPanel2 = new JPanel(new GridBagLayout());
        rightSplitPane.add((Component)propertiesPanel2, "top");
        propertiesPanel2.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Influencers")));
        JScrollPane scroll2 = new JScrollPane();
        propertiesPanel2.add((Component)scroll2, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
        scroll2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JPanel influencersPanel = new JPanel(new GridBagLayout());
        this.influencerBox = new JComboBox(new DefaultComboBoxModel());
        JButton addInfluencerButton = new JButton("Add");
        addInfluencerButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                InfluencerWrapper wrapper = (InfluencerWrapper)FlameMain.this.influencerBox.getSelectedItem();
                ParticleController controller = FlameMain.this.getEmitter();
                if (controller != null) {
                    FlameMain.this.addInfluencer(wrapper.type, controller);
                }
            }
        });
        influencersPanel.add((Component)this.influencerBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
        influencersPanel.add((Component)addInfluencerButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
        scroll2.setViewportView(influencersPanel);
        scroll2.getVerticalScrollBar().setUnitIncrement(70);
        propertiesPanel2 = new JPanel(new GridBagLayout());
        rightSplitPane.add((Component)propertiesPanel2, "bottom");
        propertiesPanel2.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(3, 0, 6, 6), BorderFactory.createTitledBorder("Particle Controller Components")));
        scroll2 = new JScrollPane();
        propertiesPanel2.add((Component)scroll2, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 0, 0), 0, 0));
        scroll2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.controllerPropertiesPanel = new JPanel(new GridBagLayout());
        scroll2.setViewportView(this.controllerPropertiesPanel);
        scroll2.getVerticalScrollBar().setUnitIncrement(70);
        rightSplit.setDividerLocation(250);
        JSplitPane leftSplit = new JSplitPane(0);
        leftSplit.setUI(new BasicSplitPaneUI(){

            @Override
            public void paint(Graphics g, JComponent jc) {
            }
        });
        leftSplit.setDividerSize(4);
        this.splitPane.add((Component)leftSplit, "left");
        JPanel spacer = new JPanel(new BorderLayout());
        leftSplit.add((Component)spacer, "top");
        spacer.add(this.lwjglCanvas.getCanvas());
        spacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JPanel emittersPanel = new JPanel(new BorderLayout());
        leftSplit.add((Component)emittersPanel, "bottom");
        emittersPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(0, 6, 6, 0), BorderFactory.createTitledBorder("Particle Controllers")));
        this.effectPanel = new EffectPanel(this);
        emittersPanel.add(this.effectPanel);
        leftSplit.setDividerLocation(625);
        this.splitPane.setDividerLocation(500);
    }

    protected void addInfluencer(Class<Influencer> type, ParticleController controller) {
        if (controller.findInfluencer(type) != null) {
            return;
        }
        try {
            Influencer newModelInfluencer;
            controller.end();
            Influencer newInfluencer = type.newInstance();
            boolean replaced = false;
            if (ColorInfluencer.class.isAssignableFrom(type)) {
                replaced = controller.replaceInfluencer(ColorInfluencer.class, (ColorInfluencer)newInfluencer);
            } else if (RegionInfluencer.class.isAssignableFrom(type)) {
                replaced = controller.replaceInfluencer(RegionInfluencer.class, (RegionInfluencer)newInfluencer);
            } else if (ModelInfluencer.class.isAssignableFrom(type)) {
                newModelInfluencer = (ModelInfluencer)newInfluencer;
                ModelInfluencer currentInfluencer = controller.findInfluencer(ModelInfluencer.class);
                if (currentInfluencer != null) {
                    newModelInfluencer.models.add(currentInfluencer.models.first());
                }
                replaced = controller.replaceInfluencer(ModelInfluencer.class, (ModelInfluencer)newInfluencer);
            } else if (ParticleControllerInfluencer.class.isAssignableFrom(type)) {
                newModelInfluencer = (ParticleControllerInfluencer)newInfluencer;
                ParticleControllerInfluencer currentInfluencer = controller.findInfluencer(ParticleControllerInfluencer.class);
                if (currentInfluencer != null) {
                    ((ParticleControllerInfluencer)newModelInfluencer).templates.add(currentInfluencer.templates.first());
                }
                replaced = controller.replaceInfluencer(ParticleControllerInfluencer.class, (ParticleControllerInfluencer)newInfluencer);
            }
            if (!replaced) {
                if (this.getControllerType() != ControllerType.ParticleController) {
                    controller.influencers.add(newInfluencer);
                } else {
                    Influencer finalizer = controller.influencers.pop();
                    controller.influencers.add(newInfluencer);
                    controller.influencers.add(finalizer);
                }
            }
            controller.init();
            this.effect.start();
            this.reloadRows();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    protected boolean canAddInfluencer(Class influencerType, ParticleController controller) {
        boolean hasSameInfluencer;
        boolean bl = hasSameInfluencer = controller.findInfluencer(influencerType) != null;
        if (!hasSameInfluencer) {
            if (ColorInfluencer.Single.class.isAssignableFrom(influencerType) && controller.findInfluencer(ColorInfluencer.Random.class) != null || ColorInfluencer.Random.class.isAssignableFrom(influencerType) && controller.findInfluencer(ColorInfluencer.Single.class) != null) {
                return false;
            }
            if (RegionInfluencer.class.isAssignableFrom(influencerType)) {
                return controller.findInfluencer(RegionInfluencer.class) == null;
            }
            if (ModelInfluencer.class.isAssignableFrom(influencerType)) {
                return controller.findInfluencer(ModelInfluencer.class) == null;
            }
            if (ParticleControllerInfluencer.class.isAssignableFrom(influencerType)) {
                return controller.findInfluencer(ParticleControllerInfluencer.class) == null;
            }
        }
        return !hasSameInfluencer;
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
                new FlameMain();
            }
        });
    }

    public AppRenderer getRenderer() {
        return this.renderer;
    }

    public File showFileLoadDialog() {
        return this.showFileDialog("Open", 0);
    }

    public File showFileSaveDialog() {
        return this.showFileDialog("Save", 1);
    }

    private File showFileDialog(String title, int mode) {
        FileDialog dialog = new FileDialog((Frame)this, title, mode);
        if (this.lastDir != null) {
            dialog.setDirectory(this.lastDir);
        }
        dialog.setVisible(true);
        String file = dialog.getFile();
        String dir = dialog.getDirectory();
        if (dir == null || file == null || file.trim().length() == 0) {
            return null;
        }
        this.lastDir = dir;
        return new File(dir, file);
    }

    @Override
    public void error(AssetDescriptor asset, Throwable throwable) {
        throwable.printStackTrace();
    }

    public PointSpriteParticleBatch getPointSpriteBatch() {
        return this.renderer.pointSpriteBatch;
    }

    public BillboardParticleBatch getBillboardBatch() {
        return this.renderer.billboardBatch;
    }

    public ModelInstanceParticleBatch getModelInstanceParticleBatch() {
        return this.renderer.modelInstanceParticleBatch;
    }

    public void setAtlas(TextureAtlas atlas) {
        this.setTexture(atlas.getTextures().first());
    }

    public void setTexture(Texture texture) {
        this.renderer.billboardBatch.setTexture(texture);
        this.renderer.pointSpriteBatch.setTexture(texture);
    }

    public Texture getTexture() {
        return this.renderer.billboardBatch.getTexture();
    }

    public TextureAtlas getAtlas(Texture texture) {
        Array<TextureAtlas> atlases = this.assetManager.getAll(TextureAtlas.class, new Array());
        for (TextureAtlas atlas : atlases) {
            if (!atlas.getTextures().contains(texture)) continue;
            return atlas;
        }
        return null;
    }

    public TextureAtlas getAtlas() {
        return this.getAtlas(this.renderer.billboardBatch.getTexture());
    }

    public boolean isUsingDefaultTexture() {
        return this.renderer.billboardBatch.getTexture() == this.assetManager.get(DEFAULT_BILLBOARD_PARTICLE, Texture.class);
    }

    public Array<ParticleEffect> getParticleEffects(Array<ParticleController> controllers, Array<ParticleEffect> out) {
        out.clear();
        this.assetManager.getAll(ParticleEffect.class, out);
        int i = 0;
        while (i < out.size) {
            ParticleEffect effect = out.get(i);
            Array<ParticleController> effectControllers = effect.getControllers();
            boolean remove = true;
            for (ParticleController controller : controllers) {
                if (!effectControllers.contains(controller, true)) continue;
                remove = false;
                break;
            }
            if (remove) {
                out.removeIndex(i);
                continue;
            }
            ++i;
        }
        return out;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveEffect(File file) {
        Closeable fileWriter = null;
        try {
            ParticleEffectLoader loader = (ParticleEffectLoader)this.assetManager.getLoader(ParticleEffect.class);
            loader.save(this.effect, new ParticleEffectLoader.ParticleEffectSaveParameter(new FileHandle(file.getAbsolutePath()), this.assetManager, this.particleSystem.getBatches()));
        }
        catch (Exception ex) {
            System.out.println("Error saving effect: " + file.getAbsolutePath());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving effect.");
        }
        finally {
            StreamUtils.closeQuietly(fileWriter);
        }
    }

    public ParticleEffect openEffect(File file, boolean replaceCurrentWorkspace) {
        try {
            ParticleEffect loadedEffect = this.load(file.getAbsolutePath(), ParticleEffect.class, null, new ParticleEffectLoader.ParticleEffectLoadParameter(this.particleSystem.getBatches()));
            loadedEffect = loadedEffect.copy();
            loadedEffect.init();
            if (replaceCurrentWorkspace) {
                this.effect = loadedEffect;
                this.controllersData.clear();
                this.particleSystem.removeAll();
                this.particleSystem.add(this.effect);
                for (ParticleController controller : this.effect.getControllers()) {
                    this.controllersData.add(new ControllerData(controller));
                }
                this.rebuildActiveControllers();
            }
            this.reloadRows();
            return loadedEffect;
        }
        catch (Exception ex) {
            System.out.println("Error loading effect: " + file.getAbsolutePath());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening effect.");
            return null;
        }
    }

    public <T> T load(String resource, Class<T> type, AssetLoader loader, AssetLoaderParameters<T> params) {
        String resolvedPath = new String(resource).replaceAll("\\\\", "/");
        boolean exist = this.assetManager.isLoaded(resolvedPath, type);
        Object oldAsset = null;
        if (exist) {
            oldAsset = this.assetManager.get(resolvedPath, type);
            for (int i = this.assetManager.getReferenceCount(resolvedPath); i > 0; --i) {
                this.assetManager.unload(resolvedPath);
            }
        }
        AssetLoader currentLoader = this.assetManager.getLoader(type);
        if (loader != null) {
            this.assetManager.setLoader(type, loader);
        }
        this.assetManager.load(resource, type, params);
        this.assetManager.finishLoading();
        Object res = this.assetManager.get(resolvedPath);
        if (currentLoader != null) {
            this.assetManager.setLoader(type, currentLoader);
        }
        if (exist) {
            EventManager.get().fire(0, new Object[]{oldAsset, res});
        }
        return res;
    }

    public void restart() {
        this.effect.init();
        this.effect.start();
    }

    class AppRenderer
    implements ApplicationListener {
        private float maxActiveTimer;
        private int maxActive;
        private int lastMaxActive;
        boolean isUpdate = true;
        private CameraInputController cameraInputController;
        private Stage ui;
        TextButton playPauseButton;
        private Label fpsLabel;
        private Label pointCountLabel;
        private Label billboardCountLabel;
        private Label modelInstanceCountLabel;
        private Label maxLabel;
        StringBuilder stringBuilder;
        public PerspectiveCamera worldCamera;
        private boolean isDrawXYZ;
        private boolean isDrawXZPlane;
        private boolean isDrawXYPlane;
        private Array<Model> models;
        private ModelInstance xyzInstance;
        private ModelInstance xzPlaneInstance;
        private ModelInstance xyPlaneInstance;
        private Environment environment;
        private ModelBatch modelBatch;
        PointSpriteParticleBatch pointSpriteBatch;
        BillboardParticleBatch billboardBatch;
        ModelInstanceParticleBatch modelInstanceParticleBatch;

        AppRenderer() {
        }

        @Override
        public void create() {
            if (this.ui != null) {
                return;
            }
            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            this.modelBatch = new ModelBatch();
            this.environment = new Environment();
            this.environment.add(new DirectionalLight().set(com.badlogic.gdx.graphics.Color.WHITE, 0.0f, 0.0f, -1.0f));
            this.worldCamera = new PerspectiveCamera(67.0f, w, h);
            this.worldCamera.position.set(10.0f, 10.0f, 10.0f);
            this.worldCamera.lookAt(0.0f, 0.0f, 0.0f);
            this.worldCamera.near = 0.1f;
            this.worldCamera.far = 300.0f;
            this.worldCamera.update();
            this.cameraInputController = new CameraInputController(this.worldCamera);
            this.pointSpriteBatch = new PointSpriteParticleBatch();
            this.pointSpriteBatch.setCamera(this.worldCamera);
            this.billboardBatch = new BillboardParticleBatch();
            this.billboardBatch.setCamera(this.worldCamera);
            this.modelInstanceParticleBatch = new ModelInstanceParticleBatch();
            FlameMain.this.particleSystem.add(this.billboardBatch);
            FlameMain.this.particleSystem.add(this.pointSpriteBatch);
            FlameMain.this.particleSystem.add(this.modelInstanceParticleBatch);
            FlameMain.this.fovValue = new NumericValue();
            FlameMain.this.fovValue.setValue(67.0f);
            FlameMain.this.fovValue.setActive(true);
            FlameMain.this.deltaMultiplier = new NumericValue();
            FlameMain.this.deltaMultiplier.setValue(1.0f);
            FlameMain.this.deltaMultiplier.setActive(true);
            FlameMain.this.backgroundColor = new GradientColorValue();
            com.badlogic.gdx.graphics.Color color = com.badlogic.gdx.graphics.Color.valueOf("878787");
            FlameMain.this.backgroundColor.setColors(new float[]{color.r, color.g, color.b});
            this.models = new Array();
            ModelBuilder builder = new ModelBuilder();
            Model xyzModel = builder.createXYZCoordinates(10.0f, new Material(), 5L);
            Model planeModel = builder.createLineGrid(10, 10, 1.0f, 1.0f, new Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.WHITE)), 1L);
            this.models.add(xyzModel);
            this.models.add(planeModel);
            this.xyzInstance = new ModelInstance(xyzModel);
            this.xzPlaneInstance = new ModelInstance(planeModel);
            this.xyPlaneInstance = new ModelInstance(planeModel);
            this.xyPlaneInstance.transform.rotate(1.0f, 0.0f, 0.0f, 90.0f);
            this.setDrawXYZ(true);
            this.setDrawXZPlane(true);
            ParticleEffectLoader.ParticleEffectLoadParameter params = new ParticleEffectLoader.ParticleEffectLoadParameter(FlameMain.this.particleSystem.getBatches());
            FlameMain.this.assetManager.load(FlameMain.DEFAULT_BILLBOARD_PARTICLE, Texture.class);
            FlameMain.this.assetManager.load(FlameMain.DEFAULT_MODEL_PARTICLE, Model.class);
            FlameMain.this.assetManager.load(FlameMain.DEFAULT_SKIN, Skin.class);
            FlameMain.this.assetManager.load(FlameMain.DEFAULT_TEMPLATE_PFX, ParticleEffect.class, params);
            FlameMain.this.assetManager.finishLoading();
            FlameMain.this.assetManager.setLoader(ParticleEffect.class, new ParticleEffectLoader(new AbsoluteFileHandleResolver()));
            FlameMain.this.assetManager.get((String)FlameMain.DEFAULT_MODEL_PARTICLE, Model.class).materials.get(0).set((Attribute)new BlendingAttribute(1, 771, 1.0f));
            this.stringBuilder = new StringBuilder();
            Skin skin = FlameMain.this.assetManager.get(FlameMain.DEFAULT_SKIN, Skin.class);
            this.ui = new Stage();
            this.fpsLabel = new Label((CharSequence)"", skin);
            this.pointCountLabel = new Label((CharSequence)"", skin);
            this.billboardCountLabel = new Label((CharSequence)"", skin);
            this.modelInstanceCountLabel = new Label((CharSequence)"", skin);
            this.maxLabel = new Label((CharSequence)"", skin);
            this.playPauseButton = new TextButton("Pause", skin);
            this.playPauseButton.addListener(new ClickListener(){

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    AppRenderer.this.isUpdate = !AppRenderer.this.isUpdate;
                    AppRenderer.this.playPauseButton.setText(AppRenderer.this.isUpdate ? "Pause" : "Play");
                }
            });
            Table table = new Table(skin);
            table.setFillParent(true);
            table.pad(5.0f);
            table.add(this.fpsLabel).expandX().left().row();
            table.add(this.pointCountLabel).expandX().left().row();
            table.add(this.billboardCountLabel).expandX().left().row();
            table.add(this.modelInstanceCountLabel).expandX().left().row();
            table.add(this.maxLabel).expandX().left().row();
            table.add(this.playPauseButton).expand().bottom().left().row();
            this.ui.addActor(table);
            FlameMain.this.setTexture((Texture)FlameMain.this.assetManager.get(FlameMain.DEFAULT_BILLBOARD_PARTICLE));
            FlameMain.this.effectPanel.createDefaultEmitter(ControllerType.Billboard, true, true);
        }

        @Override
        public void resize(int width, int height) {
            Gdx.input.setInputProcessor(new InputMultiplexer(this.ui, this.cameraInputController));
            Gdx.gl.glViewport(0, 0, width, height);
            this.worldCamera.viewportWidth = width;
            this.worldCamera.viewportHeight = height;
            this.worldCamera.update();
            this.ui.getViewport().setWorldSize(width, height);
            this.ui.getViewport().update(width, height, true);
        }

        @Override
        public void render() {
            this.update();
            this.renderWorld();
        }

        private void update() {
            this.worldCamera.fieldOfView = FlameMain.this.fovValue.getValue();
            this.worldCamera.update();
            this.cameraInputController.update();
            if (this.isUpdate) {
                FlameMain.this.particleSystem.update();
                this.stringBuilder.delete(0, this.stringBuilder.length);
                this.stringBuilder.append("Point Sprites : ").append(this.pointSpriteBatch.getBufferedCount());
                this.pointCountLabel.setText(this.stringBuilder);
                this.stringBuilder.delete(0, this.stringBuilder.length);
                this.stringBuilder.append("Billboards : ").append(this.billboardBatch.getBufferedCount());
                this.billboardCountLabel.setText(this.stringBuilder);
                this.stringBuilder.delete(0, this.stringBuilder.length);
                this.stringBuilder.append("Model Instances : ").append(this.modelInstanceParticleBatch.getBufferedCount());
                this.modelInstanceCountLabel.setText(this.stringBuilder);
            }
            this.stringBuilder.delete(0, this.stringBuilder.length);
            this.stringBuilder.append("FPS : ").append(Gdx.graphics.getFramesPerSecond());
            this.fpsLabel.setText(this.stringBuilder);
            this.ui.act(Gdx.graphics.getDeltaTime());
        }

        private void renderWorld() {
            float[] colors = FlameMain.this.backgroundColor.getColors();
            Gdx.gl.glClear(16640);
            Gdx.gl.glClearColor(colors[0], colors[1], colors[2], 0.0f);
            this.modelBatch.begin(this.worldCamera);
            if (this.isDrawXYZ) {
                this.modelBatch.render(this.xyzInstance);
            }
            if (this.isDrawXZPlane) {
                this.modelBatch.render(this.xzPlaneInstance);
            }
            if (this.isDrawXYPlane) {
                this.modelBatch.render(this.xyPlaneInstance);
            }
            FlameMain.this.particleSystem.begin();
            FlameMain.this.particleSystem.draw();
            FlameMain.this.particleSystem.end();
            this.modelBatch.render((RenderableProvider)FlameMain.this.particleSystem, this.environment);
            this.modelBatch.end();
            this.ui.draw();
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

        public void setDrawXYZ(boolean isDraw) {
            this.isDrawXYZ = isDraw;
        }

        public boolean IsDrawXYZ() {
            return this.isDrawXYZ;
        }

        public void setDrawXZPlane(boolean isDraw) {
            this.isDrawXZPlane = isDraw;
        }

        public boolean IsDrawXZPlane() {
            return this.isDrawXZPlane;
        }

        public void setDrawXYPlane(boolean isDraw) {
            this.isDrawXYPlane = isDraw;
        }

        public boolean IsDrawXYPlane() {
            return this.isDrawXYPlane;
        }
    }

    public static enum ControllerType {
        Billboard("Billboard", new InfluencerWrapper[]{new InfluencerWrapper<T>("Single Color", ColorInfluencer.Single.class), new InfluencerWrapper<T>("Random Color", ColorInfluencer.Random.class), new InfluencerWrapper<T>("Single Region", RegionInfluencer.Single.class), new InfluencerWrapper<T>("Random Region", RegionInfluencer.Random.class), new InfluencerWrapper<T>("Animated Region", RegionInfluencer.Animated.class), new InfluencerWrapper<T>("Scale", ScaleInfluencer.class), new InfluencerWrapper<T>("Spawn", SpawnInfluencer.class), new InfluencerWrapper<T>("Dynamics", DynamicsInfluencer.class)}),
        PointSprite("Point Sprite", new InfluencerWrapper[]{new InfluencerWrapper<T>("Single Color", ColorInfluencer.Single.class), new InfluencerWrapper<T>("Random Color", ColorInfluencer.Random.class), new InfluencerWrapper<T>("Single Region", RegionInfluencer.Single.class), new InfluencerWrapper<T>("Random Region", RegionInfluencer.Random.class), new InfluencerWrapper<T>("Animated Region", RegionInfluencer.Animated.class), new InfluencerWrapper<T>("Scale", ScaleInfluencer.class), new InfluencerWrapper<T>("Spawn", SpawnInfluencer.class), new InfluencerWrapper<T>("Dynamics", DynamicsInfluencer.class)}),
        ModelInstance("Model Instance", new InfluencerWrapper[]{new InfluencerWrapper<T>("Single Color", ColorInfluencer.Single.class), new InfluencerWrapper<T>("Random Color", ColorInfluencer.Random.class), new InfluencerWrapper<T>("Single Model", ModelInfluencer.Single.class), new InfluencerWrapper<T>("Random Model", ModelInfluencer.Random.class), new InfluencerWrapper<T>("Scale", ScaleInfluencer.class), new InfluencerWrapper<T>("Spawn", SpawnInfluencer.class), new InfluencerWrapper<T>("Dynamics", DynamicsInfluencer.class)}),
        ParticleController("Particle Controller", new InfluencerWrapper[]{new InfluencerWrapper<T>("Single Particle Controller", ParticleControllerInfluencer.Single.class), new InfluencerWrapper<T>("Random Particle Controller", ParticleControllerInfluencer.Random.class), new InfluencerWrapper<T>("Scale", ScaleInfluencer.class), new InfluencerWrapper<T>("Spawn", SpawnInfluencer.class), new InfluencerWrapper<T>("Dynamics", DynamicsInfluencer.class)});

        public String desc;
        public InfluencerWrapper[] wrappers;

        private ControllerType(String desc, InfluencerWrapper[] wrappers) {
            this.desc = desc;
            this.wrappers = wrappers;
        }
    }

    private static class InfluencerWrapper<T> {
        String string;
        Class<Influencer> type;

        public InfluencerWrapper(String string, Class<Influencer> type) {
            this.string = string;
            this.type = type;
        }

        public String toString() {
            return this.string;
        }
    }

    static class ControllerData {
        public boolean enabled = true;
        public ParticleController controller;

        public ControllerData(ParticleController emitter) {
            this.controller = emitter;
        }
    }
}

