/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public class RegionSequenceAttachment
extends RegionAttachment {
    private Mode mode;
    private float frameTime;
    private TextureRegion[] regions;

    public RegionSequenceAttachment(String name) {
        super(name);
    }

    @Override
    public float[] updateWorldVertices(Slot slot, boolean premultipliedAlpha) {
        if (this.regions == null) {
            throw new IllegalStateException("Regions have not been set: " + this);
        }
        int frameIndex = (int)(slot.getAttachmentTime() / this.frameTime);
        switch (this.mode) {
            case forward: {
                frameIndex = Math.min(this.regions.length - 1, frameIndex);
                break;
            }
            case forwardLoop: {
                frameIndex %= this.regions.length;
                break;
            }
            case pingPong: {
                if ((frameIndex %= this.regions.length << 1) < this.regions.length) break;
                frameIndex = this.regions.length - 1 - (frameIndex - this.regions.length);
                break;
            }
            case random: {
                frameIndex = MathUtils.random(this.regions.length - 1);
                break;
            }
            case backward: {
                frameIndex = Math.max(this.regions.length - frameIndex - 1, 0);
                break;
            }
            case backwardLoop: {
                frameIndex %= this.regions.length;
                frameIndex = this.regions.length - frameIndex - 1;
            }
        }
        this.setRegion(this.regions[frameIndex]);
        return super.updateWorldVertices(slot, premultipliedAlpha);
    }

    public TextureRegion[] getRegions() {
        if (this.regions == null) {
            throw new IllegalStateException("Regions have not been set: " + this);
        }
        return this.regions;
    }

    public void setRegions(TextureRegion[] regions) {
        this.regions = regions;
    }

    public void setFrameTime(float frameTime) {
        this.frameTime = frameTime;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public static enum Mode {
        forward,
        backward,
        forwardLoop,
        backwardLoop,
        pingPong,
        random;

    }
}

