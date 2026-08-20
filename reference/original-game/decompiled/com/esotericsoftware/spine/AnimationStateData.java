/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.ObjectFloatMap;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.SkeletonData;

public class AnimationStateData {
    private final SkeletonData skeletonData;
    final ObjectFloatMap<Key> animationToMixTime = new ObjectFloatMap();
    final Key tempKey = new Key();
    float defaultMix;

    public AnimationStateData(SkeletonData skeletonData) {
        if (skeletonData == null) {
            throw new IllegalArgumentException("skeletonData cannot be null.");
        }
        this.skeletonData = skeletonData;
    }

    public SkeletonData getSkeletonData() {
        return this.skeletonData;
    }

    public void setMix(String fromName, String toName, float duration) {
        Animation from = this.skeletonData.findAnimation(fromName);
        if (from == null) {
            throw new IllegalArgumentException("Animation not found: " + fromName);
        }
        Animation to = this.skeletonData.findAnimation(toName);
        if (to == null) {
            throw new IllegalArgumentException("Animation not found: " + toName);
        }
        this.setMix(from, to, duration);
    }

    public void setMix(Animation from, Animation to, float duration) {
        if (from == null) {
            throw new IllegalArgumentException("from cannot be null.");
        }
        if (to == null) {
            throw new IllegalArgumentException("to cannot be null.");
        }
        Key key = new Key();
        key.a1 = from;
        key.a2 = to;
        this.animationToMixTime.put(key, duration);
    }

    public float getMix(Animation from, Animation to) {
        this.tempKey.a1 = from;
        this.tempKey.a2 = to;
        return this.animationToMixTime.get(this.tempKey, this.defaultMix);
    }

    public float getDefaultMix() {
        return this.defaultMix;
    }

    public void setDefaultMix(float defaultMix) {
        this.defaultMix = defaultMix;
    }

    static class Key {
        Animation a1;
        Animation a2;

        Key() {
        }

        public int hashCode() {
            return 31 * (31 + this.a1.hashCode()) + this.a2.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            Key other = (Key)obj;
            if (this.a1 == null ? other.a1 != null : !this.a1.equals(other.a1)) {
                return false;
            }
            return !(this.a2 == null ? other.a2 != null : !this.a2.equals(other.a2));
        }
    }
}

