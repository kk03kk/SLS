/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ModelBuilder {
    private Model model;
    private Node node;
    private Array<MeshBuilder> builders = new Array();
    private Matrix4 tmpTransform = new Matrix4();

    private MeshBuilder getBuilder(VertexAttributes attributes) {
        for (MeshBuilder mb : this.builders) {
            if (!mb.getAttributes().equals(attributes) || mb.lastIndex() >= 16383) continue;
            return mb;
        }
        MeshBuilder result = new MeshBuilder();
        result.begin(attributes);
        this.builders.add(result);
        return result;
    }

    public void begin() {
        if (this.model != null) {
            throw new GdxRuntimeException("Call end() first");
        }
        this.node = null;
        this.model = new Model();
        this.builders.clear();
    }

    public Model end() {
        if (this.model == null) {
            throw new GdxRuntimeException("Call begin() first");
        }
        Model result = this.model;
        this.endnode();
        this.model = null;
        for (MeshBuilder mb : this.builders) {
            mb.end();
        }
        this.builders.clear();
        ModelBuilder.rebuildReferences(result);
        return result;
    }

    private void endnode() {
        if (this.node != null) {
            this.node = null;
        }
    }

    protected Node node(Node node) {
        if (this.model == null) {
            throw new GdxRuntimeException("Call begin() first");
        }
        this.endnode();
        this.model.nodes.add(node);
        this.node = node;
        return node;
    }

    public Node node() {
        Node node = new Node();
        this.node(node);
        node.id = "node" + this.model.nodes.size;
        return node;
    }

    public Node node(String id, Model model) {
        Node node = new Node();
        node.id = id;
        node.addChildren(model.nodes);
        this.node(node);
        for (Disposable disposable : model.getManagedDisposables()) {
            this.manage(disposable);
        }
        return node;
    }

    public void manage(Disposable disposable) {
        if (this.model == null) {
            throw new GdxRuntimeException("Call begin() first");
        }
        this.model.manageDisposable(disposable);
    }

    public void part(MeshPart meshpart, Material material) {
        if (this.node == null) {
            this.node();
        }
        this.node.parts.add(new NodePart(meshpart, material));
    }

    public MeshPart part(String id, Mesh mesh, int primitiveType, int offset, int size, Material material) {
        MeshPart meshPart = new MeshPart();
        meshPart.id = id;
        meshPart.primitiveType = primitiveType;
        meshPart.mesh = mesh;
        meshPart.offset = offset;
        meshPart.size = size;
        this.part(meshPart, material);
        return meshPart;
    }

    public MeshPart part(String id, Mesh mesh, int primitiveType, Material material) {
        return this.part(id, mesh, primitiveType, 0, mesh.getNumIndices(), material);
    }

    public MeshPartBuilder part(String id, int primitiveType, VertexAttributes attributes, Material material) {
        MeshBuilder builder = this.getBuilder(attributes);
        this.part(builder.part(id, primitiveType), material);
        return builder;
    }

    public MeshPartBuilder part(String id, int primitiveType, long attributes, Material material) {
        return this.part(id, primitiveType, MeshBuilder.createAttributes(attributes), material);
    }

    public Model createBox(float width, float height, float depth, Material material, long attributes) {
        return this.createBox(width, height, depth, 4, material, attributes);
    }

    public Model createBox(float width, float height, float depth, int primitiveType, Material material, long attributes) {
        this.begin();
        this.part("box", primitiveType, attributes, material).box(width, height, depth);
        return this.end();
    }

    public Model createRect(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ, Material material, long attributes) {
        return this.createRect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, 4, material, attributes);
    }

    public Model createRect(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ, int primitiveType, Material material, long attributes) {
        this.begin();
        this.part("rect", primitiveType, attributes, material).rect(x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ);
        return this.end();
    }

    public Model createCylinder(float width, float height, float depth, int divisions, Material material, long attributes) {
        return this.createCylinder(width, height, depth, divisions, 4, material, attributes);
    }

    public Model createCylinder(float width, float height, float depth, int divisions, int primitiveType, Material material, long attributes) {
        return this.createCylinder(width, height, depth, divisions, primitiveType, material, attributes, 0.0f, 360.0f);
    }

    public Model createCylinder(float width, float height, float depth, int divisions, Material material, long attributes, float angleFrom, float angleTo) {
        return this.createCylinder(width, height, depth, divisions, 4, material, attributes, angleFrom, angleTo);
    }

    public Model createCylinder(float width, float height, float depth, int divisions, int primitiveType, Material material, long attributes, float angleFrom, float angleTo) {
        this.begin();
        this.part("cylinder", primitiveType, attributes, material).cylinder(width, height, depth, divisions, angleFrom, angleTo);
        return this.end();
    }

    public Model createCone(float width, float height, float depth, int divisions, Material material, long attributes) {
        return this.createCone(width, height, depth, divisions, 4, material, attributes);
    }

    public Model createCone(float width, float height, float depth, int divisions, int primitiveType, Material material, long attributes) {
        return this.createCone(width, height, depth, divisions, primitiveType, material, attributes, 0.0f, 360.0f);
    }

    public Model createCone(float width, float height, float depth, int divisions, Material material, long attributes, float angleFrom, float angleTo) {
        return this.createCone(width, height, depth, divisions, 4, material, attributes, angleFrom, angleTo);
    }

    public Model createCone(float width, float height, float depth, int divisions, int primitiveType, Material material, long attributes, float angleFrom, float angleTo) {
        this.begin();
        this.part("cone", primitiveType, attributes, material).cone(width, height, depth, divisions, angleFrom, angleTo);
        return this.end();
    }

    public Model createSphere(float width, float height, float depth, int divisionsU, int divisionsV, Material material, long attributes) {
        return this.createSphere(width, height, depth, divisionsU, divisionsV, 4, material, attributes);
    }

    public Model createSphere(float width, float height, float depth, int divisionsU, int divisionsV, int primitiveType, Material material, long attributes) {
        return this.createSphere(width, height, depth, divisionsU, divisionsV, primitiveType, material, attributes, 0.0f, 360.0f, 0.0f, 180.0f);
    }

    public Model createSphere(float width, float height, float depth, int divisionsU, int divisionsV, Material material, long attributes, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        return this.createSphere(width, height, depth, divisionsU, divisionsV, 4, material, attributes, angleUFrom, angleUTo, angleVFrom, angleVTo);
    }

    public Model createSphere(float width, float height, float depth, int divisionsU, int divisionsV, int primitiveType, Material material, long attributes, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        this.begin();
        this.part("cylinder", primitiveType, attributes, material).sphere(width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo);
        return this.end();
    }

    public Model createCapsule(float radius, float height, int divisions, Material material, long attributes) {
        return this.createCapsule(radius, height, divisions, 4, material, attributes);
    }

    public Model createCapsule(float radius, float height, int divisions, int primitiveType, Material material, long attributes) {
        this.begin();
        this.part("capsule", primitiveType, attributes, material).capsule(radius, height, divisions);
        return this.end();
    }

    public static void rebuildReferences(Model model) {
        model.materials.clear();
        model.meshes.clear();
        model.meshParts.clear();
        for (Node node : model.nodes) {
            ModelBuilder.rebuildReferences(model, node);
        }
    }

    private static void rebuildReferences(Model model, Node node) {
        for (NodePart mpm : node.parts) {
            if (!model.materials.contains(mpm.material, true)) {
                model.materials.add(mpm.material);
            }
            if (model.meshParts.contains(mpm.meshPart, true)) continue;
            model.meshParts.add(mpm.meshPart);
            if (!model.meshes.contains(mpm.meshPart.mesh, true)) {
                model.meshes.add(mpm.meshPart.mesh);
            }
            model.manageDisposable(mpm.meshPart.mesh);
        }
        for (Node child : node.getChildren()) {
            ModelBuilder.rebuildReferences(model, child);
        }
    }

    public Model createXYZCoordinates(float axisLength, float capLength, float stemThickness, int divisions, int primitiveType, Material material, long attributes) {
        this.begin();
        Node node = this.node();
        MeshPartBuilder partBuilder = this.part("xyz", primitiveType, attributes, material);
        partBuilder.setColor(Color.RED);
        partBuilder.arrow(0.0f, 0.0f, 0.0f, axisLength, 0.0f, 0.0f, capLength, stemThickness, divisions);
        partBuilder.setColor(Color.GREEN);
        partBuilder.arrow(0.0f, 0.0f, 0.0f, 0.0f, axisLength, 0.0f, capLength, stemThickness, divisions);
        partBuilder.setColor(Color.BLUE);
        partBuilder.arrow(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, axisLength, capLength, stemThickness, divisions);
        return this.end();
    }

    public Model createXYZCoordinates(float axisLength, Material material, long attributes) {
        return this.createXYZCoordinates(axisLength, 0.1f, 0.1f, 5, 4, material, attributes);
    }

    public Model createArrow(float x1, float y1, float z1, float x2, float y2, float z2, float capLength, float stemThickness, int divisions, int primitiveType, Material material, long attributes) {
        this.begin();
        this.part("arrow", primitiveType, attributes, material).arrow(x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions);
        return this.end();
    }

    public Model createArrow(Vector3 from, Vector3 to, Material material, long attributes) {
        return this.createArrow(from.x, from.y, from.z, to.x, to.y, to.z, 0.1f, 0.1f, 5, 4, material, attributes);
    }

    public Model createLineGrid(int xDivisions, int zDivisions, float xSize, float zSize, Material material, long attributes) {
        this.begin();
        MeshPartBuilder partBuilder = this.part("lines", 1, attributes, material);
        float xlength = (float)xDivisions * xSize;
        float zlength = (float)zDivisions * zSize;
        float hxlength = xlength / 2.0f;
        float hzlength = zlength / 2.0f;
        float x1 = -hxlength;
        float y1 = 0.0f;
        float z1 = hzlength;
        float x2 = -hxlength;
        float y2 = 0.0f;
        float z2 = -hzlength;
        for (int i = 0; i <= xDivisions; ++i) {
            partBuilder.line(x1, y1, z1, x2, y2, z2);
            x1 += xSize;
            x2 += xSize;
        }
        x1 = -hxlength;
        y1 = 0.0f;
        z1 = -hzlength;
        x2 = hxlength;
        y2 = 0.0f;
        z2 = -hzlength;
        for (int j = 0; j <= zDivisions; ++j) {
            partBuilder.line(x1, y1, z1, x2, y2, z2);
            z1 += zSize;
            z2 += zSize;
        }
        return this.end();
    }
}

