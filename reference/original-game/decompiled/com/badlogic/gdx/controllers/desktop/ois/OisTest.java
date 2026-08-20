/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop.ois;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.controllers.desktop.DesktopControllersBuild;
import com.badlogic.gdx.controllers.desktop.OisControllers;
import com.badlogic.gdx.controllers.desktop.ois.Ois;
import com.badlogic.gdx.controllers.desktop.ois.OisJoystick;
import com.badlogic.gdx.controllers.desktop.ois.OisListener;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class OisTest {
    public static void main(String[] args) throws Exception {
        DesktopControllersBuild.main(null);
        new SharedLibraryLoader("libs/gdx-controllers-desktop-natives.jar").load("gdx-controllers-desktop");
        ApplicationAdapter app = new ApplicationAdapter(){
            Ois ois;

            @Override
            public void create() {
                this.ois = new Ois(OisControllers.getWindowHandle());
                if (this.ois.getJoysticks().size() > 0) {
                    this.ois.getJoysticks().get(0).setListener(new OisListener(){

                        @Override
                        public void xSliderMoved(OisJoystick joystick, int slider, boolean value) {
                            System.out.println("xSliderMoved: " + slider + ", " + value);
                        }

                        @Override
                        public void ySliderMoved(OisJoystick joystick, int slider, boolean value) {
                            System.out.println("ySliderMoved: " + slider + ", " + value);
                        }

                        @Override
                        public void povMoved(OisJoystick joystick, int pov, OisJoystick.OisPov value) {
                            System.out.println("povMoved: " + pov + ", " + (Object)((Object)value));
                        }

                        @Override
                        public void buttonReleased(OisJoystick joystick, int button) {
                            System.out.println("buttonReleased: " + button);
                        }

                        @Override
                        public void buttonPressed(OisJoystick joystick, int button) {
                            System.out.println("buttonPressed: " + button);
                        }

                        @Override
                        public void axisMoved(OisJoystick joystick, int axis, float value) {
                            System.out.println("axisMoved: " + axis + ", " + value);
                        }
                    });
                }
            }

            @Override
            public void render() {
                this.ois.update();
            }
        };
    }
}

