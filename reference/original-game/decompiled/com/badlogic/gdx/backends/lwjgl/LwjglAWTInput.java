/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Pool;
import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
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

public class LwjglAWTInput
implements Input,
MouseMotionListener,
MouseListener,
MouseWheelListener,
KeyListener {
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
    private final LwjglAWTCanvas lwjglAwtCanvas;
    List<KeyEvent> keyEvents = new ArrayList<KeyEvent>();
    List<TouchEvent> touchEvents = new ArrayList<TouchEvent>();
    int touchX = 0;
    int touchY = 0;
    int deltaX = 0;
    int deltaY = 0;
    boolean touchDown = false;
    boolean justTouched = false;
    int keyCount = 0;
    boolean[] keys = new boolean[256];
    boolean keyJustPressed = false;
    boolean[] justPressedKeys = new boolean[256];
    IntSet pressedButtons = new IntSet();
    InputProcessor processor;
    Canvas canvas;
    boolean catched = false;
    Robot robot = null;
    long currentEventTimeStamp;

    public LwjglAWTInput(LwjglAWTCanvas lwjglAwtCanvas) {
        this.lwjglAwtCanvas = lwjglAwtCanvas;
        this.setListeners(lwjglAwtCanvas.getCanvas());
        try {
            this.robot = new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
        }
        catch (HeadlessException headlessException) {
        }
        catch (AWTException aWTException) {
            // empty catch block
        }
    }

    public void setListeners(Canvas canvas) {
        if (this.canvas != null) {
            canvas.removeMouseListener(this);
            canvas.removeMouseMotionListener(this);
            canvas.removeMouseWheelListener(this);
            canvas.removeKeyListener(this);
        }
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
        canvas.setFocusTraversalKeysEnabled(false);
        this.canvas = canvas;
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
        return this.touchX;
    }

    @Override
    public int getX(int pointer) {
        if (pointer == 0) {
            return this.touchX;
        }
        return 0;
    }

    @Override
    public int getY() {
        return this.touchY;
    }

    @Override
    public int getY(int pointer) {
        if (pointer == 0) {
            return this.touchY;
        }
        return 0;
    }

    @Override
    public synchronized boolean isKeyPressed(int key) {
        if (key == -1) {
            return this.keyCount > 0;
        }
        if (key < 0 || key > 255) {
            return false;
        }
        return this.keys[key];
    }

    @Override
    public synchronized boolean isKeyJustPressed(int key) {
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
        return this.touchDown;
    }

    @Override
    public boolean isTouched(int pointer) {
        if (pointer == 0) {
            return this.touchDown;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void processEvents() {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            this.justTouched = false;
            if (this.keyJustPressed) {
                this.keyJustPressed = false;
                for (int i = 0; i < this.justPressedKeys.length; ++i) {
                    this.justPressedKeys[i] = false;
                }
            }
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
                            this.keyJustPressed = true;
                            this.justPressedKeys[((KeyEvent)e).keyCode] = true;
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
                            this.justTouched = true;
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
                        case 3: {
                            processor.mouseMoved(((TouchEvent)e).x, ((TouchEvent)e).y);
                            break;
                        }
                        case 4: {
                            processor.scrolled(((TouchEvent)e).scrollAmount);
                        }
                    }
                    this.usedTouchEvents.free((TouchEvent)e);
                }
            } else {
                int i;
                int len = this.touchEvents.size();
                for (i = 0; i < len; ++i) {
                    TouchEvent event = this.touchEvents.get(i);
                    if (event.type == 0) {
                        this.justTouched = true;
                    }
                    this.usedTouchEvents.free(event);
                }
                len = this.keyEvents.size();
                for (i = 0; i < len; ++i) {
                    this.usedKeyEvents.free(this.keyEvents.get(i));
                }
            }
            if (this.touchEvents.size() == 0) {
                this.deltaX = 0;
                this.deltaY = 0;
            }
            this.keyEvents.clear();
            this.touchEvents.clear();
        }
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

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            TouchEvent event = this.usedTouchEvents.obtain();
            event.pointer = 0;
            event.x = e.getX();
            event.y = e.getY();
            event.type = 2;
            event.timeStamp = System.nanoTime();
            this.touchEvents.add(event);
            this.deltaX = event.x - this.touchX;
            this.deltaY = event.y - this.touchY;
            this.touchX = event.x;
            this.touchY = event.y;
            this.checkCatched(e);
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            TouchEvent event = this.usedTouchEvents.obtain();
            event.pointer = 0;
            event.x = e.getX();
            event.y = e.getY();
            event.type = 3;
            event.timeStamp = System.nanoTime();
            this.touchEvents.add(event);
            this.deltaX = event.x - this.touchX;
            this.deltaY = event.y - this.touchY;
            this.touchX = event.x;
            this.touchY = event.y;
            this.checkCatched(e);
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        this.touchX = e.getX();
        this.touchY = e.getY();
        this.checkCatched(e);
        this.lwjglAwtCanvas.graphics.requestRendering();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.checkCatched(e);
        this.lwjglAwtCanvas.graphics.requestRendering();
    }

    private void checkCatched(MouseEvent e) {
        if (this.catched && this.robot != null && this.canvas.isShowing()) {
            int x = Math.max(0, Math.min(e.getX(), this.canvas.getWidth()) - 1) + this.canvas.getLocationOnScreen().x;
            int y = Math.max(0, Math.min(e.getY(), this.canvas.getHeight()) - 1) + this.canvas.getLocationOnScreen().y;
            if (e.getX() < 0 || e.getX() >= this.canvas.getWidth() || e.getY() < 0 || e.getY() >= this.canvas.getHeight()) {
                this.robot.mouseMove(x, y);
            }
        }
    }

    private int toGdxButton(int swingButton) {
        if (swingButton == 1) {
            return 0;
        }
        if (swingButton == 2) {
            return 2;
        }
        if (swingButton == 3) {
            return 1;
        }
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            TouchEvent event = this.usedTouchEvents.obtain();
            event.pointer = 0;
            event.x = e.getX();
            event.y = e.getY();
            event.type = 0;
            event.button = this.toGdxButton(e.getButton());
            event.timeStamp = System.nanoTime();
            this.touchEvents.add(event);
            this.deltaX = event.x - this.touchX;
            this.deltaY = event.y - this.touchY;
            this.touchX = event.x;
            this.touchY = event.y;
            this.touchDown = true;
            this.pressedButtons.add(event.button);
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            TouchEvent event = this.usedTouchEvents.obtain();
            event.pointer = 0;
            event.x = e.getX();
            event.y = e.getY();
            event.button = this.toGdxButton(e.getButton());
            event.type = 1;
            event.timeStamp = System.nanoTime();
            this.touchEvents.add(event);
            this.deltaX = event.x - this.touchX;
            this.deltaY = event.y - this.touchY;
            this.touchX = event.x;
            this.touchY = event.y;
            this.pressedButtons.remove(event.button);
            if (this.pressedButtons.size == 0) {
                this.touchDown = false;
            }
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            TouchEvent event = this.usedTouchEvents.obtain();
            event.pointer = 0;
            event.type = 4;
            event.scrollAmount = e.getWheelRotation();
            event.timeStamp = System.nanoTime();
            this.touchEvents.add(event);
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            KeyEvent event = this.usedKeyEvents.obtain();
            event.keyChar = '\u0000';
            event.keyCode = LwjglAWTInput.translateKeyCode(e.getKeyCode());
            event.type = 0;
            event.timeStamp = System.nanoTime();
            this.keyEvents.add(event);
            if (!this.keys[event.keyCode]) {
                ++this.keyCount;
                this.keys[event.keyCode] = true;
            }
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            KeyEvent event = this.usedKeyEvents.obtain();
            event.keyChar = '\u0000';
            event.keyCode = LwjglAWTInput.translateKeyCode(e.getKeyCode());
            event.type = 1;
            event.timeStamp = System.nanoTime();
            this.keyEvents.add(event);
            if (this.keys[event.keyCode]) {
                --this.keyCount;
                this.keys[event.keyCode] = false;
            }
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            KeyEvent event = this.usedKeyEvents.obtain();
            event.keyChar = e.getKeyChar();
            event.keyCode = 0;
            event.type = 2;
            event.timeStamp = System.nanoTime();
            this.keyEvents.add(event);
            this.lwjglAwtCanvas.graphics.requestRendering();
        }
    }

    protected static int translateKeyCode(int keyCode) {
        switch (keyCode) {
            case 107: {
                return 81;
            }
            case 109: {
                return 69;
            }
            case 48: {
                return 7;
            }
            case 49: {
                return 8;
            }
            case 50: {
                return 9;
            }
            case 51: {
                return 10;
            }
            case 52: {
                return 11;
            }
            case 53: {
                return 12;
            }
            case 54: {
                return 13;
            }
            case 55: {
                return 14;
            }
            case 56: {
                return 15;
            }
            case 57: {
                return 16;
            }
            case 65: {
                return 29;
            }
            case 66: {
                return 30;
            }
            case 67: {
                return 31;
            }
            case 68: {
                return 32;
            }
            case 69: {
                return 33;
            }
            case 70: {
                return 34;
            }
            case 71: {
                return 35;
            }
            case 72: {
                return 36;
            }
            case 73: {
                return 37;
            }
            case 74: {
                return 38;
            }
            case 75: {
                return 39;
            }
            case 76: {
                return 40;
            }
            case 77: {
                return 41;
            }
            case 78: {
                return 42;
            }
            case 79: {
                return 43;
            }
            case 80: {
                return 44;
            }
            case 81: {
                return 45;
            }
            case 82: {
                return 46;
            }
            case 83: {
                return 47;
            }
            case 84: {
                return 48;
            }
            case 85: {
                return 49;
            }
            case 86: {
                return 50;
            }
            case 87: {
                return 51;
            }
            case 88: {
                return 52;
            }
            case 89: {
                return 53;
            }
            case 90: {
                return 54;
            }
            case 18: {
                return 57;
            }
            case 65406: {
                return 58;
            }
            case 92: {
                return 73;
            }
            case 44: {
                return 55;
            }
            case 127: {
                return 112;
            }
            case 37: {
                return 21;
            }
            case 39: {
                return 22;
            }
            case 38: {
                return 19;
            }
            case 40: {
                return 20;
            }
            case 10: {
                return 66;
            }
            case 36: {
                return 3;
            }
            case 45: {
                return 69;
            }
            case 46: {
                return 56;
            }
            case 521: {
                return 81;
            }
            case 59: {
                return 74;
            }
            case 16: {
                return 59;
            }
            case 47: {
                return 76;
            }
            case 32: {
                return 62;
            }
            case 9: {
                return 61;
            }
            case 8: {
                return 67;
            }
            case 17: {
                return 129;
            }
            case 27: {
                return 131;
            }
            case 35: {
                return 132;
            }
            case 155: {
                return 133;
            }
            case 33: {
                return 92;
            }
            case 34: {
                return 93;
            }
            case 112: {
                return 244;
            }
            case 113: {
                return 245;
            }
            case 114: {
                return 246;
            }
            case 115: {
                return 247;
            }
            case 116: {
                return 248;
            }
            case 117: {
                return 249;
            }
            case 118: {
                return 250;
            }
            case 119: {
                return 251;
            }
            case 120: {
                return 252;
            }
            case 121: {
                return 253;
            }
            case 122: {
                return 254;
            }
            case 123: {
                return 255;
            }
            case 513: {
                return 243;
            }
            case 96: {
                return 7;
            }
            case 97: {
                return 8;
            }
            case 98: {
                return 9;
            }
            case 99: {
                return 10;
            }
            case 100: {
                return 11;
            }
            case 101: {
                return 12;
            }
            case 102: {
                return 13;
            }
            case 103: {
                return 14;
            }
            case 104: {
                return 15;
            }
            case 105: {
                return 16;
            }
        }
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void setInputProcessor(InputProcessor processor) {
        LwjglAWTInput lwjglAWTInput = this;
        synchronized (lwjglAWTInput) {
            this.processor = processor;
        }
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

    @Override
    public boolean isButtonPressed(int button) {
        return this.pressedButtons.contains(button);
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
        this.catched = catched;
        this.showCursor(!catched);
    }

    private void showCursor(boolean visible) {
        if (!visible) {
            Toolkit t = Toolkit.getDefaultToolkit();
            BufferedImage i = new BufferedImage(1, 1, 2);
            Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none");
            JFrame frame = LwjglAWTInput.findJFrame(this.canvas);
            frame.setCursor(noCursor);
        } else {
            JFrame frame = LwjglAWTInput.findJFrame(this.canvas);
            frame.setCursor(Cursor.getDefaultCursor());
        }
    }

    protected static JFrame findJFrame(Component component) {
        for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (!(parent instanceof JFrame)) continue;
            return (JFrame)parent;
        }
        return null;
    }

    @Override
    public boolean isCursorCatched() {
        return this.catched;
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
        return this.deltaY;
    }

    @Override
    public int getDeltaY(int pointer) {
        if (pointer == 0) {
            return this.deltaY;
        }
        return 0;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        if (this.robot != null) {
            this.robot.mouseMove(this.canvas.getLocationOnScreen().x + x, this.canvas.getLocationOnScreen().y + y);
        }
    }

    @Override
    public long getCurrentEventTime() {
        return this.currentEventTimeStamp;
    }

    @Override
    public void getRotationMatrix(float[] matrix) {
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

    class TouchEvent {
        static final int TOUCH_DOWN = 0;
        static final int TOUCH_UP = 1;
        static final int TOUCH_DRAGGED = 2;
        static final int TOUCH_MOVED = 3;
        static final int TOUCH_SCROLLED = 4;
        long timeStamp;
        int type;
        int x;
        int y;
        int pointer;
        int button;
        int scrollAmount;

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

