/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Pool;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public final class LwjglInput
implements Input {
    public static float keyRepeatInitialTime = 0.4f;
    public static float keyRepeatTime = 0.1f;
    List<KeyEvent> keyEvents = new ArrayList<KeyEvent>();
    List<TouchEvent> touchEvents = new ArrayList<TouchEvent>();
    boolean mousePressed = false;
    int mouseX;
    int mouseY;
    int deltaX;
    int deltaY;
    int pressedKeys = 0;
    boolean keyJustPressed = false;
    boolean[] justPressedKeys = new boolean[256];
    boolean justTouched = false;
    IntSet pressedButtons = new IntSet();
    InputProcessor processor;
    char lastKeyCharPressed;
    float keyRepeatTimer;
    long currentEventTimeStamp;
    float deltaTime;
    long lastTime;
    Pool<KeyEvent> usedKeyEvents = new Pool<KeyEvent>(16, 1000){

        @Override
        protected KeyEvent newObject() {
            return new KeyEvent();
        }
    };
    Pool<TouchEvent> usedTouchEvents = new Pool<TouchEvent>(16, 1000){

        @Override
        protected TouchEvent newObject() {
            return new TouchEvent();
        }
    };

    public LwjglInput() {
        Keyboard.enableRepeatEvents(false);
        Mouse.setClipMouseCoordinatesToWindow(false);
    }

    @Override
    public float getAccelerometerX() {
        return 0.0f;
    }

    @Override
    public float getAccelerometerY() {
        return 0.0f;
    }

    @Override
    public float getAccelerometerZ() {
        return 0.0f;
    }

    @Override
    public float getGyroscopeX() {
        return 0.0f;
    }

    @Override
    public float getGyroscopeY() {
        return 0.0f;
    }

    @Override
    public float getGyroscopeZ() {
        return 0.0f;
    }

    @Override
    public void getTextInput(final Input.TextInputListener listener, final String title, final String text, final String hint) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                JPanel panel = new JPanel(new FlowLayout());
                JPanel textPanel = new JPanel(){

                    @Override
                    public boolean isOptimizedDrawingEnabled() {
                        return false;
                    }
                };
                textPanel.setLayout(new OverlayLayout(textPanel));
                panel.add(textPanel);
                final JTextField textField = new JTextField(20);
                textField.setText(text);
                textField.setAlignmentX(0.0f);
                textPanel.add(textField);
                final JLabel placeholderLabel = new JLabel(hint);
                placeholderLabel.setForeground(Color.GRAY);
                placeholderLabel.setAlignmentX(0.0f);
                textPanel.add((Component)placeholderLabel, 0);
                textField.getDocument().addDocumentListener(new DocumentListener(){

                    @Override
                    public void removeUpdate(DocumentEvent arg0) {
                        this.updated();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent arg0) {
                        this.updated();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent arg0) {
                        this.updated();
                    }

                    private void updated() {
                        if (textField.getText().length() == 0) {
                            placeholderLabel.setVisible(true);
                        } else {
                            placeholderLabel.setVisible(false);
                        }
                    }
                });
                JOptionPane pane = new JOptionPane(panel, 3, 2, null, null, null);
                pane.setInitialValue(null);
                pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
                Border border = textField.getBorder();
                placeholderLabel.setBorder(new EmptyBorder(border.getBorderInsets(textField)));
                JDialog dialog = pane.createDialog(null, title);
                pane.selectInitialValue();
                dialog.addWindowFocusListener(new WindowFocusListener(){

                    @Override
                    public void windowLostFocus(WindowEvent arg0) {
                    }

                    @Override
                    public void windowGainedFocus(WindowEvent arg0) {
                        textField.requestFocusInWindow();
                    }
                });
                dialog.setVisible(true);
                dialog.dispose();
                Object selectedValue = pane.getValue();
                if (selectedValue != null && selectedValue instanceof Integer && (Integer)selectedValue == 0) {
                    listener.input(textField.getText());
                } else {
                    listener.canceled();
                }
            }
        });
    }

    @Override
    public int getX() {
        return (int)((float)Mouse.getX() * Display.getPixelScaleFactor());
    }

    @Override
    public int getY() {
        return Gdx.graphics.getHeight() - 1 - (int)((float)Mouse.getY() * Display.getPixelScaleFactor());
    }

    public boolean isAccelerometerAvailable() {
        return false;
    }

    public boolean isGyroscopeAvailable() {
        return false;
    }

    @Override
    public boolean isKeyPressed(int key) {
        if (!Keyboard.isCreated()) {
            return false;
        }
        if (key == -1) {
            return this.pressedKeys > 0;
        }
        return Keyboard.isKeyDown(LwjglInput.getLwjglKeyCode(key));
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        if (key == -1) {
            return this.keyJustPressed;
        }
        if (key < 0 || key > 255) {
            return false;
        }
        return this.justPressedKeys[key];
    }

    @Override
    public boolean isTouched() {
        boolean button = Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2);
        return button;
    }

    @Override
    public int getX(int pointer) {
        if (pointer > 0) {
            return 0;
        }
        return this.getX();
    }

    @Override
    public int getY(int pointer) {
        if (pointer > 0) {
            return 0;
        }
        return this.getY();
    }

    @Override
    public boolean isTouched(int pointer) {
        if (pointer > 0) {
            return false;
        }
        return this.isTouched();
    }

    public boolean supportsMultitouch() {
        return false;
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) {
    }

    @Override
    public void setCatchBackKey(boolean catchBack) {
    }

    @Override
    public boolean isCatchBackKey() {
        return false;
    }

    @Override
    public void setCatchMenuKey(boolean catchMenu) {
    }

    @Override
    public boolean isCatchMenuKey() {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void processEvents() {
        LwjglInput lwjglInput = this;
        synchronized (lwjglInput) {
            if (this.processor != null) {
                Object e;
                int i;
                InputProcessor processor = this.processor;
                int len = this.keyEvents.size();
                for (i = 0; i < len; ++i) {
                    e = this.keyEvents.get(i);
                    this.currentEventTimeStamp = ((KeyEvent)e).timeStamp;
                    switch (((KeyEvent)e).type) {
                        case 0: {
                            processor.keyDown(((KeyEvent)e).keyCode);
                            break;
                        }
                        case 1: {
                            processor.keyUp(((KeyEvent)e).keyCode);
                            break;
                        }
                        case 2: {
                            processor.keyTyped(((KeyEvent)e).keyChar);
                        }
                    }
                    this.usedKeyEvents.free((KeyEvent)e);
                }
                len = this.touchEvents.size();
                for (i = 0; i < len; ++i) {
                    e = this.touchEvents.get(i);
                    this.currentEventTimeStamp = ((TouchEvent)e).timeStamp;
                    switch (((TouchEvent)e).type) {
                        case 0: {
                            processor.touchDown(((TouchEvent)e).x, ((TouchEvent)e).y, ((TouchEvent)e).pointer, ((TouchEvent)e).button);
                            break;
                        }
                        case 1: {
                            processor.touchUp(((TouchEvent)e).x, ((TouchEvent)e).y, ((TouchEvent)e).pointer, ((TouchEvent)e).button);
                            break;
                        }
                        case 2: {
                            processor.touchDragged(((TouchEvent)e).x, ((TouchEvent)e).y, ((TouchEvent)e).pointer);
                            break;
                        }
                        case 4: {
                            processor.mouseMoved(((TouchEvent)e).x, ((TouchEvent)e).y);
                            break;
                        }
                        case 3: {
                            processor.scrolled(((TouchEvent)e).scrollAmount);
                        }
                    }
                    this.usedTouchEvents.free((TouchEvent)e);
                }
            } else {
                int i;
                int len = this.touchEvents.size();
                for (i = 0; i < len; ++i) {
                    this.usedTouchEvents.free(this.touchEvents.get(i));
                }
                len = this.keyEvents.size();
                for (i = 0; i < len; ++i) {
                    this.usedKeyEvents.free(this.keyEvents.get(i));
                }
            }
            this.keyEvents.clear();
            this.touchEvents.clear();
        }
    }

    public static int getGdxKeyCode(int lwjglKeyCode) {
        switch (lwjglKeyCode) {
            case 26: {
                return 71;
            }
            case 27: {
                return 72;
            }
            case 41: {
                return 68;
            }
            case 55: {
                return 17;
            }
            case 69: {
                return 78;
            }
            case 83: {
                return 56;
            }
            case 181: {
                return 76;
            }
            case 219: {
                return 63;
            }
            case 220: {
                return 63;
            }
            case 141: {
                return 70;
            }
            case 145: {
                return 77;
            }
            case 13: {
                return 70;
            }
            case 179: {
                return 55;
            }
            case 156: {
                return 66;
            }
            case 11: {
                return 7;
            }
            case 2: {
                return 8;
            }
            case 3: {
                return 9;
            }
            case 4: {
                return 10;
            }
            case 5: {
                return 11;
            }
            case 6: {
                return 12;
            }
            case 7: {
                return 13;
            }
            case 8: {
                return 14;
            }
            case 9: {
                return 15;
            }
            case 10: {
                return 16;
            }
            case 30: {
                return 29;
            }
            case 48: {
                return 30;
            }
            case 46: {
                return 31;
            }
            case 32: {
                return 32;
            }
            case 18: {
                return 33;
            }
            case 33: {
                return 34;
            }
            case 34: {
                return 35;
            }
            case 35: {
                return 36;
            }
            case 23: {
                return 37;
            }
            case 36: {
                return 38;
            }
            case 37: {
                return 39;
            }
            case 38: {
                return 40;
            }
            case 50: {
                return 41;
            }
            case 49: {
                return 42;
            }
            case 24: {
                return 43;
            }
            case 25: {
                return 44;
            }
            case 16: {
                return 45;
            }
            case 19: {
                return 46;
            }
            case 31: {
                return 47;
            }
            case 20: {
                return 48;
            }
            case 22: {
                return 49;
            }
            case 47: {
                return 50;
            }
            case 17: {
                return 51;
            }
            case 45: {
                return 52;
            }
            case 21: {
                return 53;
            }
            case 44: {
                return 54;
            }
            case 56: {
                return 57;
            }
            case 184: {
                return 58;
            }
            case 43: {
                return 73;
            }
            case 51: {
                return 55;
            }
            case 211: {
                return 112;
            }
            case 203: {
                return 21;
            }
            case 205: {
                return 22;
            }
            case 200: {
                return 19;
            }
            case 208: {
                return 20;
            }
            case 28: {
                return 66;
            }
            case 199: {
                return 3;
            }
            case 12: {
                return 69;
            }
            case 52: {
                return 56;
            }
            case 78: {
                return 81;
            }
            case 39: {
                return 74;
            }
            case 42: {
                return 59;
            }
            case 54: {
                return 60;
            }
            case 53: {
                return 76;
            }
            case 57: {
                return 62;
            }
            case 15: {
                return 61;
            }
            case 29: {
                return 129;
            }
            case 157: {
                return 130;
            }
            case 209: {
                return 93;
            }
            case 201: {
                return 92;
            }
            case 1: {
                return 131;
            }
            case 207: {
                return 132;
            }
            case 210: {
                return 133;
            }
            case 14: {
                return 67;
            }
            case 74: {
                return 69;
            }
            case 40: {
                return 75;
            }
            case 59: {
                return 244;
            }
            case 60: {
                return 245;
            }
            case 61: {
                return 246;
            }
            case 62: {
                return 247;
            }
            case 63: {
                return 248;
            }
            case 64: {
                return 249;
            }
            case 65: {
                return 250;
            }
            case 66: {
                return 251;
            }
            case 67: {
                return 252;
            }
            case 68: {
                return 253;
            }
            case 87: {
                return 254;
            }
            case 88: {
                return 255;
            }
            case 146: {
                return 243;
            }
            case 82: {
                return 144;
            }
            case 79: {
                return 145;
            }
            case 80: {
                return 146;
            }
            case 81: {
                return 147;
            }
            case 75: {
                return 148;
            }
            case 76: {
                return 149;
            }
            case 77: {
                return 150;
            }
            case 71: {
                return 151;
            }
            case 72: {
                return 152;
            }
            case 73: {
                return 153;
            }
        }
        return 0;
    }

    public static int getLwjglKeyCode(int gdxKeyCode) {
        switch (gdxKeyCode) {
            case 75: {
                return 40;
            }
            case 71: {
                return 26;
            }
            case 72: {
                return 27;
            }
            case 68: {
                return 41;
            }
            case 17: {
                return 55;
            }
            case 78: {
                return 69;
            }
            case 77: {
                return 145;
            }
            case 70: {
                return 13;
            }
            case 63: {
                return 219;
            }
            case 7: {
                return 11;
            }
            case 8: {
                return 2;
            }
            case 9: {
                return 3;
            }
            case 10: {
                return 4;
            }
            case 11: {
                return 5;
            }
            case 12: {
                return 6;
            }
            case 13: {
                return 7;
            }
            case 14: {
                return 8;
            }
            case 15: {
                return 9;
            }
            case 16: {
                return 10;
            }
            case 29: {
                return 30;
            }
            case 30: {
                return 48;
            }
            case 31: {
                return 46;
            }
            case 32: {
                return 32;
            }
            case 33: {
                return 18;
            }
            case 34: {
                return 33;
            }
            case 35: {
                return 34;
            }
            case 36: {
                return 35;
            }
            case 37: {
                return 23;
            }
            case 38: {
                return 36;
            }
            case 39: {
                return 37;
            }
            case 40: {
                return 38;
            }
            case 41: {
                return 50;
            }
            case 42: {
                return 49;
            }
            case 43: {
                return 24;
            }
            case 44: {
                return 25;
            }
            case 45: {
                return 16;
            }
            case 46: {
                return 19;
            }
            case 47: {
                return 31;
            }
            case 48: {
                return 20;
            }
            case 49: {
                return 22;
            }
            case 50: {
                return 47;
            }
            case 51: {
                return 17;
            }
            case 52: {
                return 45;
            }
            case 53: {
                return 21;
            }
            case 54: {
                return 44;
            }
            case 57: {
                return 56;
            }
            case 58: {
                return 184;
            }
            case 73: {
                return 43;
            }
            case 55: {
                return 51;
            }
            case 112: {
                return 211;
            }
            case 21: {
                return 203;
            }
            case 22: {
                return 205;
            }
            case 19: {
                return 200;
            }
            case 20: {
                return 208;
            }
            case 66: {
                return 28;
            }
            case 3: {
                return 199;
            }
            case 132: {
                return 207;
            }
            case 93: {
                return 209;
            }
            case 92: {
                return 201;
            }
            case 133: {
                return 210;
            }
            case 69: {
                return 12;
            }
            case 56: {
                return 52;
            }
            case 81: {
                return 78;
            }
            case 74: {
                return 39;
            }
            case 59: {
                return 42;
            }
            case 60: {
                return 54;
            }
            case 76: {
                return 53;
            }
            case 62: {
                return 57;
            }
            case 61: {
                return 15;
            }
            case 67: {
                return 14;
            }
            case 129: {
                return 29;
            }
            case 130: {
                return 157;
            }
            case 131: {
                return 1;
            }
            case 244: {
                return 59;
            }
            case 245: {
                return 60;
            }
            case 246: {
                return 61;
            }
            case 247: {
                return 62;
            }
            case 248: {
                return 63;
            }
            case 249: {
                return 64;
            }
            case 250: {
                return 65;
            }
            case 251: {
                return 66;
            }
            case 252: {
                return 67;
            }
            case 253: {
                return 68;
            }
            case 254: {
                return 87;
            }
            case 255: {
                return 88;
            }
            case 243: {
                return 146;
            }
            case 144: {
                return 82;
            }
            case 145: {
                return 79;
            }
            case 146: {
                return 80;
            }
            case 147: {
                return 81;
            }
            case 148: {
                return 75;
            }
            case 149: {
                return 76;
            }
            case 150: {
                return 77;
            }
            case 151: {
                return 71;
            }
            case 152: {
                return 72;
            }
            case 153: {
                return 73;
            }
        }
        return 0;
    }

    public void update() {
        this.updateTime();
        this.updateMouse();
        this.updateKeyboard();
    }

    private int toGdxButton(int button) {
        if (button == 0) {
            return 0;
        }
        if (button == 1) {
            return 1;
        }
        if (button == 2) {
            return 2;
        }
        if (button == 3) {
            return 3;
        }
        if (button == 4) {
            return 4;
        }
        return -1;
    }

    void updateTime() {
        long thisTime = System.nanoTime();
        this.deltaTime = (float)(thisTime - this.lastTime) / 1.0E9f;
        this.lastTime = thisTime;
    }

    void updateMouse() {
        this.justTouched = false;
        if (Mouse.isCreated()) {
            int events = 0;
            while (Mouse.next()) {
                ++events;
                int x = (int)((float)Mouse.getEventX() * Display.getPixelScaleFactor());
                int y = Gdx.graphics.getHeight() - (int)((float)Mouse.getEventY() * Display.getPixelScaleFactor()) - 1;
                int button = Mouse.getEventButton();
                int gdxButton = this.toGdxButton(button);
                if (button != -1 && gdxButton == -1) continue;
                TouchEvent event = this.usedTouchEvents.obtain();
                event.x = x;
                event.y = y;
                event.button = gdxButton;
                event.pointer = 0;
                event.timeStamp = Mouse.getEventNanoseconds();
                if (button == -1) {
                    if (Mouse.getEventDWheel() != 0) {
                        event.type = 3;
                        event.scrollAmount = (int)(-Math.signum(Mouse.getEventDWheel()));
                    } else {
                        event.type = this.pressedButtons.size > 0 ? 2 : 4;
                    }
                } else if (Mouse.getEventButtonState()) {
                    event.type = 0;
                    this.pressedButtons.add(event.button);
                    this.justTouched = true;
                } else {
                    event.type = 1;
                    this.pressedButtons.remove(event.button);
                }
                this.touchEvents.add(event);
                this.mouseX = event.x;
                this.mouseY = event.y;
                this.deltaX = (int)((float)Mouse.getEventDX() * Display.getPixelScaleFactor());
                this.deltaY = (int)((float)Mouse.getEventDY() * Display.getPixelScaleFactor());
            }
            if (events == 0) {
                this.deltaX = 0;
                this.deltaY = 0;
            } else {
                Gdx.graphics.requestRendering();
            }
        }
    }

    void updateKeyboard() {
        if (this.keyJustPressed) {
            this.keyJustPressed = false;
            for (int i = 0; i < this.justPressedKeys.length; ++i) {
                this.justPressedKeys[i] = false;
            }
        }
        if (this.lastKeyCharPressed != '\u0000') {
            this.keyRepeatTimer -= this.deltaTime;
            if (this.keyRepeatTimer < 0.0f) {
                this.keyRepeatTimer = keyRepeatTime;
                KeyEvent event = this.usedKeyEvents.obtain();
                event.keyCode = 0;
                event.keyChar = this.lastKeyCharPressed;
                event.type = 2;
                event.timeStamp = System.nanoTime();
                this.keyEvents.add(event);
                Gdx.graphics.requestRendering();
            }
        }
        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                int keyCode = LwjglInput.getGdxKeyCode(Keyboard.getEventKey());
                int keyChar = Keyboard.getEventCharacter();
                if (Keyboard.getEventKeyState() || keyCode == 0 && keyChar != 0 && Character.isDefined((char)keyChar)) {
                    KeyEvent event;
                    long timeStamp = Keyboard.getEventNanoseconds();
                    switch (keyCode) {
                        case 67: {
                            keyChar = 8;
                            break;
                        }
                        case 112: {
                            keyChar = 127;
                        }
                    }
                    if (keyCode != 0) {
                        event = this.usedKeyEvents.obtain();
                        event.keyCode = keyCode;
                        event.keyChar = '\u0000';
                        event.type = 0;
                        event.timeStamp = timeStamp;
                        this.keyEvents.add(event);
                        ++this.pressedKeys;
                        this.keyJustPressed = true;
                        this.justPressedKeys[keyCode] = true;
                        this.lastKeyCharPressed = (char)keyChar;
                        this.keyRepeatTimer = keyRepeatInitialTime;
                    }
                    event = this.usedKeyEvents.obtain();
                    event.keyCode = 0;
                    event.keyChar = (char)keyChar;
                    event.type = 2;
                    event.timeStamp = timeStamp;
                    this.keyEvents.add(event);
                } else {
                    KeyEvent event = this.usedKeyEvents.obtain();
                    event.keyCode = keyCode;
                    event.keyChar = '\u0000';
                    event.type = 1;
                    event.timeStamp = Keyboard.getEventNanoseconds();
                    this.keyEvents.add(event);
                    --this.pressedKeys;
                    this.lastKeyCharPressed = '\u0000';
                }
                Gdx.graphics.requestRendering();
            }
        }
    }

    @Override
    public void setInputProcessor(InputProcessor processor) {
        this.processor = processor;
    }

    @Override
    public InputProcessor getInputProcessor() {
        return this.processor;
    }

    @Override
    public void vibrate(int milliseconds) {
    }

    @Override
    public boolean justTouched() {
        return this.justTouched;
    }

    private int toLwjglButton(int button) {
        switch (button) {
            case 0: {
                return 0;
            }
            case 1: {
                return 1;
            }
            case 2: {
                return 2;
            }
            case 3: {
                return 3;
            }
            case 4: {
                return 4;
            }
        }
        return 0;
    }

    @Override
    public boolean isButtonPressed(int button) {
        return Mouse.isButtonDown(this.toLwjglButton(button));
    }

    @Override
    public void vibrate(long[] pattern, int repeat) {
    }

    @Override
    public void cancelVibrate() {
    }

    @Override
    public float getAzimuth() {
        return 0.0f;
    }

    @Override
    public float getPitch() {
        return 0.0f;
    }

    @Override
    public float getRoll() {
        return 0.0f;
    }

    @Override
    public boolean isPeripheralAvailable(Input.Peripheral peripheral) {
        return peripheral == Input.Peripheral.HardwareKeyboard;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public Input.Orientation getNativeOrientation() {
        return Input.Orientation.Landscape;
    }

    @Override
    public void setCursorCatched(boolean catched) {
        Mouse.setGrabbed(catched);
    }

    @Override
    public boolean isCursorCatched() {
        return Mouse.isGrabbed();
    }

    @Override
    public int getDeltaX() {
        return this.deltaX;
    }

    @Override
    public int getDeltaX(int pointer) {
        if (pointer == 0) {
            return this.deltaX;
        }
        return 0;
    }

    @Override
    public int getDeltaY() {
        return -this.deltaY;
    }

    @Override
    public int getDeltaY(int pointer) {
        if (pointer == 0) {
            return -this.deltaY;
        }
        return 0;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        Mouse.setCursorPosition(x, Gdx.graphics.getHeight() - 1 - y);
    }

    @Override
    public long getCurrentEventTime() {
        return this.currentEventTimeStamp;
    }

    @Override
    public void getRotationMatrix(float[] matrix) {
    }

    class TouchEvent {
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_SCROLLED = 3;
        static final int TOUCH_MOVED = 4;
        long timeStamp;
        int type;
        int x;
        int y;
        int scrollAmount;
        int button;
        int pointer;

        TouchEvent() {
        }
    }

    class KeyEvent {
        static final int KEY_DOWN = 0;
        static final int KEY_UP = 1;
        static final int KEY_TYPED = 2;
        long timeStamp;
        int type;
        int keyCode;
        char keyChar;

        KeyEvent() {
        }
    }
}

