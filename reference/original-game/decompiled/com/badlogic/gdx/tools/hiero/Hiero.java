/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.tools.hiero.BMFontUtil;
import com.badlogic.gdx.tools.hiero.HieroSettings;
import com.badlogic.gdx.tools.hiero.unicodefont.GlyphPage;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ColorEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ConfigurableEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.DistanceFieldEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.EffectUtil;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.GradientEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.OutlineEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.OutlineWobbleEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.OutlineZigzagEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ShadowEffect;
import com.badlogic.gdx.utils.StringBuilder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.lwjgl.opengl.GL11;

public class Hiero
extends JFrame {
    UnicodeFont unicodeFont;
    com.badlogic.gdx.graphics.Color renderingBackgroundColor = com.badlogic.gdx.graphics.Color.BLACK;
    List<EffectPanel> effectPanels = new ArrayList<EffectPanel>();
    Preferences prefs;
    ColorEffect colorEffect;
    boolean batchMode = false;
    JScrollPane appliedEffectsScroll;
    JPanel appliedEffectsPanel;
    JButton addEffectButton;
    JTextPane sampleTextPane;
    JSpinner padAdvanceXSpinner;
    JList effectsList;
    LwjglCanvas rendererCanvas;
    JPanel gamePanel;
    JTextField fontFileText;
    JRadioButton fontFileRadio;
    JRadioButton systemFontRadio;
    JSpinner padBottomSpinner;
    JSpinner padLeftSpinner;
    JSpinner padRightSpinner;
    JSpinner padTopSpinner;
    JList fontList;
    JSpinner fontSizeSpinner;
    JSpinner gammaSpinner;
    DefaultComboBoxModel fontListModel;
    JLabel backgroundColorLabel;
    JButton browseButton;
    JSpinner padAdvanceYSpinner;
    JCheckBox italicCheckBox;
    JCheckBox boldCheckBox;
    JCheckBox monoCheckBox;
    JRadioButton javaRadio;
    JRadioButton nativeRadio;
    JRadioButton freeTypeRadio;
    JLabel glyphsTotalLabel;
    JLabel glyphPagesTotalLabel;
    JComboBox glyphPageHeightCombo;
    JComboBox glyphPageWidthCombo;
    JComboBox glyphPageCombo;
    JPanel glyphCachePanel;
    JRadioButton glyphCacheRadio;
    JRadioButton sampleTextRadio;
    DefaultComboBoxModel glyphPageComboModel;
    JButton resetCacheButton;
    JButton sampleAsciiButton;
    JButton sampleNeheButton;
    JButton sampleExtendedButton;
    DefaultComboBoxModel effectsListModel;
    JMenuItem openMenuItem;
    JMenuItem saveMenuItem;
    JMenuItem exitMenuItem;
    JMenuItem saveBMFontMenuItem;
    File saveBmFontFile;
    String lastSaveFilename = "";
    String lastSaveBMFilename = "";
    String lastOpenFilename = "";
    JPanel effectsPanel;
    JScrollPane effectsScroll;
    JPanel unicodePanel;
    JPanel bitmapPanel;
    static final String NEHE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz\n1234567890 \n\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*\u0000\u007f";
    public static final String EXTENDED_CHARS;

    public Hiero(String[] args) {
        super("Hiero v5 - Bitmap Font Tool");
        Splash splash = new Splash(this, "/splash.jpg", 2000);
        this.initialize();
        splash.close();
        this.rendererCanvas = new LwjglCanvas(new Renderer());
        this.gamePanel.add(this.rendererCanvas.getCanvas());
        this.prefs = Preferences.userNodeForPackage(Hiero.class);
        Color backgroundColor = EffectUtil.fromString(this.prefs.get("background", "000000"));
        this.backgroundColorLabel.setIcon(Hiero.getColorIcon(backgroundColor));
        this.renderingBackgroundColor = new com.badlogic.gdx.graphics.Color((float)backgroundColor.getRed() / 255.0f, (float)backgroundColor.getGreen() / 255.0f, (float)backgroundColor.getBlue() / 255.0f, 1.0f);
        this.fontList.setSelectedValue(this.prefs.get("system.font", "Arial"), true);
        this.fontFileText.setText(this.prefs.get("font.file", ""));
        Color foregroundColor = EffectUtil.fromString(this.prefs.get("foreground", "ffffff"));
        this.colorEffect = new ColorEffect();
        this.colorEffect.setColor(foregroundColor);
        this.effectsListModel.addElement(this.colorEffect);
        this.effectsListModel.addElement(new GradientEffect());
        this.effectsListModel.addElement(new OutlineEffect());
        this.effectsListModel.addElement(new OutlineWobbleEffect());
        this.effectsListModel.addElement(new OutlineZigzagEffect());
        this.effectsListModel.addElement(new ShadowEffect());
        this.effectsListModel.addElement(new DistanceFieldEffect());
        new EffectPanel(this.colorEffect);
        this.parseArgs(args);
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosed(WindowEvent event) {
                System.exit(0);
            }
        });
        this.updateFontSelector();
        this.setVisible(true);
    }

    void initialize() {
        this.initializeComponents();
        this.initializeMenus();
        this.initializeEvents();
        this.setSize(1024, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(2);
    }

    private void parseArgs(String[] args) {
        float scale = 1.0f;
        for (int i = 0; i < args.length; ++i) {
            File f;
            boolean more;
            String param = args[i];
            boolean bl = more = i < args.length - 1;
            if (param.equals("-b") || param.equals("--batch")) {
                this.batchMode = true;
                continue;
            }
            if (more && (param.equals("-s") || param.equals("--scale"))) {
                scale = Float.parseFloat(args[++i]);
                continue;
            }
            if (more && (param.equals("-i") || param.equals("--input"))) {
                f = new File(args[++i]);
                this.open(f);
                this.fontFileRadio.setText("");
                this.updateFont();
                continue;
            }
            if (more && (param.equals("-o") || param.equals("--output"))) {
                f = new File(args[++i]);
                this.saveBm(f);
                continue;
            }
            System.err.println("Unknown parameter: " + param);
            this.exit(3);
        }
        this.fontSizeSpinner.setValue((int)(0.5f + Math.max(4.0f, scale * (float)((Integer)this.fontSizeSpinner.getValue()).intValue())));
    }

    void updateFontSelector() {
        boolean useFile = this.fontFileRadio.isSelected();
        this.fontList.setEnabled(!useFile);
        this.fontFileText.setEnabled(useFile);
        this.browseButton.setEnabled(useFile);
    }

    void updateFont() {
        int fontSize = (Integer)this.fontSizeSpinner.getValue();
        File file = null;
        if (!(!this.fontFileRadio.isSelected() || (file = new File(this.fontFileText.getText())).exists() && file.isFile())) {
            file = null;
        }
        boolean isFreeType = this.freeTypeRadio.isSelected();
        boolean isNative = this.nativeRadio.isSelected();
        boolean isJava = this.javaRadio.isSelected();
        this.addEffectButton.setVisible(isJava);
        this.effectsScroll.setVisible(isJava);
        this.appliedEffectsScroll.setVisible(isJava);
        this.boldCheckBox.setEnabled(!isFreeType);
        this.italicCheckBox.setEnabled(!isFreeType);
        this.bitmapPanel.setVisible(isFreeType);
        this.unicodePanel.setVisible(!isFreeType);
        this.updateFontSelector();
        UnicodeFont unicodeFont = null;
        if (file != null) {
            try {
                unicodeFont = new UnicodeFont(this.fontFileText.getText(), fontSize, this.boldCheckBox.isSelected(), this.italicCheckBox.isSelected());
            }
            catch (Throwable ex) {
                ex.printStackTrace();
                this.fontFileRadio.setSelected(false);
            }
        }
        if (unicodeFont == null) {
            unicodeFont = new UnicodeFont(Font.decode((String)this.fontList.getSelectedValue()), fontSize, this.boldCheckBox.isSelected(), this.italicCheckBox.isSelected());
        }
        unicodeFont.setMono(this.monoCheckBox.isSelected());
        unicodeFont.setGamma(((Number)this.gammaSpinner.getValue()).floatValue());
        unicodeFont.setPaddingTop(((Number)this.padTopSpinner.getValue()).intValue());
        unicodeFont.setPaddingRight(((Number)this.padRightSpinner.getValue()).intValue());
        unicodeFont.setPaddingBottom(((Number)this.padBottomSpinner.getValue()).intValue());
        unicodeFont.setPaddingLeft(((Number)this.padLeftSpinner.getValue()).intValue());
        unicodeFont.setPaddingAdvanceX(((Number)this.padAdvanceXSpinner.getValue()).intValue());
        unicodeFont.setPaddingAdvanceY(((Number)this.padAdvanceYSpinner.getValue()).intValue());
        unicodeFont.setGlyphPageWidth(((Number)this.glyphPageWidthCombo.getSelectedItem()).intValue());
        unicodeFont.setGlyphPageHeight(((Number)this.glyphPageHeightCombo.getSelectedItem()).intValue());
        if (this.nativeRadio.isSelected()) {
            unicodeFont.setRenderType(UnicodeFont.RenderType.Native);
        } else if (this.freeTypeRadio.isSelected()) {
            unicodeFont.setRenderType(UnicodeFont.RenderType.FreeType);
        } else {
            unicodeFont.setRenderType(UnicodeFont.RenderType.Java);
        }
        for (EffectPanel panel : this.effectPanels) {
            unicodeFont.getEffects().add(panel.getEffect());
        }
        int size = this.sampleTextPane.getFont().getSize();
        if (size < 14) {
            size = 14;
        }
        this.sampleTextPane.setFont(unicodeFont.getFont().deriveFont((float)size));
        if (this.unicodeFont != null) {
            this.unicodeFont.dispose();
        }
        this.unicodeFont = unicodeFont;
        this.updateFontSelector();
    }

    void saveBm(File file) {
        this.saveBmFontFile = file;
    }

    void save(File file) throws IOException {
        HieroSettings settings = new HieroSettings();
        settings.setFontName((String)this.fontList.getSelectedValue());
        settings.setFontSize((Integer)this.fontSizeSpinner.getValue());
        settings.setFont2File(this.fontFileText.getText());
        settings.setFont2Active(this.fontFileRadio.isSelected());
        settings.setBold(this.boldCheckBox.isSelected());
        settings.setItalic(this.italicCheckBox.isSelected());
        settings.setMono(this.monoCheckBox.isSelected());
        settings.setGamma(((Number)this.gammaSpinner.getValue()).floatValue());
        settings.setPaddingTop(((Number)this.padTopSpinner.getValue()).intValue());
        settings.setPaddingRight(((Number)this.padRightSpinner.getValue()).intValue());
        settings.setPaddingBottom(((Number)this.padBottomSpinner.getValue()).intValue());
        settings.setPaddingLeft(((Number)this.padLeftSpinner.getValue()).intValue());
        settings.setPaddingAdvanceX(((Number)this.padAdvanceXSpinner.getValue()).intValue());
        settings.setPaddingAdvanceY(((Number)this.padAdvanceYSpinner.getValue()).intValue());
        settings.setGlyphPageWidth(((Number)this.glyphPageWidthCombo.getSelectedItem()).intValue());
        settings.setGlyphPageHeight(((Number)this.glyphPageHeightCombo.getSelectedItem()).intValue());
        settings.setGlyphText(this.sampleTextPane.getText());
        if (this.nativeRadio.isSelected()) {
            settings.setRenderType(UnicodeFont.RenderType.Native.ordinal());
        } else if (this.freeTypeRadio.isSelected()) {
            settings.setRenderType(UnicodeFont.RenderType.FreeType.ordinal());
        } else {
            settings.setRenderType(UnicodeFont.RenderType.Java.ordinal());
        }
        for (EffectPanel panel : this.effectPanels) {
            settings.getEffects().add(panel.getEffect());
        }
        settings.save(file);
    }

    void open(File file) {
        String font2;
        EffectPanel[] panels = this.effectPanels.toArray(new EffectPanel[this.effectPanels.size()]);
        for (int i = 0; i < panels.length; ++i) {
            panels[i].remove();
        }
        HieroSettings settings = new HieroSettings(file.getAbsolutePath());
        this.fontList.setSelectedValue(settings.getFontName(), true);
        this.fontSizeSpinner.setValue(new Integer(settings.getFontSize()));
        this.boldCheckBox.setSelected(settings.isBold());
        this.italicCheckBox.setSelected(settings.isItalic());
        this.monoCheckBox.setSelected(settings.isMono());
        this.gammaSpinner.setValue(new Float(settings.getGamma()));
        this.padTopSpinner.setValue(new Integer(settings.getPaddingTop()));
        this.padRightSpinner.setValue(new Integer(settings.getPaddingRight()));
        this.padBottomSpinner.setValue(new Integer(settings.getPaddingBottom()));
        this.padLeftSpinner.setValue(new Integer(settings.getPaddingLeft()));
        this.padAdvanceXSpinner.setValue(new Integer(settings.getPaddingAdvanceX()));
        this.padAdvanceYSpinner.setValue(new Integer(settings.getPaddingAdvanceY()));
        this.glyphPageWidthCombo.setSelectedItem(new Integer(settings.getGlyphPageWidth()));
        this.glyphPageHeightCombo.setSelectedItem(new Integer(settings.getGlyphPageHeight()));
        if (settings.getRenderType() == UnicodeFont.RenderType.Native.ordinal()) {
            this.nativeRadio.setSelected(true);
        } else if (settings.getRenderType() == UnicodeFont.RenderType.FreeType.ordinal()) {
            this.freeTypeRadio.setSelected(true);
        } else if (settings.getRenderType() == UnicodeFont.RenderType.Java.ordinal()) {
            this.javaRadio.setSelected(true);
        }
        String gt = settings.getGlyphText();
        if (gt.length() > 0) {
            this.sampleTextPane.setText(settings.getGlyphText());
        }
        if ((font2 = settings.getFont2File()).length() > 0) {
            this.fontFileText.setText(font2);
        } else {
            this.fontFileText.setText(this.prefs.get("font.file", ""));
        }
        this.fontFileRadio.setSelected(settings.isFont2Active());
        this.systemFontRadio.setSelected(!settings.isFont2Active());
        block1: for (ConfigurableEffect settingsEffect : settings.getEffects()) {
            int n = this.effectsListModel.getSize();
            for (int i = 0; i < n; ++i) {
                ConfigurableEffect effect = (ConfigurableEffect)this.effectsListModel.getElementAt(i);
                if (effect.getClass() != settingsEffect.getClass()) continue;
                effect.setValues(settingsEffect.getValues());
                new EffectPanel(effect);
                continue block1;
            }
        }
        this.updateFont();
    }

    void exit(final int exitCode) {
        this.rendererCanvas.stop();
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                System.exit(exitCode);
            }
        });
    }

    private void initializeEvents() {
        this.fontList.addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                if (evt.getValueIsAdjusting()) {
                    return;
                }
                Hiero.this.prefs.put("system.font", (String)Hiero.this.fontList.getSelectedValue());
                Hiero.this.updateFont();
            }
        });
        class FontUpdateListener
        implements ChangeListener,
        ActionListener {
            FontUpdateListener() {
            }

            @Override
            public void stateChanged(ChangeEvent evt) {
                Hiero.this.updateFont();
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.updateFont();
            }

            public void addSpinners(JSpinner[] spinners) {
                for (int i = 0; i < spinners.length; ++i) {
                    final JSpinner spinner = spinners[i];
                    spinner.addChangeListener(this);
                    ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().addKeyListener(new KeyAdapter(){
                        String lastText;

                        @Override
                        public void keyReleased(KeyEvent evt) {
                            JFormattedTextField textField = ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
                            String text = textField.getText();
                            if (text.length() == 0) {
                                return;
                            }
                            if (text.equals(this.lastText)) {
                                return;
                            }
                            this.lastText = text;
                            int caretPosition = textField.getCaretPosition();
                            try {
                                spinner.setValue(Integer.valueOf(text));
                            }
                            catch (NumberFormatException numberFormatException) {
                                // empty catch block
                            }
                            textField.setCaretPosition(caretPosition);
                        }
                    });
                }
            }
        }
        FontUpdateListener listener = new FontUpdateListener();
        listener.addSpinners(new JSpinner[]{this.padTopSpinner, this.padRightSpinner, this.padBottomSpinner, this.padLeftSpinner, this.padAdvanceXSpinner, this.padAdvanceYSpinner});
        this.fontSizeSpinner.addChangeListener(listener);
        this.gammaSpinner.addChangeListener(listener);
        this.glyphPageWidthCombo.addActionListener(listener);
        this.glyphPageHeightCombo.addActionListener(listener);
        this.boldCheckBox.addActionListener(listener);
        this.italicCheckBox.addActionListener(listener);
        this.monoCheckBox.addActionListener(listener);
        this.resetCacheButton.addActionListener(listener);
        this.javaRadio.addActionListener(listener);
        this.nativeRadio.addActionListener(listener);
        this.freeTypeRadio.addActionListener(listener);
        this.sampleTextRadio.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.glyphCachePanel.setVisible(false);
            }
        });
        this.glyphCacheRadio.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.glyphCachePanel.setVisible(true);
            }
        });
        this.fontFileText.getDocument().addDocumentListener(new DocumentListener(){

            @Override
            public void removeUpdate(DocumentEvent evt) {
                this.changed();
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                this.changed();
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                this.changed();
            }

            private void changed() {
                File file = new File(Hiero.this.fontFileText.getText());
                if (!(!Hiero.this.fontList.isEnabled() || file.exists() && file.isFile())) {
                    return;
                }
                Hiero.this.prefs.put("font.file", Hiero.this.fontFileText.getText());
                Hiero.this.updateFont();
            }
        });
        ActionListener al = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.updateFontSelector();
                Hiero.this.updateFont();
            }
        };
        this.systemFontRadio.addActionListener(al);
        this.fontFileRadio.addActionListener(al);
        this.browseButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                String fileName;
                FileDialog dialog = new FileDialog((Frame)Hiero.this, "Choose TrueType font file", 0);
                dialog.setLocationRelativeTo(null);
                dialog.setFile("*.ttf");
                dialog.setDirectory(Hiero.this.prefs.get("dir.font", ""));
                dialog.setVisible(true);
                if (dialog.getDirectory() != null) {
                    Hiero.this.prefs.put("dir.font", dialog.getDirectory());
                }
                if ((fileName = dialog.getFile()) == null) {
                    return;
                }
                Hiero.this.fontFileText.setText(new File(dialog.getDirectory(), fileName).getAbsolutePath());
            }
        });
        this.backgroundColorLabel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent evt) {
                Color color = JColorChooser.showDialog(null, "Choose a background color", EffectUtil.fromString(Hiero.this.prefs.get("background", "000000")));
                if (color == null) {
                    return;
                }
                Hiero.this.renderingBackgroundColor = new com.badlogic.gdx.graphics.Color((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, 1.0f);
                Hiero.this.backgroundColorLabel.setIcon(Hiero.getColorIcon(color));
                Hiero.this.prefs.put("background", EffectUtil.toString(color));
            }
        });
        this.effectsList.addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                ConfigurableEffect selectedEffect = (ConfigurableEffect)Hiero.this.effectsList.getSelectedValue();
                boolean enabled = selectedEffect != null;
                Iterator<EffectPanel> iter = Hiero.this.effectPanels.iterator();
                while (iter.hasNext()) {
                    ConfigurableEffect effect = iter.next().getEffect();
                    if (effect != selectedEffect) continue;
                    enabled = false;
                    break;
                }
                Hiero.this.addEffectButton.setEnabled(enabled);
            }
        });
        this.effectsList.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2 && Hiero.this.addEffectButton.isEnabled()) {
                    Hiero.this.addEffectButton.doClick();
                }
            }
        });
        this.addEffectButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                new EffectPanel((ConfigurableEffect)Hiero.this.effectsList.getSelectedValue());
            }
        });
        this.openMenuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                String fileName;
                FileDialog dialog = new FileDialog((Frame)Hiero.this, "Open Hiero settings file", 0);
                dialog.setLocationRelativeTo(null);
                dialog.setFile("*.hiero");
                dialog.setDirectory(Hiero.this.prefs.get("dir.open", ""));
                dialog.setVisible(true);
                if (dialog.getDirectory() != null) {
                    Hiero.this.prefs.put("dir.open", dialog.getDirectory());
                }
                if ((fileName = dialog.getFile()) == null) {
                    return;
                }
                Hiero.this.lastOpenFilename = fileName;
                Hiero.this.open(new File(dialog.getDirectory(), fileName));
            }
        });
        this.saveMenuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                String fileName;
                FileDialog dialog = new FileDialog((Frame)Hiero.this, "Save Hiero settings file", 1);
                dialog.setLocationRelativeTo(null);
                dialog.setFile("*.hiero");
                dialog.setDirectory(Hiero.this.prefs.get("dir.save", ""));
                if (Hiero.this.lastSaveFilename.length() > 0) {
                    dialog.setFile(Hiero.this.lastSaveFilename);
                } else if (Hiero.this.lastOpenFilename.length() > 0) {
                    dialog.setFile(Hiero.this.lastOpenFilename);
                }
                dialog.setVisible(true);
                if (dialog.getDirectory() != null) {
                    Hiero.this.prefs.put("dir.save", dialog.getDirectory());
                }
                if ((fileName = dialog.getFile()) == null) {
                    return;
                }
                if (!fileName.endsWith(".hiero")) {
                    fileName = fileName + ".hiero";
                }
                Hiero.this.lastSaveFilename = fileName;
                File file = new File(dialog.getDirectory(), fileName);
                try {
                    Hiero.this.save(file);
                }
                catch (IOException ex) {
                    throw new RuntimeException("Error saving Hiero settings file: " + file.getAbsolutePath(), ex);
                }
            }
        });
        this.saveBMFontMenuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                String fileName;
                FileDialog dialog = new FileDialog((Frame)Hiero.this, "Save BMFont files", 1);
                dialog.setLocationRelativeTo(null);
                dialog.setFile("*.fnt");
                dialog.setDirectory(Hiero.this.prefs.get("dir.savebm", ""));
                if (Hiero.this.lastSaveBMFilename.length() > 0) {
                    dialog.setFile(Hiero.this.lastSaveBMFilename);
                } else if (Hiero.this.lastOpenFilename.length() > 0) {
                    dialog.setFile(Hiero.this.lastOpenFilename.replace(".hiero", ".fnt"));
                }
                dialog.setVisible(true);
                if (dialog.getDirectory() != null) {
                    Hiero.this.prefs.put("dir.savebm", dialog.getDirectory());
                }
                if ((fileName = dialog.getFile()) == null) {
                    return;
                }
                Hiero.this.lastSaveBMFilename = fileName;
                Hiero.this.saveBm(new File(dialog.getDirectory(), fileName));
            }
        });
        this.exitMenuItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.dispose();
            }
        });
        this.sampleNeheButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.sampleTextPane.setText(Hiero.NEHE_CHARS);
                Hiero.this.resetCacheButton.doClick();
            }
        });
        this.sampleAsciiButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(Hiero.NEHE_CHARS);
                buffer.append('\n');
                int count = 0;
                for (int i = 33; i <= 255; ++i) {
                    if (buffer.indexOf(Character.toString((char)i)) != -1) continue;
                    buffer.append((char)i);
                    if (++count % 30 != 0) continue;
                    buffer.append('\n');
                }
                Hiero.this.sampleTextPane.setText(buffer.toString());
                Hiero.this.resetCacheButton.doClick();
            }
        });
        this.sampleExtendedButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                Hiero.this.sampleTextPane.setText(EXTENDED_CHARS);
                Hiero.this.resetCacheButton.doClick();
            }
        });
    }

    private void initializeComponents() {
        this.getContentPane().setLayout(new GridBagLayout());
        JPanel leftSidePanel = new JPanel();
        leftSidePanel.setLayout(new GridBagLayout());
        this.getContentPane().add((Component)leftSidePanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        JPanel fontPanel = new JPanel();
        leftSidePanel.add((Component)fontPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(5, 5, 5, 5), 0, 0));
        fontPanel.setLayout(new GridBagLayout());
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));
        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(32, 0, 256, 1));
        fontPanel.add((Component)this.fontSizeSpinner, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 10), 0, 0));
        ((JSpinner.DefaultEditor)this.fontSizeSpinner.getEditor()).getTextField().setColumns(2);
        JScrollPane fontScroll = new JScrollPane();
        fontPanel.add((Component)fontScroll, new GridBagConstraints(1, 1, 3, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.fontListModel = new DefaultComboBoxModel<String>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        this.fontList = new JList();
        fontScroll.setViewportView(this.fontList);
        this.fontList.setModel(this.fontListModel);
        this.fontList.setVisibleRowCount(6);
        this.fontList.setSelectedIndex(0);
        fontScroll.setMinimumSize(new Dimension(220, this.fontList.getPreferredScrollableViewportSize().height));
        this.systemFontRadio = new JRadioButton("System:", true);
        fontPanel.add((Component)this.systemFontRadio, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 12, 0, new Insets(0, 5, 0, 5), 0, 0));
        this.systemFontRadio.setMargin(new Insets(0, 0, 0, 0));
        this.fontFileRadio = new JRadioButton("File:");
        fontPanel.add((Component)this.fontFileRadio, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.fontFileRadio.setMargin(new Insets(0, 0, 0, 0));
        this.fontFileText = new JTextField();
        fontPanel.add((Component)this.fontFileText, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 5, 0), 0, 0));
        fontPanel.add((Component)new JLabel("Size:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.unicodePanel = new JPanel(new GridBagLayout());
        fontPanel.add((Component)this.unicodePanel, new GridBagConstraints(2, 3, 2, 1, 0.0, 0.0, 13, 2, new Insets(0, 0, 0, 5), 0, 0));
        this.boldCheckBox = new JCheckBox("Bold");
        this.unicodePanel.add((Component)this.boldCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.italicCheckBox = new JCheckBox("Italic");
        this.unicodePanel.add((Component)this.italicCheckBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.bitmapPanel = new JPanel(new GridBagLayout());
        fontPanel.add((Component)this.bitmapPanel, new GridBagConstraints(2, 3, 2, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 5), 0, 0));
        this.bitmapPanel.add((Component)new JLabel("Gamma:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.gammaSpinner = new JSpinner(new SpinnerNumberModel(1.8f, 0.0, 30.0, 0.01));
        ((JSpinner.DefaultEditor)this.gammaSpinner.getEditor()).getTextField().setColumns(2);
        this.bitmapPanel.add((Component)this.gammaSpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 5, 10), 0, 0));
        this.monoCheckBox = new JCheckBox("Mono");
        this.bitmapPanel.add((Component)this.monoCheckBox, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.browseButton = new JButton("...");
        fontPanel.add((Component)this.browseButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.browseButton.setMargin(new Insets(0, 0, 0, 0));
        fontPanel.add((Component)new JLabel("Rendering:"), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 12, 0, new Insets(0, 0, 5, 5), 0, 0));
        JPanel renderingPanel = new JPanel(new GridBagLayout());
        fontPanel.add((Component)renderingPanel, new GridBagConstraints(1, 4, 3, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.freeTypeRadio = new JRadioButton("FreeType");
        renderingPanel.add((Component)this.freeTypeRadio, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.javaRadio = new JRadioButton("Java");
        renderingPanel.add((Component)this.javaRadio, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.nativeRadio = new JRadioButton("Native");
        renderingPanel.add((Component)this.nativeRadio, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.systemFontRadio);
        buttonGroup.add(this.fontFileRadio);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(this.freeTypeRadio);
        buttonGroup.add(this.javaRadio);
        buttonGroup.add(this.nativeRadio);
        this.freeTypeRadio.setSelected(true);
        JPanel samplePanel = new JPanel();
        leftSidePanel.add((Component)samplePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 1, new Insets(5, 0, 5, 5), 0, 0));
        samplePanel.setLayout(new GridBagLayout());
        samplePanel.setBorder(BorderFactory.createTitledBorder("Sample Text"));
        JScrollPane textScroll = new JScrollPane();
        samplePanel.add((Component)textScroll, new GridBagConstraints(0, 0, 4, 1, 1.0, 1.0, 10, 1, new Insets(0, 5, 5, 5), 0, 0));
        this.sampleTextPane = new JTextPane();
        textScroll.setViewportView(this.sampleTextPane);
        this.sampleNeheButton = new JButton();
        this.sampleNeheButton.setText("NEHE");
        samplePanel.add((Component)this.sampleNeheButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.sampleAsciiButton = new JButton();
        this.sampleAsciiButton.setText("ASCII");
        samplePanel.add((Component)this.sampleAsciiButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.sampleExtendedButton = new JButton();
        this.sampleExtendedButton.setText("Extended");
        samplePanel.add((Component)this.sampleExtendedButton, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 13, 0, new Insets(0, 0, 5, 5), 0, 0));
        JPanel renderingPanel2 = new JPanel();
        leftSidePanel.add((Component)renderingPanel2, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, 10, 1, new Insets(0, 5, 5, 5), 0, 0));
        renderingPanel2.setBorder(BorderFactory.createTitledBorder("Rendering"));
        renderingPanel2.setLayout(new GridBagLayout());
        JPanel wrapperPanel = new JPanel();
        renderingPanel2.add((Component)wrapperPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 5, 5, 5), 0, 0));
        wrapperPanel.setLayout(new BorderLayout());
        wrapperPanel.setBackground(Color.white);
        this.gamePanel = new JPanel();
        wrapperPanel.add(this.gamePanel);
        this.gamePanel.setLayout(new BorderLayout());
        this.gamePanel.setBackground(Color.white);
        this.glyphCachePanel = new JPanel(){
            private int maxWidth;

            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = this.maxWidth = Math.max(this.maxWidth, size.width);
                return size;
            }
        };
        this.glyphCachePanel.setVisible(false);
        renderingPanel2.add((Component)this.glyphCachePanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 11, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.glyphCachePanel.setLayout(new GridBagLayout());
        this.glyphCachePanel.add((Component)new JLabel("Glyphs:"), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.glyphCachePanel.add((Component)new JLabel("Pages:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.glyphCachePanel.add((Component)new JLabel("Page width:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.glyphCachePanel.add((Component)new JLabel("Page height:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.glyphPageWidthCombo = new JComboBox<Integer>(new DefaultComboBoxModel<Integer>(new Integer[]{new Integer(32), new Integer(64), new Integer(128), new Integer(256), new Integer(512), new Integer(1024), new Integer(2048)}));
        this.glyphCachePanel.add((Component)this.glyphPageWidthCombo, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.glyphPageWidthCombo.setSelectedIndex(4);
        this.glyphPageHeightCombo = new JComboBox<Integer>(new DefaultComboBoxModel<Integer>(new Integer[]{new Integer(32), new Integer(64), new Integer(128), new Integer(256), new Integer(512), new Integer(1024), new Integer(2048)}));
        this.glyphCachePanel.add((Component)this.glyphPageHeightCombo, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.glyphPageHeightCombo.setSelectedIndex(4);
        this.resetCacheButton = new JButton("Reset Cache");
        this.glyphCachePanel.add((Component)this.resetCacheButton, new GridBagConstraints(0, 6, 2, 1, 1.0, 0.0, 10, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.glyphPagesTotalLabel = new JLabel("1");
        this.glyphCachePanel.add((Component)this.glyphPagesTotalLabel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.glyphsTotalLabel = new JLabel("0");
        this.glyphCachePanel.add((Component)this.glyphsTotalLabel, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.glyphPageComboModel = new DefaultComboBoxModel();
        this.glyphPageCombo = new JComboBox();
        this.glyphCachePanel.add((Component)this.glyphPageCombo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.glyphPageCombo.setModel(this.glyphPageComboModel);
        this.glyphCachePanel.add((Component)new JLabel("View:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 5, 5), 0, 0));
        JPanel radioButtonsPanel = new JPanel();
        renderingPanel2.add((Component)radioButtonsPanel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        radioButtonsPanel.setLayout(new GridBagLayout());
        this.sampleTextRadio = new JRadioButton("Sample text");
        radioButtonsPanel.add((Component)this.sampleTextRadio, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        this.sampleTextRadio.setSelected(true);
        this.glyphCacheRadio = new JRadioButton("Glyph cache");
        radioButtonsPanel.add((Component)this.glyphCacheRadio, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 5, 5), 0, 0));
        radioButtonsPanel.add((Component)new JLabel("Background:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 5, 5, 5), 0, 0));
        this.backgroundColorLabel = new JLabel();
        radioButtonsPanel.add((Component)this.backgroundColorLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 5, 5), 0, 0));
        ButtonGroup buttonGroup2 = new ButtonGroup();
        buttonGroup2.add(this.glyphCacheRadio);
        buttonGroup2.add(this.sampleTextRadio);
        JPanel rightSidePanel = new JPanel();
        rightSidePanel.setLayout(new GridBagLayout());
        this.getContentPane().add((Component)rightSidePanel, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        JPanel paddingPanel = new JPanel();
        paddingPanel.setLayout(new GridBagLayout());
        rightSidePanel.add((Component)paddingPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        paddingPanel.setBorder(BorderFactory.createTitledBorder("Padding"));
        this.padTopSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
        paddingPanel.add((Component)this.padTopSpinner, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        ((JSpinner.DefaultEditor)this.padTopSpinner.getEditor()).getTextField().setColumns(2);
        this.padRightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
        paddingPanel.add((Component)this.padRightSpinner, new GridBagConstraints(2, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 5), 0, 0));
        ((JSpinner.DefaultEditor)this.padRightSpinner.getEditor()).getTextField().setColumns(2);
        this.padLeftSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
        paddingPanel.add((Component)this.padLeftSpinner, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, 13, 0, new Insets(0, 5, 0, 0), 0, 0));
        ((JSpinner.DefaultEditor)this.padLeftSpinner.getEditor()).getTextField().setColumns(2);
        this.padBottomSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
        paddingPanel.add((Component)this.padBottomSpinner, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        ((JSpinner.DefaultEditor)this.padBottomSpinner.getEditor()).getTextField().setColumns(2);
        JPanel advancePanel = new JPanel();
        FlowLayout advancePanelLayout = new FlowLayout();
        advancePanel.setLayout(advancePanelLayout);
        paddingPanel.add((Component)advancePanel, new GridBagConstraints(0, 4, 3, 1, 1.0, 0.0, 10, 0, new Insets(0, 0, 0, 0), 0, 0));
        advancePanel.add(new JLabel("X:"));
        this.padAdvanceXSpinner = new JSpinner(new SpinnerNumberModel(-2, -999, 999, 1));
        advancePanel.add(this.padAdvanceXSpinner);
        ((JSpinner.DefaultEditor)this.padAdvanceXSpinner.getEditor()).getTextField().setColumns(2);
        advancePanel.add(new JLabel("Y:"));
        this.padAdvanceYSpinner = new JSpinner(new SpinnerNumberModel(-2, -999, 999, 1));
        advancePanel.add(this.padAdvanceYSpinner);
        ((JSpinner.DefaultEditor)this.padAdvanceYSpinner.getEditor()).getTextField().setColumns(2);
        this.effectsPanel = new JPanel();
        this.effectsPanel.setLayout(new GridBagLayout());
        rightSidePanel.add((Component)this.effectsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(5, 0, 5, 5), 0, 0));
        this.effectsPanel.setBorder(BorderFactory.createTitledBorder("Effects"));
        this.effectsPanel.setMinimumSize(new Dimension(210, 1));
        this.effectsScroll = new JScrollPane();
        this.effectsPanel.add((Component)this.effectsScroll, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 11, 2, new Insets(0, 5, 5, 5), 0, 0));
        this.effectsListModel = new DefaultComboBoxModel();
        this.effectsList = new JList();
        this.effectsScroll.setViewportView(this.effectsList);
        this.effectsList.setModel(this.effectsListModel);
        this.effectsList.setVisibleRowCount(7);
        this.effectsScroll.setMinimumSize(this.effectsList.getPreferredScrollableViewportSize());
        this.addEffectButton = new JButton("Add");
        this.effectsPanel.add((Component)this.addEffectButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 5, 6, 5), 0, 0));
        this.addEffectButton.setEnabled(false);
        this.appliedEffectsScroll = new JScrollPane();
        this.effectsPanel.add((Component)this.appliedEffectsScroll, new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, 11, 1, new Insets(0, 0, 5, 0), 0, 0));
        this.appliedEffectsScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.appliedEffectsScroll.setHorizontalScrollBarPolicy(31);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        this.appliedEffectsScroll.setViewportView(panel);
        this.appliedEffectsPanel = new JPanel();
        this.appliedEffectsPanel.setLayout(new GridBagLayout());
        panel.add((Component)this.appliedEffectsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 11, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.appliedEffectsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));
    }

    private void initializeMenus() {
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu();
        menuBar.add(fileMenu);
        fileMenu.setText("File");
        fileMenu.setMnemonic(70);
        this.openMenuItem = new JMenuItem("Open Hiero settings file...");
        this.openMenuItem.setMnemonic(79);
        this.openMenuItem.setAccelerator(KeyStroke.getKeyStroke(79, 2));
        fileMenu.add(this.openMenuItem);
        this.saveMenuItem = new JMenuItem("Save Hiero settings file...");
        this.saveMenuItem.setMnemonic(83);
        this.saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(83, 2));
        fileMenu.add(this.saveMenuItem);
        fileMenu.addSeparator();
        this.saveBMFontMenuItem = new JMenuItem("Save BMFont files (text)...");
        this.saveBMFontMenuItem.setMnemonic(66);
        this.saveBMFontMenuItem.setAccelerator(KeyStroke.getKeyStroke(66, 2));
        fileMenu.add(this.saveBMFontMenuItem);
        fileMenu.addSeparator();
        this.exitMenuItem = new JMenuItem("Exit");
        this.exitMenuItem.setMnemonic(88);
        fileMenu.add(this.exitMenuItem);
    }

    static Icon getColorIcon(Color color) {
        BufferedImage image = new BufferedImage(32, 16, 1);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.fillRect(1, 1, 30, 14);
        g.setColor(Color.black);
        g.drawRect(0, 0, 31, 15);
        return new ImageIcon(image);
    }

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                new Hiero(args);
            }
        });
    }

    static {
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        for (int c : new int[]{0, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 884, 885, 890, 891, 892, 893, 894, 900, 901, 902, 903, 904, 905, 906, 908, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997, 998, 999, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096, 1097, 1098, 1099, 1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156, 1157, 1158, 1159, 1160, 1161, 1162, 1163, 1164, 1165, 1166, 1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176, 1177, 1178, 1179, 1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194, 1195, 1196, 1197, 1198, 1199, 1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216, 1217, 1218, 1219, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228, 1229, 1230, 1231, 1232, 1233, 1234, 1235, 1236, 1237, 1238, 1239, 1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1276, 1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 8192, 8193, 8194, 8195, 8196, 8197, 8198, 8199, 8200, 8201, 8202, 8203, 8204, 8205, 8206, 8207, 8210, 8211, 8212, 8213, 8214, 8215, 8216, 8217, 8218, 8219, 8220, 8221, 8222, 8223, 8224, 8225, 8226, 8230, 8234, 8235, 8236, 8237, 8238, 8239, 8240, 8242, 8243, 8244, 8249, 8250, 8252, 8254, 8260, 8286, 8298, 8299, 8300, 8301, 8302, 8303, 8352, 8353, 8354, 8355, 8356, 8357, 8358, 8359, 8360, 8361, 8363, 8364, 8365, 8366, 8367, 8368, 8369, 8370, 8371, 8372, 8373, 8377, 8378, 11360, 11361, 11362, 11363, 11364, 11365, 11366, 11367, 11368, 11369, 11370, 11371, 11372, 11373, 11377, 11378, 11379, 11380, 11381, 11382, 11383}) {
            if (++i > 26) {
                i = 0;
                buffer.append("\r\n");
            }
            buffer.append((char)c);
        }
        EXTENDED_CHARS = buffer.toString();
    }

    class Renderer
    extends ApplicationAdapter {
        SpriteBatch batch;
        int width;
        int height;

        Renderer() {
        }

        @Override
        public void create() {
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClearDepth(1.0);
            GL11.glDisable(2896);
            this.batch = new SpriteBatch();
            Hiero.this.sampleNeheButton.doClick();
        }

        @Override
        public void resize(int width, int height) {
            this.width = width;
            this.height = height;
            this.batch.getProjectionMatrix().setToOrtho2D(0.0f, 0.0f, width, height);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void render() {
            int viewWidth = Gdx.graphics.getWidth();
            int viewHeight = Gdx.graphics.getHeight();
            if (Hiero.this.sampleTextRadio.isSelected()) {
                GL11.glClearColor(Hiero.this.renderingBackgroundColor.r, Hiero.this.renderingBackgroundColor.g, Hiero.this.renderingBackgroundColor.b, Hiero.this.renderingBackgroundColor.a);
                GL11.glClear(16384);
            } else {
                GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glClear(16384);
            }
            String sampleText = Hiero.this.sampleTextPane.getText();
            GL11.glEnable(3553);
            GL11.glEnableClientState(32888);
            GL11.glEnableClientState(32884);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glViewport(0, 0, this.width, this.height);
            GL11.glScissor(0, 0, this.width, this.height);
            GL11.glMatrixMode(5889);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0, this.width, this.height, 0.0, 1.0, -1.0);
            GL11.glMatrixMode(5888);
            GL11.glLoadIdentity();
            Hiero.this.unicodeFont.addGlyphs(sampleText);
            if (!Hiero.this.unicodeFont.getEffects().isEmpty() && Hiero.this.unicodeFont.loadGlyphs(64)) {
                Hiero.this.glyphPageComboModel.removeAllElements();
                int pageCount = Hiero.this.unicodeFont.getGlyphPages().size();
                int glyphCount = 0;
                for (int i = 0; i < pageCount; ++i) {
                    Hiero.this.glyphPageComboModel.addElement("Page " + (i + 1));
                    glyphCount += ((GlyphPage)Hiero.this.unicodeFont.getGlyphPages().get(i)).getGlyphs().size();
                }
                Hiero.this.glyphPagesTotalLabel.setText(String.valueOf(pageCount));
                Hiero.this.glyphsTotalLabel.setText(String.valueOf(glyphCount));
            }
            if (Hiero.this.sampleTextRadio.isSelected()) {
                int offset = Hiero.this.unicodeFont.getYOffset(sampleText);
                if (offset > 0) {
                    offset = 0;
                }
                Hiero.this.unicodeFont.drawString(10.0f, 12.0f, sampleText, com.badlogic.gdx.graphics.Color.WHITE, 0, sampleText.length());
            } else {
                int index = Hiero.this.glyphPageCombo.getSelectedIndex();
                List pages = Hiero.this.unicodeFont.getGlyphPages();
                if (index >= 0 && index < pages.size()) {
                    Texture texture = ((GlyphPage)pages.get(Hiero.this.glyphPageCombo.getSelectedIndex())).getTexture();
                    GL11.glDisable(3553);
                    GL11.glColor4f(Hiero.this.renderingBackgroundColor.r, Hiero.this.renderingBackgroundColor.g, Hiero.this.renderingBackgroundColor.b, Hiero.this.renderingBackgroundColor.a);
                    GL11.glBegin(7);
                    GL11.glVertex3f(0.0f, 0.0f, 0.0f);
                    GL11.glVertex3f(0.0f, texture.getHeight(), 0.0f);
                    GL11.glVertex3f(texture.getWidth(), texture.getHeight(), 0.0f);
                    GL11.glVertex3f(texture.getWidth(), 0.0f, 0.0f);
                    GL11.glEnd();
                    GL11.glEnable(3553);
                    texture.bind();
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    GL11.glBegin(7);
                    GL11.glTexCoord2f(0.0f, 0.0f);
                    GL11.glVertex3f(0.0f, 0.0f, 0.0f);
                    GL11.glTexCoord2f(0.0f, 1.0f);
                    GL11.glVertex3f(0.0f, texture.getHeight(), 0.0f);
                    GL11.glTexCoord2f(1.0f, 1.0f);
                    GL11.glVertex3f(texture.getWidth(), texture.getHeight(), 0.0f);
                    GL11.glTexCoord2f(1.0f, 0.0f);
                    GL11.glVertex3f(texture.getWidth(), 0.0f, 0.0f);
                    GL11.glEnd();
                }
            }
            GL11.glDisable(3553);
            GL11.glDisableClientState(32888);
            GL11.glDisableClientState(32884);
            if (Hiero.this.saveBmFontFile != null) {
                try {
                    BMFontUtil bmFont = new BMFontUtil(Hiero.this.unicodeFont);
                    bmFont.save(Hiero.this.saveBmFontFile);
                    if (Hiero.this.batchMode) {
                        Hiero.this.exit(0);
                    }
                }
                catch (Throwable ex) {
                    System.out.println("Error saving BMFont files: " + Hiero.this.saveBmFontFile.getAbsolutePath());
                    ex.printStackTrace();
                }
                finally {
                    Hiero.this.saveBmFontFile = null;
                }
            }
        }
    }

    private static class Splash
    extends JWindow {
        final int minMillis;
        final long startTime;

        public Splash(Frame frame, String imageFile, int minMillis) {
            super(frame);
            this.minMillis = minMillis;
            this.getContentPane().add((Component)new JLabel(new ImageIcon(Splash.class.getResource(imageFile))), "Center");
            this.pack();
            this.setLocationRelativeTo(null);
            this.setVisible(true);
            this.startTime = System.currentTimeMillis();
        }

        public void close() {
            final long endTime = System.currentTimeMillis();
            new Thread(new Runnable(){

                @Override
                public void run() {
                    if (endTime - Splash.this.startTime < (long)Splash.this.minMillis) {
                        Splash.this.addMouseListener(new MouseAdapter(){

                            @Override
                            public void mousePressed(MouseEvent evt) {
                                Splash.this.dispose();
                            }
                        });
                        try {
                            Thread.sleep((long)Splash.this.minMillis - (endTime - Splash.this.startTime));
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                        }
                    }
                    EventQueue.invokeLater(new Runnable(){

                        @Override
                        public void run() {
                            Splash.this.dispose();
                        }
                    });
                }
            }, "Splash").start();
        }
    }

    private class EffectPanel
    extends JPanel {
        final Color selectedColor = new Color(11653865);
        final ConfigurableEffect effect;
        List values;
        JButton upButton;
        JButton downButton;
        JButton deleteButton;
        private JPanel valuesPanel;
        JLabel nameLabel;
        GridBagConstraints constrains = new GridBagConstraints(0, -1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0);

        EffectPanel(ConfigurableEffect effect) {
            this.effect = effect;
            Hiero.this.effectPanels.add(this);
            Hiero.this.effectsList.getListSelectionListeners()[0].valueChanged(null);
            this.setLayout(new GridBagLayout());
            this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
            Hiero.this.appliedEffectsPanel.add((Component)this, this.constrains);
            JPanel titlePanel = new JPanel();
            titlePanel.setLayout(new LayoutManager(){

                @Override
                public void removeLayoutComponent(Component comp) {
                }

                @Override
                public Dimension preferredLayoutSize(Container parent) {
                    return null;
                }

                @Override
                public Dimension minimumLayoutSize(Container parent) {
                    return null;
                }

                @Override
                public void layoutContainer(Container parent) {
                    Dimension buttonSize = EffectPanel.this.upButton.getPreferredSize();
                    int upButtonX = EffectPanel.this.getWidth() - buttonSize.width * 3 - 6 - 5;
                    EffectPanel.this.upButton.setBounds(upButtonX, 0, buttonSize.width, buttonSize.height);
                    EffectPanel.this.downButton.setBounds(EffectPanel.this.getWidth() - buttonSize.width * 2 - 3 - 5, 0, buttonSize.width, buttonSize.height);
                    EffectPanel.this.deleteButton.setBounds(EffectPanel.this.getWidth() - buttonSize.width - 5, 0, buttonSize.width, buttonSize.height);
                    Dimension labelSize = EffectPanel.this.nameLabel.getPreferredSize();
                    EffectPanel.this.nameLabel.setBounds(5, buttonSize.height / 2 - labelSize.height / 2, EffectPanel.this.getWidth() - 5, labelSize.height);
                }

                @Override
                public void addLayoutComponent(String name, Component comp) {
                }
            });
            this.upButton = new JButton();
            titlePanel.add(this.upButton);
            this.upButton.setText("Up");
            this.upButton.setMargin(new Insets(0, 0, 0, 0));
            Font font = this.upButton.getFont();
            this.upButton.setFont(new Font(font.getName(), font.getStyle(), font.getSize() - 2));
            this.downButton = new JButton();
            titlePanel.add(this.downButton);
            this.downButton.setText("Down");
            this.downButton.setMargin(new Insets(0, 0, 0, 0));
            font = this.downButton.getFont();
            this.downButton.setFont(new Font(font.getName(), font.getStyle(), font.getSize() - 2));
            this.deleteButton = new JButton();
            titlePanel.add(this.deleteButton);
            this.deleteButton.setText("X");
            this.deleteButton.setMargin(new Insets(0, 0, 0, 0));
            font = this.deleteButton.getFont();
            this.deleteButton.setFont(new Font(font.getName(), font.getStyle(), font.getSize() - 2));
            this.nameLabel = new JLabel(effect.toString());
            titlePanel.add(this.nameLabel);
            font = this.nameLabel.getFont();
            this.nameLabel.setFont(new Font(font.getName(), 1, font.getSize()));
            titlePanel.setPreferredSize(new Dimension(0, Math.max(this.nameLabel.getPreferredSize().height, this.deleteButton.getPreferredSize().height)));
            this.add((Component)titlePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 10, 1, new Insets(5, 0, 0, 5), 0, 0));
            titlePanel.setOpaque(false);
            this.valuesPanel = new JPanel();
            this.valuesPanel.setOpaque(false);
            this.valuesPanel.setLayout(new GridBagLayout());
            this.add((Component)this.valuesPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 10, 5, 0), 0, 0));
            this.upButton.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent evt) {
                    int currentIndex = Hiero.this.effectPanels.indexOf(EffectPanel.this);
                    if (currentIndex > 0) {
                        EffectPanel.this.moveEffect(currentIndex - 1);
                        Hiero.this.updateFont();
                        EffectPanel.this.updateUpDownButtons();
                    }
                }
            });
            this.downButton.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent evt) {
                    int currentIndex = Hiero.this.effectPanels.indexOf(EffectPanel.this);
                    if (currentIndex < Hiero.this.effectPanels.size() - 1) {
                        EffectPanel.this.moveEffect(currentIndex + 1);
                        Hiero.this.updateFont();
                        EffectPanel.this.updateUpDownButtons();
                    }
                }
            });
            this.deleteButton.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent evt) {
                    EffectPanel.this.remove();
                    Hiero.this.updateFont();
                    EffectPanel.this.updateUpDownButtons();
                }
            });
            this.updateValues();
            Hiero.this.updateFont();
            this.updateUpDownButtons();
        }

        public void remove() {
            Hiero.this.effectPanels.remove(this);
            Hiero.this.appliedEffectsPanel.remove(this);
            Hiero.this.getContentPane().validate();
            Hiero.this.effectsList.getListSelectionListeners()[0].valueChanged(null);
        }

        public void updateValues() {
            Hiero.this.prefs.put("foreground", EffectUtil.toString(Hiero.this.colorEffect.getColor()));
            this.valuesPanel.removeAll();
            this.values = this.effect.getValues();
            Iterator iter = this.values.iterator();
            while (iter.hasNext()) {
                this.addValue((ConfigurableEffect.Value)iter.next());
            }
        }

        public void updateUpDownButtons() {
            for (int index = 0; index < Hiero.this.effectPanels.size(); ++index) {
                EffectPanel effectPanel = Hiero.this.effectPanels.get(index);
                if (index == 0) {
                    effectPanel.upButton.setEnabled(false);
                } else {
                    effectPanel.upButton.setEnabled(true);
                }
                if (index == Hiero.this.effectPanels.size() - 1) {
                    effectPanel.downButton.setEnabled(false);
                    continue;
                }
                effectPanel.downButton.setEnabled(true);
            }
        }

        public void moveEffect(int newIndex) {
            Hiero.this.appliedEffectsPanel.remove(this);
            Hiero.this.effectPanels.remove(this);
            Hiero.this.appliedEffectsPanel.add(this, this.constrains, newIndex);
            Hiero.this.effectPanels.add(newIndex, this);
        }

        public void addValue(final ConfigurableEffect.Value value) {
            JLabel valueNameLabel = new JLabel(value.getName() + ":");
            this.valuesPanel.add((Component)valueNameLabel, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 5), 0, 0));
            final JLabel valueValueLabel = new JLabel();
            this.valuesPanel.add((Component)valueValueLabel, new GridBagConstraints(1, -1, 1, 1, 1.0, 0.0, 17, 1, new Insets(0, 0, 0, 5), 0, 0));
            valueValueLabel.setOpaque(true);
            if (value.getObject() instanceof Color) {
                valueValueLabel.setIcon(Hiero.getColorIcon((Color)value.getObject()));
            } else {
                valueValueLabel.setText(value.toString());
            }
            valueValueLabel.addMouseListener(new MouseAdapter(){

                @Override
                public void mouseEntered(MouseEvent evt) {
                    valueValueLabel.setBackground(EffectPanel.this.selectedColor);
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    valueValueLabel.setBackground(null);
                }

                @Override
                public void mouseClicked(MouseEvent evt) {
                    Object oldObject = value.getObject();
                    value.showDialog();
                    if (!value.getObject().equals(oldObject)) {
                        EffectPanel.this.effect.setValues(EffectPanel.this.values);
                        EffectPanel.this.updateValues();
                        Hiero.this.updateFont();
                    }
                }
            });
        }

        public ConfigurableEffect getEffect() {
            return this.effect;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            EffectPanel other = (EffectPanel)obj;
            return !(this.effect == null ? other.effect != null : !this.effect.equals(other.effect));
        }
    }
}

