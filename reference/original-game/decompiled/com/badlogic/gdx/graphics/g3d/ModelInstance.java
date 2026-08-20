/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Pool;
import java.io.Serializable;

public class ModelInstance
implements RenderableProvider {
    public static boolean defaultShareKeyframes = true;
    public final Array<Material> materials = new Array();
    public final Array<Node> nodes = new Array();
    public final Array<Animation> animations = new Array();
    public final Model model;
    public Matrix4 transform;
    public Object userData;

    public ModelInstance(Model model) {
        this(model, (String[])null);
    }

    public ModelInstance(Model model, String nodeId, boolean mergeTransform) {
        this(model, null, nodeId, false, false, mergeTransform);
    }

    public ModelInstance(Model model, Matrix4 transform, String nodeId, boolean mergeTransform) {
        this(model, transform, nodeId, false, false, mergeTransform);
    }

    public ModelInstance(Model model, String nodeId, boolean parentTransform, boolean mergeTransform) {
        this(model, null, nodeId, true, parentTransform, mergeTransform);
    }

    public ModelInstance(Model model, Matrix4 transform, String nodeId, boolean parentTransform, boolean mergeTransform) {
        this(model, transform, nodeId, true, parentTransform, mergeTransform);
    }

    public ModelInstance(Model model, String nodeId, boolean recursive, boolean parentTransform, boolean mergeTransform) {
        this(model, null, nodeId, recursive, parentTransform, mergeTransform);
    }

    public ModelInstance(Model model, Matrix4 transform, String nodeId, boolean recursive, boolean parentTransform, boolean mergeTransform) {
        this(model, transform, nodeId, recursive, parentTransform, mergeTransform, defaultShareKeyframes);
    }

    public ModelInstance(Model model, Matrix4 transform, String nodeId, boolean recursive, boolean parentTransform, boolean mergeTransform, boolean shareKeyframes) {
        this.model = model;
        this.transform = transform == null ? new Matrix4() : transform;
        Node node = model.getNode(nodeId, recursive);
        Node copy = node.copy();
        this.nodes.add(copy);
        if (mergeTransform) {
            this.transform.mul(parentTransform ? node.globalTransform : node.localTransform);
            copy.translation.set(0.0f, 0.0f, 0.0f);
            copy.rotation.idt();
            copy.scale.set(1.0f, 1.0f, 1.0f);
        } else if (parentTransform && copy.hasParent()) {
            this.transform.mul(node.getParent().globalTransform);
        }
        this.invalidate();
        this.copyAnimations(model.animations, shareKeyframes);
        this.calculateTransforms();
    }

    public ModelInstance(Model model, String ... rootNodeIds) {
        this(model, (Matrix4)null, rootNodeIds);
    }

    public ModelInstance(Model model, Matrix4 transform, String ... rootNodeIds) {
        this.model = model;
        Matrix4 matrix4 = this.transform = transform == null ? new Matrix4() : transform;
        if (rootNodeIds == null) {
            this.copyNodes(model.nodes);
        } else {
            this.copyNodes(model.nodes, rootNodeIds);
        }
        this.copyAnimations(model.animations, defaultShareKeyframes);
        this.calculateTransforms();
    }

    public ModelInstance(Model model, Array<String> rootNodeIds) {
        this(model, null, rootNodeIds);
    }

    public ModelInstance(Model model, Matrix4 transform, Array<String> rootNodeIds) {
        this(model, transform, rootNodeIds, defaultShareKeyframes);
    }

    public ModelInstance(Model model, Matrix4 transform, Array<String> rootNodeIds, boolean shareKeyframes) {
        this.model = model;
        this.transform = transform == null ? new Matrix4() : transform;
        this.copyNodes(model.nodes, rootNodeIds);
        this.copyAnimations(model.animations, shareKeyframes);
        this.calculateTransforms();
    }

    public ModelInstance(Model model, Vector3 position) {
        this(model);
        this.transform.setToTranslation(position);
    }

    public ModelInstance(Model model, float x, float y, float z) {
        this(model);
        this.transform.setToTranslation(x, y, z);
    }

    public ModelInstance(Model model, Matrix4 transform) {
        this(model, transform, (String[])null);
    }

    public ModelInstance(ModelInstance copyFrom) {
        this(copyFrom, copyFrom.transform.cpy());
    }

    public ModelInstance(ModelInstance copyFrom, Matrix4 transform) {
        this(copyFrom, transform, defaultShareKeyframes);
    }

    public ModelInstance(ModelInstance copyFrom, Matrix4 transform, boolean shareKeyframes) {
        this.model = copyFrom.model;
        this.transform = transform == null ? new Matrix4() : transform;
        this.copyNodes(copyFrom.nodes);
        this.copyAnimations(copyFrom.animations, shareKeyframes);
        this.calculateTransforms();
    }

    public ModelInstance copy() {
        return new ModelInstance(this);
    }

    private void copyNodes(Array<Node> nodes) {
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            this.nodes.add(node.copy());
        }
        this.invalidate();
    }

    private void copyNodes(Array<Node> nodes, String ... nodeIds) {
        int n = nodes.size;
        block0: for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            for (String nodeId : nodeIds) {
                if (!nodeId.equals(node.id)) continue;
                this.nodes.add(node.copy());
                continue block0;
            }
        }
        this.invalidate();
    }

    private void copyNodes(Array<Node> nodes, Array<String> nodeIds) {
        int n = nodes.size;
        block0: for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            for (String nodeId : nodeIds) {
                if (!nodeId.equals(node.id)) continue;
                this.nodes.add(node.copy());
                continue block0;
            }
        }
        this.invalidate();
    }

    private void invalidate(Node node) {
        int i;
        int n = node.parts.size;
        for (i = 0; i < n; ++i) {
            NodePart part = node.parts.get(i);
            ArrayMap<Node, Matrix4> bindPose = part.invBoneBindTransforms;
            if (bindPose != null) {
                for (int j = 0; j < bindPose.size; ++j) {
                    ((Node[])bindPose.keys)[j] = this.getNode(((Node[])bindPose.keys)[j].id);
                }
            }
            if (this.materials.contains(part.material, true)) continue;
            int midx = this.materials.indexOf(part.material, false);
            if (midx < 0) {
                part.material = part.material.copy();
                this.materials.add(part.material);
                continue;
            }
            part.material = this.materials.get(midx);
        }
        n = node.getChildCount();
        for (i = 0; i < n; ++i) {
            this.invalidate(node.getChild(i));
        }
    }

    private void invalidate() {
        int n = this.nodes.size;
        for (int i = 0; i < n; ++i) {
            this.invalidate(this.nodes.get(i));
        }
    }

    private void copyAnimations(Iterable<Animation> source, boolean shareKeyframes) {
        for (Animation anim : source) {
            Animation animation = new Animation();
            animation.id = anim.id;
            animation.duration = anim.duration;
            for (NodeAnimation nanim : anim.nodeAnimations) {
                Node node = this.getNode(nanim.node.id);
                if (node == null) continue;
                NodeAnimation nodeAnim = new NodeAnimation();
                nodeAnim.node = node;
                if (shareKeyframes) {
                    nodeAnim.translation = nanim.translation;
                    nodeAnim.rotation = nanim.rotation;
                    nodeAnim.scaling = nanim.scaling;
                } else {
                    if (nanim.translation != null) {
                        nodeAnim.translation = new Array();
                        for (NodeKeyframe<Serializable> nodeKeyframe : nanim.translation) {
                            nodeAnim.translation.add(new NodeKeyframe(nodeKeyframe.keytime, nodeKeyframe.value));
                        }
                    }
                    if (nanim.rotation != null) {
                        nodeAnim.rotation = new Array();
                        for (NodeKeyframe<Serializable> nodeKeyframe : nanim.rotation) {
                            nodeAnim.rotation.add(new NodeKeyframe(nodeKeyframe.keytime, nodeKeyframe.value));
                        }
                    }
                    if (nanim.scaling != null) {
                        nodeAnim.scaling = new Array();
                        for (NodeKeyframe<Serializable> nodeKeyframe : nanim.scaling) {
                            nodeAnim.scaling.add(new NodeKeyframe(nodeKeyframe.keytime, nodeKeyframe.value));
                        }
                    }
                }
                if (nodeAnim.translation == null && nodeAnim.rotation == null && nodeAnim.scaling == null) continue;
                animation.nodeAnimations.add(nodeAnim);
            }
            if (animation.nodeAnimations.size <= 0) continue;
            this.animations.add(animation);
        }
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        for (Node node : this.nodes) {
            this.getRenderables(node, renderables, pool);
        }
    }

    public Renderable getRenderable(Renderable out) {
        return this.getRenderable(out, this.nodes.get(0));
    }

    public Renderable getRenderable(Renderable out, Node node) {
        return this.getRenderable(out, node, node.parts.get(0));
    }

    public Renderable getRenderable(Renderable out, Node node, NodePart nodePart) {
        nodePart.setRenderable(out);
        if (nodePart.bones == null && this.transform != null) {
            out.worldTransform.set(this.transform).mul(node.globalTransform);
        } else if (this.transform != null) {
            out.worldTransform.set(this.transform);
        } else {
            out.worldTransform.idt();
        }
        out.userData = this.userData;
        return out;
    }

    protected void getRenderables(Node node, Array<Renderable> renderables, Pool<Renderable> pool) {
        if (node.parts.size > 0) {
            for (NodePart nodePart : node.parts) {
                if (!nodePart.enabled) continue;
                renderables.add(this.getRenderable(pool.obtain(), node, nodePart));
            }
        }
        for (Node child : node.getChildren()) {
            this.getRenderables(child, renderables, pool);
        }
    }

    public void calculateTransforms() {
        int i;
        int n = this.nodes.size;
        for (i = 0; i < n; ++i) {
            this.nodes.get(i).calculateTransforms(true);
        }
        for (i = 0; i < n; ++i) {
            this.nodes.get(i).calculateBoneTransforms(true);
        }
    }

    public BoundingBox calculateBoundingBox(BoundingBox out) {
        out.inf();
        return this.extendBoundingBox(out);
    }

    public BoundingBox extendBoundingBox(BoundingBox out) {
        int n = this.nodes.size;
        for (int i = 0; i < n; ++i) {
            this.nodes.get(i).extendBoundingBox(out);
        }
        return out;
    }

    public Animation getAnimation(String id) {
        return this.getAnimation(id, false);
    }

    public Animation getAnimation(String id, boolean ignoreCase) {
        int n = this.animations.size;
        if (ignoreCase) {
            for (int i = 0; i < n; ++i) {
                Animation animation = this.animations.get(i);
                if (!animation.id.equalsIgnoreCase(id)) continue;
                return animation;
            }
        } else {
            for (int i = 0; i < n; ++i) {
                Animation animation = this.animations.get(i);
                if (!animation.id.equals(id)) continue;
                return animation;
            }
        }
        return null;
    }

    public Material getMaterial(String id) {
        return this.getMaterial(id, true);
    }

    public Material getMaterial(String id, boolean ignoreCase) {
        int n = this.materials.size;
        if (ignoreCase) {
            for (int i = 0; i < n; ++i) {
                Material material = this.materials.get(i);
                if (!material.id.equalsIgnoreCase(id)) continue;
                return material;
            }
        } else {
            for (int i = 0; i < n; ++i) {
                Material material = this.materials.get(i);
                if (!material.id.equals(id)) continue;
                return material;
            }
        }
        return null;
    }

    public Node getNode(String id) {
        return this.getNode(id, true);
    }

    public Node getNode(String id, boolean recursive) {
        return this.getNode(id, recursive, false);
    }

    public Node getNode(String id, boolean recursive, boolean ignoreCase) {
        return Node.getNode(this.nodes, id, recursive, ignoreCase);
    }
}

