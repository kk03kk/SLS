/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.BaseAnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;

public class AnimationController
extends BaseAnimationController {
    protected final Pool<AnimationDesc> animationPool = new Pool<AnimationDesc>(){

        @Override
        protected AnimationDesc newObject() {
            return new AnimationDesc();
        }
    };
    public AnimationDesc current;
    public AnimationDesc queued;
    public float queuedTransitionTime;
    public AnimationDesc previous;
    public float transitionCurrentTime;
    public float transitionTargetTime;
    public boolean inAction;
    public boolean paused;
    public boolean allowSameAnimation;
    private boolean justChangedAnimation = false;

    public AnimationController(ModelInstance target) {
        super(target);
    }

    private AnimationDesc obtain(Animation anim, float offset, float duration, int loopCount, float speed, AnimationListener listener) {
        if (anim == null) {
            return null;
        }
        AnimationDesc result = this.animationPool.obtain();
        result.animation = anim;
        result.listener = listener;
        result.loopCount = loopCount;
        result.speed = speed;
        result.offset = offset;
        result.duration = duration < 0.0f ? anim.duration - offset : duration;
        result.time = speed < 0.0f ? result.duration : 0.0f;
        return result;
    }

    private AnimationDesc obtain(String id, float offset, float duration, int loopCount, float speed, AnimationListener listener) {
        if (id == null) {
            return null;
        }
        Animation anim = this.target.getAnimation(id);
        if (anim == null) {
            throw new GdxRuntimeException("Unknown animation: " + id);
        }
        return this.obtain(anim, offset, duration, loopCount, speed, listener);
    }

    private AnimationDesc obtain(AnimationDesc anim) {
        return this.obtain(anim.animation, anim.offset, anim.duration, anim.loopCount, anim.speed, anim.listener);
    }

    public void update(float delta) {
        if (this.paused) {
            return;
        }
        if (this.previous != null) {
            float f;
            this.transitionCurrentTime += delta;
            if (f >= this.transitionTargetTime) {
                this.removeAnimation(this.previous.animation);
                this.justChangedAnimation = true;
                this.animationPool.free(this.previous);
                this.previous = null;
            }
        }
        if (this.justChangedAnimation) {
            this.target.calculateTransforms();
            this.justChangedAnimation = false;
        }
        if (this.current == null || this.current.loopCount == 0 || this.current.animation == null) {
            return;
        }
        float remain = this.current.update(delta);
        if (remain != 0.0f && this.queued != null) {
            this.inAction = false;
            this.animate(this.queued, this.queuedTransitionTime);
            this.queued = null;
            this.update(remain);
            return;
        }
        if (this.previous != null) {
            this.applyAnimations(this.previous.animation, this.previous.offset + this.previous.time, this.current.animation, this.current.offset + this.current.time, this.transitionCurrentTime / this.transitionTargetTime);
        } else {
            this.applyAnimation(this.current.animation, this.current.offset + this.current.time);
        }
    }

    public AnimationDesc setAnimation(String id) {
        return this.setAnimation(id, 1, 1.0f, null);
    }

    public AnimationDesc setAnimation(String id, int loopCount) {
        return this.setAnimation(id, loopCount, 1.0f, null);
    }

    public AnimationDesc setAnimation(String id, AnimationListener listener) {
        return this.setAnimation(id, 1, 1.0f, listener);
    }

    public AnimationDesc setAnimation(String id, int loopCount, AnimationListener listener) {
        return this.setAnimation(id, loopCount, 1.0f, listener);
    }

    public AnimationDesc setAnimation(String id, int loopCount, float speed, AnimationListener listener) {
        return this.setAnimation(id, 0.0f, -1.0f, loopCount, speed, listener);
    }

    public AnimationDesc setAnimation(String id, float offset, float duration, int loopCount, float speed, AnimationListener listener) {
        return this.setAnimation(this.obtain(id, offset, duration, loopCount, speed, listener));
    }

    protected AnimationDesc setAnimation(Animation anim, float offset, float duration, int loopCount, float speed, AnimationListener listener) {
        return this.setAnimation(this.obtain(anim, offset, duration, loopCount, speed, listener));
    }

    protected AnimationDesc setAnimation(AnimationDesc anim) {
        if (this.current == null) {
            this.current = anim;
        } else {
            if (!this.allowSameAnimation && anim != null && this.current.animation == anim.animation) {
                anim.time = this.current.time;
            } else {
                this.removeAnimation(this.current.animation);
            }
            this.animationPool.free(this.current);
            this.current = anim;
        }
        this.justChangedAnimation = true;
        return anim;
    }

    public AnimationDesc animate(String id, float transitionTime) {
        return this.animate(id, 1, 1.0f, null, transitionTime);
    }

    public AnimationDesc animate(String id, AnimationListener listener, float transitionTime) {
        return this.animate(id, 1, 1.0f, listener, transitionTime);
    }

    public AnimationDesc animate(String id, int loopCount, AnimationListener listener, float transitionTime) {
        return this.animate(id, loopCount, 1.0f, listener, transitionTime);
    }

    public AnimationDesc animate(String id, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.animate(id, 0.0f, -1.0f, loopCount, speed, listener, transitionTime);
    }

    public AnimationDesc animate(String id, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.animate(this.obtain(id, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc animate(Animation anim, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.animate(this.obtain(anim, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc animate(AnimationDesc anim, float transitionTime) {
        if (this.current == null) {
            this.current = anim;
        } else if (this.inAction) {
            this.queue(anim, transitionTime);
        } else if (!this.allowSameAnimation && anim != null && this.current.animation == anim.animation) {
            anim.time = this.current.time;
            this.animationPool.free(this.current);
            this.current = anim;
        } else {
            if (this.previous != null) {
                this.removeAnimation(this.previous.animation);
                this.animationPool.free(this.previous);
            }
            this.previous = this.current;
            this.current = anim;
            this.transitionCurrentTime = 0.0f;
            this.transitionTargetTime = transitionTime;
        }
        return anim;
    }

    public AnimationDesc queue(String id, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.queue(id, 0.0f, -1.0f, loopCount, speed, listener, transitionTime);
    }

    public AnimationDesc queue(String id, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.queue(this.obtain(id, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc queue(Animation anim, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.queue(this.obtain(anim, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc queue(AnimationDesc anim, float transitionTime) {
        if (this.current == null || this.current.loopCount == 0) {
            this.animate(anim, transitionTime);
        } else {
            if (this.queued != null) {
                this.animationPool.free(this.queued);
            }
            this.queued = anim;
            this.queuedTransitionTime = transitionTime;
            if (this.current.loopCount < 0) {
                this.current.loopCount = 1;
            }
        }
        return anim;
    }

    public AnimationDesc action(String id, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.action(id, 0.0f, -1.0f, loopCount, speed, listener, transitionTime);
    }

    public AnimationDesc action(String id, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.action(this.obtain(id, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc action(Animation anim, float offset, float duration, int loopCount, float speed, AnimationListener listener, float transitionTime) {
        return this.action(this.obtain(anim, offset, duration, loopCount, speed, listener), transitionTime);
    }

    protected AnimationDesc action(AnimationDesc anim, float transitionTime) {
        if (anim.loopCount < 0) {
            throw new GdxRuntimeException("An action cannot be continuous");
        }
        if (this.current == null || this.current.loopCount == 0) {
            this.animate(anim, transitionTime);
        } else {
            AnimationDesc toQueue = this.inAction ? null : this.obtain(this.current);
            this.inAction = false;
            this.animate(anim, transitionTime);
            this.inAction = true;
            if (toQueue != null) {
                this.queue(toQueue, transitionTime);
            }
        }
        return anim;
    }

    public static class AnimationDesc {
        public AnimationListener listener;
        public Animation animation;
        public float speed;
        public float time;
        public float offset;
        public float duration;
        public int loopCount;

        protected AnimationDesc() {
        }

        protected float update(float delta) {
            if (this.loopCount != 0 && this.animation != null) {
                int loops;
                float diff = this.speed * delta;
                if (!MathUtils.isZero(this.duration)) {
                    this.time += diff;
                    loops = (int)Math.abs(this.time / this.duration);
                    if (this.time < 0.0f) {
                        ++loops;
                        while (this.time < 0.0f) {
                            this.time += this.duration;
                        }
                    }
                    this.time = Math.abs(this.time % this.duration);
                } else {
                    loops = 1;
                }
                for (int i = 0; i < loops; ++i) {
                    if (this.loopCount > 0) {
                        --this.loopCount;
                    }
                    if (this.loopCount != 0 && this.listener != null) {
                        this.listener.onLoop(this);
                    }
                    if (this.loopCount != 0) continue;
                    float result = (float)(loops - 1 - i) * this.duration + (diff < 0.0f ? this.duration - this.time : this.time);
                    float f = this.time = diff < 0.0f ? 0.0f : this.duration;
                    if (this.listener != null) {
                        this.listener.onEnd(this);
                    }
                    return result;
                }
                return 0.0f;
            }
            return delta;
        }
    }

    public static interface AnimationListener {
        public void onEnd(AnimationDesc var1);

        public void onLoop(AnimationDesc var1);
    }
}

