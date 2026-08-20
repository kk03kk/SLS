/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;

public class BaseAnimationController {
    private final Pool<Transform> transformPool = new Pool<Transform>(){

        @Override
        protected Transform newObject() {
            return new Transform();
        }
    };
    private static final ObjectMap<Node, Transform> transforms = new ObjectMap();
    private boolean applying = false;
    public final ModelInstance target;
    private static final Transform tmpT = new Transform();

    public BaseAnimationController(ModelInstance target) {
        this.target = target;
    }

    protected void begin() {
        if (this.applying) {
            throw new GdxRuntimeException("You must call end() after each call to being()");
        }
        this.applying = true;
    }

    protected void apply(Animation animation, float time, float weight) {
        if (!this.applying) {
            throw new GdxRuntimeException("You must call begin() before adding an animation");
        }
        BaseAnimationController.applyAnimation(transforms, this.transformPool, weight, animation, time);
    }

    protected void end() {
        if (!this.applying) {
            throw new GdxRuntimeException("You must call begin() first");
        }
        for (ObjectMap.Entry entry : transforms.entries()) {
            ((Transform)entry.value).toMatrix4(((Node)entry.key).localTransform);
            this.transformPool.free((Transform)entry.value);
        }
        transforms.clear();
        this.target.calculateTransforms();
        this.applying = false;
    }

    protected void applyAnimation(Animation animation, float time) {
        if (this.applying) {
            throw new GdxRuntimeException("Call end() first");
        }
        BaseAnimationController.applyAnimation(null, null, 1.0f, animation, time);
        this.target.calculateTransforms();
    }

    protected void applyAnimations(Animation anim1, float time1, Animation anim2, float time2, float weight) {
        if (anim2 == null || weight == 0.0f) {
            this.applyAnimation(anim1, time1);
        } else if (anim1 == null || weight == 1.0f) {
            this.applyAnimation(anim2, time2);
        } else {
            if (this.applying) {
                throw new GdxRuntimeException("Call end() first");
            }
            this.begin();
            this.apply(anim1, time1, 1.0f);
            this.apply(anim2, time2, weight);
            this.end();
        }
    }

    private static final <T> int getFirstKeyframeIndexAtTime(Array<NodeKeyframe<T>> arr, float time) {
        int n = arr.size - 1;
        for (int i = 0; i < n; ++i) {
            if (!(time >= arr.get((int)i).keytime) || !(time <= arr.get((int)(i + 1)).keytime)) continue;
            return i;
        }
        return 0;
    }

    private static final Vector3 getTranslationAtTime(NodeAnimation nodeAnim, float time, Vector3 out) {
        if (nodeAnim.translation == null) {
            return out.set(nodeAnim.node.translation);
        }
        if (nodeAnim.translation.size == 1) {
            return out.set((Vector3)nodeAnim.translation.get((int)0).value);
        }
        int index = BaseAnimationController.getFirstKeyframeIndexAtTime(nodeAnim.translation, time);
        NodeKeyframe<Vector3> firstKeyframe = nodeAnim.translation.get(index);
        out.set((Vector3)firstKeyframe.value);
        if (++index < nodeAnim.translation.size) {
            NodeKeyframe<Vector3> secondKeyframe = nodeAnim.translation.get(index);
            float t = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime);
            out.lerp((Vector3)secondKeyframe.value, t);
        }
        return out;
    }

    private static final Quaternion getRotationAtTime(NodeAnimation nodeAnim, float time, Quaternion out) {
        if (nodeAnim.rotation == null) {
            return out.set(nodeAnim.node.rotation);
        }
        if (nodeAnim.rotation.size == 1) {
            return out.set((Quaternion)nodeAnim.rotation.get((int)0).value);
        }
        int index = BaseAnimationController.getFirstKeyframeIndexAtTime(nodeAnim.rotation, time);
        NodeKeyframe<Quaternion> firstKeyframe = nodeAnim.rotation.get(index);
        out.set((Quaternion)firstKeyframe.value);
        if (++index < nodeAnim.rotation.size) {
            NodeKeyframe<Quaternion> secondKeyframe = nodeAnim.rotation.get(index);
            float t = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime);
            out.slerp((Quaternion)secondKeyframe.value, t);
        }
        return out;
    }

    private static final Vector3 getScalingAtTime(NodeAnimation nodeAnim, float time, Vector3 out) {
        if (nodeAnim.scaling == null) {
            return out.set(nodeAnim.node.scale);
        }
        if (nodeAnim.scaling.size == 1) {
            return out.set((Vector3)nodeAnim.scaling.get((int)0).value);
        }
        int index = BaseAnimationController.getFirstKeyframeIndexAtTime(nodeAnim.scaling, time);
        NodeKeyframe<Vector3> firstKeyframe = nodeAnim.scaling.get(index);
        out.set((Vector3)firstKeyframe.value);
        if (++index < nodeAnim.scaling.size) {
            NodeKeyframe<Vector3> secondKeyframe = nodeAnim.scaling.get(index);
            float t = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime);
            out.lerp((Vector3)secondKeyframe.value, t);
        }
        return out;
    }

    private static final Transform getNodeAnimationTransform(NodeAnimation nodeAnim, float time) {
        Transform transform = tmpT;
        BaseAnimationController.getTranslationAtTime(nodeAnim, time, transform.translation);
        BaseAnimationController.getRotationAtTime(nodeAnim, time, transform.rotation);
        BaseAnimationController.getScalingAtTime(nodeAnim, time, transform.scale);
        return transform;
    }

    private static final void applyNodeAnimationDirectly(NodeAnimation nodeAnim, float time) {
        Node node = nodeAnim.node;
        node.isAnimated = true;
        Transform transform = BaseAnimationController.getNodeAnimationTransform(nodeAnim, time);
        transform.toMatrix4(node.localTransform);
    }

    private static final void applyNodeAnimationBlending(NodeAnimation nodeAnim, ObjectMap<Node, Transform> out, Pool<Transform> pool, float alpha, float time) {
        Node node = nodeAnim.node;
        node.isAnimated = true;
        Transform transform = BaseAnimationController.getNodeAnimationTransform(nodeAnim, time);
        Transform t = out.get(node, null);
        if (t != null) {
            if (alpha > 0.999999f) {
                t.set(transform);
            } else {
                t.lerp(transform, alpha);
            }
        } else if (alpha > 0.999999f) {
            out.put(node, pool.obtain().set(transform));
        } else {
            out.put(node, pool.obtain().set(node.translation, node.rotation, node.scale).lerp(transform, alpha));
        }
    }

    protected static void applyAnimation(ObjectMap<Node, Transform> out, Pool<Transform> pool, float alpha, Animation animation, float time) {
        if (out == null) {
            for (NodeAnimation nodeAnimation : animation.nodeAnimations) {
                BaseAnimationController.applyNodeAnimationDirectly(nodeAnimation, time);
            }
        } else {
            for (Node node : out.keys()) {
                node.isAnimated = false;
            }
            for (NodeAnimation nodeAnimation : animation.nodeAnimations) {
                BaseAnimationController.applyNodeAnimationBlending(nodeAnimation, out, pool, alpha, time);
            }
            for (ObjectMap.Entry entry : out.entries()) {
                if (((Node)entry.key).isAnimated) continue;
                ((Node)entry.key).isAnimated = true;
                ((Transform)entry.value).lerp(((Node)entry.key).translation, ((Node)entry.key).rotation, ((Node)entry.key).scale, alpha);
            }
        }
    }

    protected void removeAnimation(Animation animation) {
        for (NodeAnimation nodeAnim : animation.nodeAnimations) {
            nodeAnim.node.isAnimated = false;
        }
    }

    public static final class Transform
    implements Pool.Poolable {
        public final Vector3 translation = new Vector3();
        public final Quaternion rotation = new Quaternion();
        public final Vector3 scale = new Vector3(1.0f, 1.0f, 1.0f);

        public Transform idt() {
            this.translation.set(0.0f, 0.0f, 0.0f);
            this.rotation.idt();
            this.scale.set(1.0f, 1.0f, 1.0f);
            return this;
        }

        public Transform set(Vector3 t, Quaternion r, Vector3 s) {
            this.translation.set(t);
            this.rotation.set(r);
            this.scale.set(s);
            return this;
        }

        public Transform set(Transform other) {
            return this.set(other.translation, other.rotation, other.scale);
        }

        public Transform lerp(Transform target, float alpha) {
            return this.lerp(target.translation, target.rotation, target.scale, alpha);
        }

        public Transform lerp(Vector3 targetT, Quaternion targetR, Vector3 targetS, float alpha) {
            this.translation.lerp(targetT, alpha);
            this.rotation.slerp(targetR, alpha);
            this.scale.lerp(targetS, alpha);
            return this;
        }

        public Matrix4 toMatrix4(Matrix4 out) {
            return out.set(this.translation, this.rotation, this.scale);
        }

        @Override
        public void reset() {
            this.idt();
        }

        public String toString() {
            return this.translation.toString() + " - " + this.rotation.toString() + " - " + this.scale.toString();
        }
    }
}

