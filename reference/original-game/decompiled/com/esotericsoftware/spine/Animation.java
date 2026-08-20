/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.IkConstraint;
import com.esotericsoftware.spine.PathConstraint;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.TransformConstraint;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.VertexAttachment;

public class Animation {
    final String name;
    private final Array<Timeline> timelines;
    private float duration;

    public Animation(String name, Array<Timeline> timelines, float duration) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (timelines == null) {
            throw new IllegalArgumentException("timelines cannot be null.");
        }
        this.name = name;
        this.timelines = timelines;
        this.duration = duration;
    }

    public Array<Timeline> getTimelines() {
        return this.timelines;
    }

    public float getDuration() {
        return this.duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void apply(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events) {
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        if (loop && this.duration != 0.0f) {
            time %= this.duration;
            if (lastTime > 0.0f) {
                lastTime %= this.duration;
            }
        }
        Array<Timeline> timelines = this.timelines;
        int n = timelines.size;
        for (int i = 0; i < n; ++i) {
            timelines.get(i).apply(skeleton, lastTime, time, events, 1.0f);
        }
    }

    public void mix(Skeleton skeleton, float lastTime, float time, boolean loop, Array<Event> events, float alpha) {
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        if (loop && this.duration != 0.0f) {
            time %= this.duration;
            if (lastTime > 0.0f) {
                lastTime %= this.duration;
            }
        }
        Array<Timeline> timelines = this.timelines;
        int n = timelines.size;
        for (int i = 0; i < n; ++i) {
            timelines.get(i).apply(skeleton, lastTime, time, events, alpha);
        }
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    static int binarySearch(float[] values, float target, int step) {
        int low = 0;
        int high = values.length / step - 2;
        if (high == 0) {
            return step;
        }
        int current = high >>> 1;
        while (true) {
            if (values[(current + 1) * step] <= target) {
                low = current + 1;
            } else {
                high = current;
            }
            if (low == high) {
                return (low + 1) * step;
            }
            current = low + high >>> 1;
        }
    }

    static int binarySearch(float[] values, float target) {
        int low = 0;
        int high = values.length - 2;
        if (high == 0) {
            return 1;
        }
        int current = high >>> 1;
        while (true) {
            if (values[current + 1] <= target) {
                low = current + 1;
            } else {
                high = current;
            }
            if (low == high) {
                return low + 1;
            }
            current = low + high >>> 1;
        }
    }

    static int linearSearch(float[] values, float target, int step) {
        int last = values.length - step;
        for (int i = 0; i <= last; i += step) {
            if (!(values[i] > target)) continue;
            return i;
        }
        return -1;
    }

    public static class PathConstraintMixTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 3;
        private static final int PREV_TIME = -3;
        private static final int PREV_ROTATE = -2;
        private static final int PREV_TRANSLATE = -1;
        private static final int ROTATE = 1;
        private static final int TRANSLATE = 2;
        int pathConstraintIndex;
        private final float[] frames;

        public PathConstraintMixTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 3];
        }

        public void setPathConstraintIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.pathConstraintIndex = index;
        }

        public int getPathConstraintIndex() {
            return this.pathConstraintIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float rotateMix, float translateMix) {
            this.frames[frameIndex *= 3] = time;
            this.frames[frameIndex + 1] = rotateMix;
            this.frames[frameIndex + 2] = translateMix;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            PathConstraint constraint = skeleton.pathConstraints.get(this.pathConstraintIndex);
            if (time >= frames[frames.length - 3]) {
                int i = frames.length;
                constraint.rotateMix += (frames[i + -2] - constraint.rotateMix) * alpha;
                constraint.translateMix += (frames[i + -1] - constraint.translateMix) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 3);
            float rotate = frames[frame + -2];
            float translate = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 3 - 1, 1.0f - (time - frameTime) / (frames[frame + -3] - frameTime));
            constraint.rotateMix += (rotate + (frames[frame + 1] - rotate) * percent - constraint.rotateMix) * alpha;
            constraint.translateMix += (translate + (frames[frame + 2] - translate) * percent - constraint.translateMix) * alpha;
        }
    }

    public static class PathConstraintSpacingTimeline
    extends PathConstraintPositionTimeline {
        public PathConstraintSpacingTimeline(int frameCount) {
            super(frameCount);
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            PathConstraint constraint = skeleton.pathConstraints.get(this.pathConstraintIndex);
            if (time >= frames[frames.length - 2]) {
                int i = frames.length;
                constraint.spacing += (frames[i + -1] - constraint.spacing) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 2);
            float spacing = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 2 - 1, 1.0f - (time - frameTime) / (frames[frame + -2] - frameTime));
            constraint.spacing += (spacing + (frames[frame + 1] - spacing) * percent - constraint.spacing) * alpha;
        }
    }

    public static class PathConstraintPositionTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 2;
        static final int PREV_TIME = -2;
        static final int PREV_VALUE = -1;
        static final int VALUE = 1;
        int pathConstraintIndex;
        final float[] frames;

        public PathConstraintPositionTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 2];
        }

        public void setPathConstraintIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.pathConstraintIndex = index;
        }

        public int getPathConstraintIndex() {
            return this.pathConstraintIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float value) {
            this.frames[frameIndex *= 2] = time;
            this.frames[frameIndex + 1] = value;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            PathConstraint constraint = skeleton.pathConstraints.get(this.pathConstraintIndex);
            if (time >= frames[frames.length - 2]) {
                int i = frames.length;
                constraint.position += (frames[i + -1] - constraint.position) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 2);
            float position = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 2 - 1, 1.0f - (time - frameTime) / (frames[frame + -2] - frameTime));
            constraint.position += (position + (frames[frame + 1] - position) * percent - constraint.position) * alpha;
        }
    }

    public static class TransformConstraintTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 5;
        private static final int PREV_TIME = -5;
        private static final int PREV_ROTATE = -4;
        private static final int PREV_TRANSLATE = -3;
        private static final int PREV_SCALE = -2;
        private static final int PREV_SHEAR = -1;
        private static final int ROTATE = 1;
        private static final int TRANSLATE = 2;
        private static final int SCALE = 3;
        private static final int SHEAR = 4;
        int transformConstraintIndex;
        private final float[] frames;

        public TransformConstraintTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 5];
        }

        public void setTransformConstraintIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.transformConstraintIndex = index;
        }

        public int getTransformConstraintIndex() {
            return this.transformConstraintIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float rotateMix, float translateMix, float scaleMix, float shearMix) {
            this.frames[frameIndex *= 5] = time;
            this.frames[frameIndex + 1] = rotateMix;
            this.frames[frameIndex + 2] = translateMix;
            this.frames[frameIndex + 3] = scaleMix;
            this.frames[frameIndex + 4] = shearMix;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            TransformConstraint constraint = skeleton.transformConstraints.get(this.transformConstraintIndex);
            if (time >= frames[frames.length - 5]) {
                int i = frames.length;
                constraint.rotateMix += (frames[i + -4] - constraint.rotateMix) * alpha;
                constraint.translateMix += (frames[i + -3] - constraint.translateMix) * alpha;
                constraint.scaleMix += (frames[i + -2] - constraint.scaleMix) * alpha;
                constraint.shearMix += (frames[i + -1] - constraint.shearMix) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 5);
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 5 - 1, 1.0f - (time - frameTime) / (frames[frame + -5] - frameTime));
            float rotate = frames[frame + -4];
            float translate = frames[frame + -3];
            float scale = frames[frame + -2];
            float shear = frames[frame + -1];
            constraint.rotateMix += (rotate + (frames[frame + 1] - rotate) * percent - constraint.rotateMix) * alpha;
            constraint.translateMix += (translate + (frames[frame + 2] - translate) * percent - constraint.translateMix) * alpha;
            constraint.scaleMix += (scale + (frames[frame + 3] - scale) * percent - constraint.scaleMix) * alpha;
            constraint.shearMix += (shear + (frames[frame + 4] - shear) * percent - constraint.shearMix) * alpha;
        }
    }

    public static class IkConstraintTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 3;
        private static final int PREV_TIME = -3;
        private static final int PREV_MIX = -2;
        private static final int PREV_BEND_DIRECTION = -1;
        private static final int MIX = 1;
        private static final int BEND_DIRECTION = 2;
        int ikConstraintIndex;
        private final float[] frames;

        public IkConstraintTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 3];
        }

        public void setIkConstraintIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.ikConstraintIndex = index;
        }

        public int getIkConstraintIndex() {
            return this.ikConstraintIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float mix, int bendDirection) {
            this.frames[frameIndex *= 3] = time;
            this.frames[frameIndex + 1] = mix;
            this.frames[frameIndex + 2] = bendDirection;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            IkConstraint constraint = skeleton.ikConstraints.get(this.ikConstraintIndex);
            if (time >= frames[frames.length - 3]) {
                constraint.mix += (frames[frames.length + -2] - constraint.mix) * alpha;
                constraint.bendDirection = (int)frames[frames.length + -1];
                return;
            }
            int frame = Animation.binarySearch(frames, time, 3);
            float mix = frames[frame + -2];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 3 - 1, 1.0f - (time - frameTime) / (frames[frame + -3] - frameTime));
            constraint.mix += (mix + (frames[frame + 1] - mix) * percent - constraint.mix) * alpha;
            constraint.bendDirection = (int)frames[frame + -1];
        }
    }

    public static class DeformTimeline
    extends CurveTimeline {
        private final float[] frames;
        private final float[][] frameVertices;
        int slotIndex;
        VertexAttachment attachment;

        public DeformTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount];
            this.frameVertices = new float[frameCount][];
        }

        public void setSlotIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.slotIndex = index;
        }

        public int getSlotIndex() {
            return this.slotIndex;
        }

        public void setAttachment(VertexAttachment attachment) {
            this.attachment = attachment;
        }

        public Attachment getAttachment() {
            return this.attachment;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public float[][] getVertices() {
            return this.frameVertices;
        }

        public void setFrame(int frameIndex, float time, float[] vertices) {
            this.frames[frameIndex] = time;
            this.frameVertices[frameIndex] = vertices;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
            Slot slot = skeleton.slots.get(this.slotIndex);
            Attachment slotAttachment = slot.attachment;
            if (!(slotAttachment instanceof VertexAttachment) || !((VertexAttachment)slotAttachment).applyDeform(this.attachment)) {
                return;
            }
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            float[][] frameVertices = this.frameVertices;
            int vertexCount = frameVertices[0].length;
            FloatArray verticesArray = slot.getAttachmentVertices();
            if (verticesArray.size != vertexCount) {
                alpha = 1.0f;
            }
            float[] vertices = verticesArray.setSize(vertexCount);
            if (time >= frames[frames.length - 1]) {
                float[] lastVertices = frameVertices[frames.length - 1];
                if (alpha < 1.0f) {
                    for (int i = 0; i < vertexCount; ++i) {
                        int n = i;
                        vertices[n] = vertices[n] + (lastVertices[i] - vertices[i]) * alpha;
                    }
                } else {
                    System.arraycopy(lastVertices, 0, vertices, 0, vertexCount);
                }
                return;
            }
            int frame = Animation.binarySearch(frames, time);
            float[] prevVertices = frameVertices[frame - 1];
            float[] nextVertices = frameVertices[frame];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame - 1, 1.0f - (time - frameTime) / (frames[frame - 1] - frameTime));
            if (alpha < 1.0f) {
                for (int i = 0; i < vertexCount; ++i) {
                    float prev = prevVertices[i];
                    int n = i;
                    vertices[n] = vertices[n] + (prev + (nextVertices[i] - prev) * percent - vertices[i]) * alpha;
                }
            } else {
                for (int i = 0; i < vertexCount; ++i) {
                    float prev = prevVertices[i];
                    vertices[i] = prev + (nextVertices[i] - prev) * percent;
                }
            }
        }
    }

    public static class DrawOrderTimeline
    implements Timeline {
        private final float[] frames;
        private final int[][] drawOrders;

        public DrawOrderTimeline(int frameCount) {
            this.frames = new float[frameCount];
            this.drawOrders = new int[frameCount][];
        }

        public int getFrameCount() {
            return this.frames.length;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public int[][] getDrawOrders() {
            return this.drawOrders;
        }

        public void setFrame(int frameIndex, float time, int[] drawOrder) {
            this.frames[frameIndex] = time;
            this.drawOrders[frameIndex] = drawOrder;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            int frame = time >= frames[frames.length - 1] ? frames.length - 1 : Animation.binarySearch(frames, time) - 1;
            Array<Slot> drawOrder = skeleton.drawOrder;
            Array<Slot> slots = skeleton.slots;
            int[] drawOrderToSetupIndex = this.drawOrders[frame];
            if (drawOrderToSetupIndex == null) {
                System.arraycopy(slots.items, 0, drawOrder.items, 0, slots.size);
            } else {
                int n = drawOrderToSetupIndex.length;
                for (int i = 0; i < n; ++i) {
                    drawOrder.set(i, slots.get(drawOrderToSetupIndex[i]));
                }
            }
        }
    }

    public static class EventTimeline
    implements Timeline {
        private final float[] frames;
        private final Event[] events;

        public EventTimeline(int frameCount) {
            this.frames = new float[frameCount];
            this.events = new Event[frameCount];
        }

        public int getFrameCount() {
            return this.frames.length;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public Event[] getEvents() {
            return this.events;
        }

        public void setFrame(int frameIndex, Event event) {
            this.frames[frameIndex] = event.time;
            this.events[frameIndex] = event;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> firedEvents, float alpha) {
            int frame;
            if (firedEvents == null) {
                return;
            }
            float[] frames = this.frames;
            int frameCount = frames.length;
            if (lastTime > time) {
                this.apply(skeleton, lastTime, 2.1474836E9f, firedEvents, alpha);
                lastTime = -1.0f;
            } else if (lastTime >= frames[frameCount - 1]) {
                return;
            }
            if (time < frames[0]) {
                return;
            }
            if (lastTime < frames[0]) {
                frame = 0;
            } else {
                float frameTime = frames[frame];
                for (frame = Animation.binarySearch(frames, lastTime); frame > 0 && frames[frame - 1] == frameTime; --frame) {
                }
            }
            while (frame < frameCount && time >= frames[frame]) {
                firedEvents.add(this.events[frame]);
                ++frame;
            }
        }
    }

    public static class AttachmentTimeline
    implements Timeline {
        int slotIndex;
        final float[] frames;
        final String[] attachmentNames;

        public AttachmentTimeline(int frameCount) {
            this.frames = new float[frameCount];
            this.attachmentNames = new String[frameCount];
        }

        public int getFrameCount() {
            return this.frames.length;
        }

        public void setSlotIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.slotIndex = index;
        }

        public int getSlotIndex() {
            return this.slotIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public String[] getAttachmentNames() {
            return this.attachmentNames;
        }

        public void setFrame(int frameIndex, float time, String attachmentName) {
            this.frames[frameIndex] = time;
            this.attachmentNames[frameIndex] = attachmentName;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            int frameIndex = time >= frames[frames.length - 1] ? frames.length - 1 : Animation.binarySearch(frames, time, 1) - 1;
            String attachmentName = this.attachmentNames[frameIndex];
            skeleton.slots.get(this.slotIndex).setAttachment(attachmentName == null ? null : skeleton.getAttachment(this.slotIndex, attachmentName));
        }
    }

    public static class ColorTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 5;
        private static final int PREV_TIME = -5;
        private static final int PREV_R = -4;
        private static final int PREV_G = -3;
        private static final int PREV_B = -2;
        private static final int PREV_A = -1;
        private static final int R = 1;
        private static final int G = 2;
        private static final int B = 3;
        private static final int A = 4;
        int slotIndex;
        private final float[] frames;

        public ColorTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 5];
        }

        public void setSlotIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.slotIndex = index;
        }

        public int getSlotIndex() {
            return this.slotIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float r, float g, float b, float a) {
            this.frames[frameIndex *= 5] = time;
            this.frames[frameIndex + 1] = r;
            this.frames[frameIndex + 2] = g;
            this.frames[frameIndex + 3] = b;
            this.frames[frameIndex + 4] = a;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float a;
            float b;
            float g;
            float r;
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            if (time >= frames[frames.length - 5]) {
                int i = frames.length;
                r = frames[i + -4];
                g = frames[i + -3];
                b = frames[i + -2];
                a = frames[i + -1];
            } else {
                int frame = Animation.binarySearch(frames, time, 5);
                r = frames[frame + -4];
                g = frames[frame + -3];
                b = frames[frame + -2];
                a = frames[frame + -1];
                float frameTime = frames[frame];
                float percent = this.getCurvePercent(frame / 5 - 1, 1.0f - (time - frameTime) / (frames[frame + -5] - frameTime));
                r += (frames[frame + 1] - r) * percent;
                g += (frames[frame + 2] - g) * percent;
                b += (frames[frame + 3] - b) * percent;
                a += (frames[frame + 4] - a) * percent;
            }
            Color color = skeleton.slots.get((int)this.slotIndex).color;
            if (alpha < 1.0f) {
                color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
            } else {
                color.set(r, g, b, a);
            }
        }
    }

    public static class ShearTimeline
    extends TranslateTimeline {
        public ShearTimeline(int frameCount) {
            super(frameCount);
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            Bone bone = skeleton.bones.get(this.boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.shearX += (bone.data.shearX + frames[frames.length + -2] - bone.shearX) * alpha;
                bone.shearY += (bone.data.shearY + frames[frames.length + -1] - bone.shearY) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 3);
            float prevX = frames[frame + -2];
            float prevY = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 3 - 1, 1.0f - (time - frameTime) / (frames[frame + -3] - frameTime));
            bone.shearX += (bone.data.shearX + (prevX + (frames[frame + 1] - prevX) * percent) - bone.shearX) * alpha;
            bone.shearY += (bone.data.shearY + (prevY + (frames[frame + 2] - prevY) * percent) - bone.shearY) * alpha;
        }
    }

    public static class ScaleTimeline
    extends TranslateTimeline {
        public ScaleTimeline(int frameCount) {
            super(frameCount);
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            Bone bone = skeleton.bones.get(this.boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.scaleX += (bone.data.scaleX * frames[frames.length + -2] - bone.scaleX) * alpha;
                bone.scaleY += (bone.data.scaleY * frames[frames.length + -1] - bone.scaleY) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 3);
            float prevX = frames[frame + -2];
            float prevY = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 3 - 1, 1.0f - (time - frameTime) / (frames[frame + -3] - frameTime));
            bone.scaleX += (bone.data.scaleX * (prevX + (frames[frame + 1] - prevX) * percent) - bone.scaleX) * alpha;
            bone.scaleY += (bone.data.scaleY * (prevY + (frames[frame + 2] - prevY) * percent) - bone.scaleY) * alpha;
        }
    }

    public static class TranslateTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 3;
        static final int PREV_TIME = -3;
        static final int PREV_X = -2;
        static final int PREV_Y = -1;
        static final int X = 1;
        static final int Y = 2;
        int boneIndex;
        final float[] frames;

        public TranslateTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount * 3];
        }

        public void setBoneIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.boneIndex = index;
        }

        public int getBoneIndex() {
            return this.boneIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float x, float y) {
            this.frames[frameIndex *= 3] = time;
            this.frames[frameIndex + 1] = x;
            this.frames[frameIndex + 2] = y;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            Bone bone = skeleton.bones.get(this.boneIndex);
            if (time >= frames[frames.length - 3]) {
                bone.x += (bone.data.x + frames[frames.length + -2] - bone.x) * alpha;
                bone.y += (bone.data.y + frames[frames.length + -1] - bone.y) * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 3);
            float prevX = frames[frame + -2];
            float prevY = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent(frame / 3 - 1, 1.0f - (time - frameTime) / (frames[frame + -3] - frameTime));
            bone.x += (bone.data.x + prevX + (frames[frame + 1] - prevX) * percent - bone.x) * alpha;
            bone.y += (bone.data.y + prevY + (frames[frame + 2] - prevY) * percent - bone.y) * alpha;
        }
    }

    public static class RotateTimeline
    extends CurveTimeline {
        public static final int ENTRIES = 2;
        private static final int PREV_TIME = -2;
        private static final int PREV_ROTATION = -1;
        private static final int ROTATION = 1;
        int boneIndex;
        final float[] frames;

        public RotateTimeline(int frameCount) {
            super(frameCount);
            this.frames = new float[frameCount << 1];
        }

        public void setBoneIndex(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0.");
            }
            this.boneIndex = index;
        }

        public int getBoneIndex() {
            return this.boneIndex;
        }

        public float[] getFrames() {
            return this.frames;
        }

        public void setFrame(int frameIndex, float time, float degrees) {
            this.frames[frameIndex <<= 1] = time;
            this.frames[frameIndex + 1] = degrees;
        }

        @Override
        public void apply(Skeleton skeleton, float lastTime, float time, Array<Event> events, float alpha) {
            float amount;
            float[] frames = this.frames;
            if (time < frames[0]) {
                return;
            }
            Bone bone = skeleton.bones.get(this.boneIndex);
            if (time >= frames[frames.length - 2]) {
                float amount2;
                for (amount2 = bone.data.rotation + frames[frames.length + -1] - bone.rotation; amount2 > 180.0f; amount2 -= 360.0f) {
                }
                while (amount2 < -180.0f) {
                    amount2 += 360.0f;
                }
                bone.rotation += amount2 * alpha;
                return;
            }
            int frame = Animation.binarySearch(frames, time, 2);
            float prevRotation = frames[frame + -1];
            float frameTime = frames[frame];
            float percent = this.getCurvePercent((frame >> 1) - 1, 1.0f - (time - frameTime) / (frames[frame + -2] - frameTime));
            for (amount = frames[frame + 1] - prevRotation; amount > 180.0f; amount -= 360.0f) {
            }
            while (amount < -180.0f) {
                amount += 360.0f;
            }
            for (amount = bone.data.rotation + (prevRotation + amount * percent) - bone.rotation; amount > 180.0f; amount -= 360.0f) {
            }
            while (amount < -180.0f) {
                amount += 360.0f;
            }
            bone.rotation += amount * alpha;
        }
    }

    public static abstract class CurveTimeline
    implements Timeline {
        public static final float LINEAR = 0.0f;
        public static final float STEPPED = 1.0f;
        public static final float BEZIER = 2.0f;
        private static final int BEZIER_SIZE = 19;
        private final float[] curves;

        public CurveTimeline(int frameCount) {
            if (frameCount <= 0) {
                throw new IllegalArgumentException("frameCount must be > 0: " + frameCount);
            }
            this.curves = new float[(frameCount - 1) * 19];
        }

        public int getFrameCount() {
            return this.curves.length / 19 + 1;
        }

        public void setLinear(int frameIndex) {
            this.curves[frameIndex * 19] = 0.0f;
        }

        public void setStepped(int frameIndex) {
            this.curves[frameIndex * 19] = 1.0f;
        }

        public float getCurveType(int frameIndex) {
            int index = frameIndex * 19;
            if (index == this.curves.length) {
                return 0.0f;
            }
            float type = this.curves[index];
            if (type == 0.0f) {
                return 0.0f;
            }
            if (type == 1.0f) {
                return 1.0f;
            }
            return 2.0f;
        }

        public void setCurve(int frameIndex, float cx1, float cy1, float cx2, float cy2) {
            float tmpx = (-cx1 * 2.0f + cx2) * 0.03f;
            float tmpy = (-cy1 * 2.0f + cy2) * 0.03f;
            float dddfx = ((cx1 - cx2) * 3.0f + 1.0f) * 0.006f;
            float dddfy = ((cy1 - cy2) * 3.0f + 1.0f) * 0.006f;
            float ddfx = tmpx * 2.0f + dddfx;
            float ddfy = tmpy * 2.0f + dddfy;
            float dfx = cx1 * 0.3f + tmpx + dddfx * 0.16666667f;
            float dfy = cy1 * 0.3f + tmpy + dddfy * 0.16666667f;
            int i = frameIndex * 19;
            float[] curves = this.curves;
            curves[i++] = 2.0f;
            float x = dfx;
            float y = dfy;
            int n = i + 19 - 1;
            while (i < n) {
                curves[i] = x;
                curves[i + 1] = y;
                x += (dfx += (ddfx += dddfx));
                y += (dfy += (ddfy += dddfy));
                i += 2;
            }
        }

        public float getCurvePercent(int frameIndex, float percent) {
            percent = MathUtils.clamp(percent, 0.0f, 1.0f);
            float[] curves = this.curves;
            int i = frameIndex * 19;
            float type = curves[i];
            if (type == 0.0f) {
                return percent;
            }
            if (type == 1.0f) {
                return 0.0f;
            }
            float x = 0.0f;
            int start = ++i;
            int n = i + 19 - 1;
            while (i < n) {
                x = curves[i];
                if (x >= percent) {
                    float prevY;
                    float prevX;
                    if (i == start) {
                        prevX = 0.0f;
                        prevY = 0.0f;
                    } else {
                        prevX = curves[i - 2];
                        prevY = curves[i - 1];
                    }
                    return prevY + (curves[i + 1] - prevY) * (percent - prevX) / (x - prevX);
                }
                i += 2;
            }
            float y = curves[i - 1];
            return y + (1.0f - y) * (percent - x) / (1.0f - x);
        }
    }

    public static interface Timeline {
        public void apply(Skeleton var1, float var2, float var3, Array<Event> var4, float var5);
    }
}

