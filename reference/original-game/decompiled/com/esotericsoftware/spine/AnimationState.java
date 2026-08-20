/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;

public class AnimationState {
    private AnimationStateData data;
    private Array<TrackEntry> tracks = new Array();
    private final Array<Event> events = new Array();
    private final Array<AnimationStateListener> listeners = new Array();
    private float timeScale = 1.0f;
    private Pool<TrackEntry> trackEntryPool = new Pool(){

        protected Object newObject() {
            return new TrackEntry();
        }
    };

    public AnimationState() {
    }

    public AnimationState(AnimationStateData data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        this.data = data;
    }

    public void update(float delta) {
        delta *= this.timeScale;
        for (int i = 0; i < this.tracks.size; ++i) {
            TrackEntry current = this.tracks.get(i);
            if (current == null) continue;
            TrackEntry next = current.next;
            if (next != null) {
                float nextTime = current.lastTime - next.delay;
                if (nextTime >= 0.0f) {
                    float nextDelta = delta * next.timeScale;
                    next.time = nextTime + nextDelta;
                    current.time += delta * current.timeScale;
                    this.setCurrent(i, next);
                    next.time -= nextDelta;
                    current = next;
                }
            } else if (!current.loop && current.lastTime >= current.endTime) {
                this.clearTrack(i);
                continue;
            }
            current.time += delta * current.timeScale;
            if (current.previous == null) continue;
            float previousDelta = delta * current.previous.timeScale;
            current.previous.time += previousDelta;
            current.mixTime += previousDelta;
        }
    }

    public void apply(Skeleton skeleton) {
        Array<Event> events = this.events;
        int listenerCount = this.listeners.size;
        for (int i = 0; i < this.tracks.size; ++i) {
            TrackEntry previous;
            TrackEntry current = this.tracks.get(i);
            if (current == null) continue;
            events.size = 0;
            float time = current.time;
            float lastTime = current.lastTime;
            float endTime = current.endTime;
            boolean loop = current.loop;
            if (!loop && time > endTime) {
                time = endTime;
            }
            if ((previous = current.previous) == null) {
                current.animation.mix(skeleton, lastTime, time, loop, events, current.mix);
            } else {
                float previousTime = previous.time;
                if (!previous.loop && previousTime > previous.endTime) {
                    previousTime = previous.endTime;
                }
                previous.animation.apply(skeleton, previousTime, previousTime, previous.loop, null);
                float alpha = current.mixTime / current.mixDuration * current.mix;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    this.trackEntryPool.free(previous);
                    current.previous = null;
                }
                current.animation.mix(skeleton, lastTime, time, loop, events, alpha);
            }
            int nn = events.size;
            for (int ii = 0; ii < nn; ++ii) {
                Event event = events.get(ii);
                if (current.listener != null) {
                    current.listener.event(i, event);
                }
                for (int iii = 0; iii < listenerCount; ++iii) {
                    this.listeners.get(iii).event(i, event);
                }
            }
            if (loop ? lastTime % endTime > time % endTime : lastTime < endTime && time >= endTime) {
                int count = (int)(time / endTime);
                if (current.listener != null) {
                    current.listener.complete(i, count);
                }
                int nn2 = this.listeners.size;
                for (int ii = 0; ii < nn2; ++ii) {
                    this.listeners.get(ii).complete(i, count);
                }
            }
            current.lastTime = current.time;
        }
    }

    public void clearTracks() {
        int n = this.tracks.size;
        for (int i = 0; i < n; ++i) {
            this.clearTrack(i);
        }
        this.tracks.clear();
    }

    public void clearTrack(int trackIndex) {
        if (trackIndex >= this.tracks.size) {
            return;
        }
        TrackEntry current = this.tracks.get(trackIndex);
        if (current == null) {
            return;
        }
        if (current.listener != null) {
            current.listener.end(trackIndex);
        }
        int n = this.listeners.size;
        for (int i = 0; i < n; ++i) {
            this.listeners.get(i).end(trackIndex);
        }
        this.tracks.set(trackIndex, null);
        this.freeAll(current);
        if (current.previous != null) {
            this.trackEntryPool.free(current.previous);
        }
    }

    private void freeAll(TrackEntry entry) {
        while (entry != null) {
            TrackEntry next = entry.next;
            this.trackEntryPool.free(entry);
            entry = next;
        }
    }

    private TrackEntry expandToIndex(int index) {
        if (index < this.tracks.size) {
            return this.tracks.get(index);
        }
        this.tracks.ensureCapacity(index - this.tracks.size + 1);
        this.tracks.size = index + 1;
        return null;
    }

    private void setCurrent(int index, TrackEntry entry) {
        TrackEntry current = this.expandToIndex(index);
        if (current != null) {
            TrackEntry previous = current.previous;
            current.previous = null;
            if (current.listener != null) {
                current.listener.end(index);
            }
            int n = this.listeners.size;
            for (int i = 0; i < n; ++i) {
                this.listeners.get(i).end(index);
            }
            entry.mixDuration = this.data.getMix(current.animation, entry.animation);
            if (entry.mixDuration > 0.0f) {
                entry.mixTime = 0.0f;
                if (previous != null && current.mixTime / current.mixDuration < 0.5f) {
                    entry.previous = previous;
                    previous = current;
                } else {
                    entry.previous = current;
                }
            } else {
                this.trackEntryPool.free(current);
            }
            if (previous != null) {
                this.trackEntryPool.free(previous);
            }
        }
        this.tracks.set(index, entry);
        if (entry.listener != null) {
            entry.listener.start(index);
        }
        int n = this.listeners.size;
        for (int i = 0; i < n; ++i) {
            this.listeners.get(i).start(index);
        }
    }

    public TrackEntry setAnimation(int trackIndex, String animationName, boolean loop) {
        Animation animation = this.data.getSkeletonData().findAnimation(animationName);
        if (animation == null) {
            throw new IllegalArgumentException("Animation not found: " + animationName);
        }
        return this.setAnimation(trackIndex, animation, loop);
    }

    public TrackEntry setAnimation(int trackIndex, Animation animation, boolean loop) {
        TrackEntry current = this.expandToIndex(trackIndex);
        if (current != null) {
            this.freeAll(current.next);
        }
        TrackEntry entry = this.trackEntryPool.obtain();
        entry.animation = animation;
        entry.loop = loop;
        entry.endTime = animation.getDuration();
        this.setCurrent(trackIndex, entry);
        return entry;
    }

    public TrackEntry addAnimation(int trackIndex, String animationName, boolean loop, float delay) {
        Animation animation = this.data.getSkeletonData().findAnimation(animationName);
        if (animation == null) {
            throw new IllegalArgumentException("Animation not found: " + animationName);
        }
        return this.addAnimation(trackIndex, animation, loop, delay);
    }

    public TrackEntry addAnimation(int trackIndex, Animation animation, boolean loop, float delay) {
        TrackEntry entry = this.trackEntryPool.obtain();
        entry.animation = animation;
        entry.loop = loop;
        entry.endTime = animation.getDuration();
        TrackEntry last = this.expandToIndex(trackIndex);
        if (last != null) {
            while (last.next != null) {
                last = last.next;
            }
            last.next = entry;
        } else {
            this.tracks.set(trackIndex, entry);
        }
        if (delay <= 0.0f) {
            delay = last != null ? (delay += last.endTime - this.data.getMix(last.animation, animation)) : 0.0f;
        }
        entry.delay = delay;
        return entry;
    }

    public TrackEntry getCurrent(int trackIndex) {
        if (trackIndex >= this.tracks.size) {
            return null;
        }
        return this.tracks.get(trackIndex);
    }

    public void addListener(AnimationStateListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        }
        this.listeners.add(listener);
    }

    public void removeListener(AnimationStateListener listener) {
        this.listeners.removeValue(listener, true);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    public float getTimeScale() {
        return this.timeScale;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = timeScale;
    }

    public AnimationStateData getData() {
        return this.data;
    }

    public void setData(AnimationStateData data) {
        this.data = data;
    }

    public Array<TrackEntry> getTracks() {
        return this.tracks;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        int n = this.tracks.size;
        for (int i = 0; i < n; ++i) {
            TrackEntry entry = this.tracks.get(i);
            if (entry == null) continue;
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(entry.toString());
        }
        if (buffer.length() == 0) {
            return "<none>";
        }
        return buffer.toString();
    }

    public static abstract class AnimationStateAdapter
    implements AnimationStateListener {
        @Override
        public void event(int trackIndex, Event event) {
        }

        @Override
        public void complete(int trackIndex, int loopCount) {
        }

        @Override
        public void start(int trackIndex) {
        }

        @Override
        public void end(int trackIndex) {
        }
    }

    public static interface AnimationStateListener {
        public void event(int var1, Event var2);

        public void complete(int var1, int var2);

        public void start(int var1);

        public void end(int var1);
    }

    public static class TrackEntry
    implements Pool.Poolable {
        TrackEntry next;
        TrackEntry previous;
        Animation animation;
        boolean loop;
        float delay;
        float time;
        float lastTime = -1.0f;
        float endTime;
        float timeScale = 1.0f;
        float mixTime;
        float mixDuration;
        AnimationStateListener listener;
        float mix = 1.0f;

        @Override
        public void reset() {
            this.next = null;
            this.previous = null;
            this.animation = null;
            this.listener = null;
            this.timeScale = 1.0f;
            this.lastTime = -1.0f;
            this.time = 0.0f;
        }

        public Animation getAnimation() {
            return this.animation;
        }

        public void setAnimation(Animation animation) {
            this.animation = animation;
        }

        public boolean getLoop() {
            return this.loop;
        }

        public void setLoop(boolean loop) {
            this.loop = loop;
        }

        public float getDelay() {
            return this.delay;
        }

        public void setDelay(float delay) {
            this.delay = delay;
        }

        public float getTime() {
            return this.time;
        }

        public void setTime(float time) {
            this.time = time;
        }

        public float getEndTime() {
            return this.endTime;
        }

        public void setEndTime(float endTime) {
            this.endTime = endTime;
        }

        public AnimationStateListener getListener() {
            return this.listener;
        }

        public void setListener(AnimationStateListener listener) {
            this.listener = listener;
        }

        public float getLastTime() {
            return this.lastTime;
        }

        public void setLastTime(float lastTime) {
            this.lastTime = lastTime;
        }

        public float getMix() {
            return this.mix;
        }

        public void setMix(float mix) {
            this.mix = mix;
        }

        public float getTimeScale() {
            return this.timeScale;
        }

        public void setTimeScale(float timeScale) {
            this.timeScale = timeScale;
        }

        public TrackEntry getNext() {
            return this.next;
        }

        public void setNext(TrackEntry next) {
            this.next = next;
        }

        public boolean isComplete() {
            return this.time >= this.endTime;
        }

        public String toString() {
            return this.animation == null ? "<none>" : this.animation.name;
        }
    }
}

