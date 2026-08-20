/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.Selection;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;

public class Tree
extends WidgetGroup {
    TreeStyle style;
    final Array<Node> rootNodes = new Array();
    final Selection<Node> selection = new Selection<Node>(){

        @Override
        protected void changed() {
            switch (this.size()) {
                case 0: {
                    Tree.this.rangeStart = null;
                    break;
                }
                case 1: {
                    Tree.this.rangeStart = (Node)this.first();
                }
            }
        }
    };
    float ySpacing = 4.0f;
    float iconSpacingLeft = 2.0f;
    float iconSpacingRight = 2.0f;
    float padding = 0.0f;
    float indentSpacing;
    private float leftColumnWidth;
    private float prefWidth;
    private float prefHeight;
    private boolean sizeInvalid = true;
    private Node foundNode;
    Node overNode;
    Node rangeStart;
    private ClickListener clickListener;

    public Tree(Skin skin) {
        this(skin.get(TreeStyle.class));
    }

    public Tree(Skin skin, String styleName) {
        this(skin.get(styleName, TreeStyle.class));
    }

    public Tree(TreeStyle style) {
        this.selection.setActor(this);
        this.selection.setMultiple(true);
        this.setStyle(style);
        this.initialize();
    }

    private void initialize() {
        this.clickListener = new ClickListener(){

            @Override
            public void clicked(InputEvent event, float x, float y) {
                Node node = Tree.this.getNodeAt(y);
                if (node == null) {
                    return;
                }
                if (node != Tree.this.getNodeAt(this.getTouchDownY())) {
                    return;
                }
                if (Tree.this.selection.getMultiple() && Tree.this.selection.hasItems() && UIUtils.shift()) {
                    float end;
                    float start;
                    if (Tree.this.rangeStart == null) {
                        Tree.this.rangeStart = node;
                    }
                    Node rangeStart = Tree.this.rangeStart;
                    if (!UIUtils.ctrl()) {
                        Tree.this.selection.clear();
                    }
                    if ((start = rangeStart.actor.getY()) > (end = node.actor.getY())) {
                        Tree.this.selectNodes(Tree.this.rootNodes, end, start);
                    } else {
                        Tree.this.selectNodes(Tree.this.rootNodes, start, end);
                        Tree.this.selection.items().orderedItems().reverse();
                    }
                    Tree.this.selection.fireChangeEvent();
                    Tree.this.rangeStart = rangeStart;
                    return;
                }
                if (!(node.children.size <= 0 || Tree.this.selection.getMultiple() && UIUtils.ctrl())) {
                    float rowX = node.actor.getX();
                    if (node.icon != null) {
                        rowX -= Tree.this.iconSpacingRight + node.icon.getMinWidth();
                    }
                    if (x < rowX) {
                        node.setExpanded(!node.expanded);
                        return;
                    }
                }
                if (!node.isSelectable()) {
                    return;
                }
                Tree.this.selection.choose(node);
                if (!Tree.this.selection.isEmpty()) {
                    Tree.this.rangeStart = node;
                }
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                Tree.this.setOverNode(Tree.this.getNodeAt(y));
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (toActor == null || !toActor.isDescendantOf(Tree.this)) {
                    Tree.this.setOverNode(null);
                }
            }
        };
        this.addListener(this.clickListener);
    }

    public void setStyle(TreeStyle style) {
        this.style = style;
        this.indentSpacing = Math.max(style.plus.getMinWidth(), style.minus.getMinWidth()) + this.iconSpacingLeft;
    }

    public void add(Node node) {
        this.insert(this.rootNodes.size, node);
    }

    public void insert(int index, Node node) {
        this.remove(node);
        node.parent = null;
        this.rootNodes.insert(index, node);
        node.addToTree(this);
        this.invalidateHierarchy();
    }

    public void remove(Node node) {
        if (node.parent != null) {
            node.parent.remove(node);
            return;
        }
        this.rootNodes.removeValue(node, true);
        node.removeFromTree(this);
        this.invalidateHierarchy();
    }

    @Override
    public void clearChildren() {
        super.clearChildren();
        this.setOverNode(null);
        this.rootNodes.clear();
        this.selection.clear();
    }

    public Array<Node> getNodes() {
        return this.rootNodes;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.sizeInvalid = true;
    }

    private void computeSize() {
        this.sizeInvalid = false;
        this.prefWidth = this.style.plus.getMinWidth();
        this.prefWidth = Math.max(this.prefWidth, this.style.minus.getMinWidth());
        this.prefHeight = this.getHeight();
        this.leftColumnWidth = 0.0f;
        this.computeSize(this.rootNodes, this.indentSpacing);
        this.leftColumnWidth += this.iconSpacingLeft + this.padding;
        this.prefWidth += this.leftColumnWidth + this.padding;
        this.prefHeight = this.getHeight() - this.prefHeight;
    }

    private void computeSize(Array<Node> nodes, float indent) {
        float ySpacing = this.ySpacing;
        float spacing = this.iconSpacingLeft + this.iconSpacingRight;
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            float rowWidth = indent + this.iconSpacingRight;
            Actor actor = node.actor;
            if (actor instanceof Layout) {
                Layout layout = (Layout)((Object)actor);
                rowWidth += layout.getPrefWidth();
                node.height = layout.getPrefHeight();
                layout.pack();
            } else {
                rowWidth += actor.getWidth();
                node.height = actor.getHeight();
            }
            if (node.icon != null) {
                rowWidth += spacing + node.icon.getMinWidth();
                node.height = Math.max(node.height, node.icon.getMinHeight());
            }
            this.prefWidth = Math.max(this.prefWidth, rowWidth);
            this.prefHeight -= node.height + ySpacing;
            if (!node.expanded) continue;
            this.computeSize(node.children, indent + this.indentSpacing);
        }
    }

    @Override
    public void layout() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        this.layout(this.rootNodes, this.leftColumnWidth + this.indentSpacing + this.iconSpacingRight, this.getHeight() - this.ySpacing / 2.0f);
    }

    private float layout(Array<Node> nodes, float indent, float y) {
        float ySpacing = this.ySpacing;
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            float x = indent;
            if (node.icon != null) {
                x += node.icon.getMinWidth();
            }
            node.actor.setPosition(x, y -= node.height);
            y -= ySpacing;
            if (!node.expanded) continue;
            y = this.layout(node.children, indent + this.indentSpacing, y);
        }
        return y;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        if (this.style.background != null) {
            this.style.background.draw(batch, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        this.draw(batch, this.rootNodes, this.leftColumnWidth);
        super.draw(batch, parentAlpha);
    }

    private void draw(Batch batch, Array<Node> nodes, float indent) {
        Drawable plus = this.style.plus;
        Drawable minus = this.style.minus;
        float x = this.getX();
        float y = this.getY();
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            Actor actor = node.actor;
            if (this.selection.contains(node) && this.style.selection != null) {
                this.style.selection.draw(batch, x, y + actor.getY() - this.ySpacing / 2.0f, this.getWidth(), node.height + this.ySpacing);
            } else if (node == this.overNode && this.style.over != null) {
                this.style.over.draw(batch, x, y + actor.getY() - this.ySpacing / 2.0f, this.getWidth(), node.height + this.ySpacing);
            }
            if (node.icon != null) {
                float iconY = actor.getY() + (float)Math.round((node.height - node.icon.getMinHeight()) / 2.0f);
                batch.setColor(actor.getColor());
                node.icon.draw(batch, x + node.actor.getX() - this.iconSpacingRight - node.icon.getMinWidth(), y + iconY, node.icon.getMinWidth(), node.icon.getMinHeight());
                batch.setColor(Color.WHITE);
            }
            if (node.children.size == 0) continue;
            Drawable expandIcon = node.expanded ? minus : plus;
            float iconY = actor.getY() + (float)Math.round((node.height - expandIcon.getMinHeight()) / 2.0f);
            expandIcon.draw(batch, x + indent - this.iconSpacingLeft, y + iconY, expandIcon.getMinWidth(), expandIcon.getMinHeight());
            if (!node.expanded) continue;
            this.draw(batch, node.children, indent + this.indentSpacing);
        }
    }

    public Node getNodeAt(float y) {
        this.foundNode = null;
        this.getNodeAt(this.rootNodes, y, this.getHeight());
        return this.foundNode;
    }

    private float getNodeAt(Array<Node> nodes, float y, float rowY) {
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            if (y >= rowY - node.height - this.ySpacing && y < rowY) {
                this.foundNode = node;
                return -1.0f;
            }
            rowY -= node.height + this.ySpacing;
            if (!node.expanded || (rowY = this.getNodeAt(node.children, y, rowY)) != -1.0f) continue;
            return -1.0f;
        }
        return rowY;
    }

    void selectNodes(Array<Node> nodes, float low, float high) {
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            if (node.actor.getY() < low) break;
            if (!node.isSelectable()) continue;
            if (node.actor.getY() <= high) {
                this.selection.add(node);
            }
            if (!node.expanded) continue;
            this.selectNodes(node.children, low, high);
        }
    }

    public Selection<Node> getSelection() {
        return this.selection;
    }

    public TreeStyle getStyle() {
        return this.style;
    }

    public Array<Node> getRootNodes() {
        return this.rootNodes;
    }

    public Node getOverNode() {
        return this.overNode;
    }

    public Object getOverObject() {
        if (this.overNode == null) {
            return null;
        }
        return this.overNode.getObject();
    }

    public void setOverNode(Node overNode) {
        this.overNode = overNode;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    public float getIndentSpacing() {
        return this.indentSpacing;
    }

    public void setYSpacing(float ySpacing) {
        this.ySpacing = ySpacing;
    }

    public float getYSpacing() {
        return this.ySpacing;
    }

    public void setIconSpacing(float left, float right) {
        this.iconSpacingLeft = left;
        this.iconSpacingRight = right;
    }

    @Override
    public float getPrefWidth() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefWidth;
    }

    @Override
    public float getPrefHeight() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefHeight;
    }

    public void findExpandedObjects(Array objects) {
        Tree.findExpandedObjects(this.rootNodes, objects);
    }

    public void restoreExpandedObjects(Array objects) {
        int n = objects.size;
        for (int i = 0; i < n; ++i) {
            Node node = this.findNode(objects.get(i));
            if (node == null) continue;
            node.setExpanded(true);
            node.expandTo();
        }
    }

    static boolean findExpandedObjects(Array<Node> nodes, Array objects) {
        boolean expanded = false;
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            if (!node.expanded || Tree.findExpandedObjects(node.children, objects)) continue;
            objects.add(node.object);
        }
        return expanded;
    }

    public Node findNode(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null.");
        }
        return Tree.findNode(this.rootNodes, object);
    }

    static Node findNode(Array<Node> nodes, Object object) {
        Node node;
        int i;
        int n = nodes.size;
        for (i = 0; i < n; ++i) {
            node = nodes.get(i);
            if (!object.equals(node.object)) continue;
            return node;
        }
        n = nodes.size;
        for (i = 0; i < n; ++i) {
            node = nodes.get(i);
            Node found = Tree.findNode(node.children, object);
            if (found == null) continue;
            return found;
        }
        return null;
    }

    public void collapseAll() {
        Tree.collapseAll(this.rootNodes);
    }

    static void collapseAll(Array<Node> nodes) {
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            Node node = nodes.get(i);
            node.setExpanded(false);
            Tree.collapseAll(node.children);
        }
    }

    public void expandAll() {
        Tree.expandAll(this.rootNodes);
    }

    static void expandAll(Array<Node> nodes) {
        int n = nodes.size;
        for (int i = 0; i < n; ++i) {
            nodes.get(i).expandAll();
        }
    }

    public ClickListener getClickListener() {
        return this.clickListener;
    }

    public static class TreeStyle {
        public Drawable plus;
        public Drawable minus;
        public Drawable over;
        public Drawable selection;
        public Drawable background;

        public TreeStyle() {
        }

        public TreeStyle(Drawable plus, Drawable minus, Drawable selection) {
            this.plus = plus;
            this.minus = minus;
            this.selection = selection;
        }

        public TreeStyle(TreeStyle style) {
            this.plus = style.plus;
            this.minus = style.minus;
            this.selection = style.selection;
        }
    }

    public static class Node {
        final Actor actor;
        Node parent;
        final Array<Node> children = new Array(0);
        boolean selectable = true;
        boolean expanded;
        Drawable icon;
        float height;
        Object object;

        public Node(Actor actor) {
            if (actor == null) {
                throw new IllegalArgumentException("actor cannot be null.");
            }
            this.actor = actor;
        }

        public void setExpanded(boolean expanded) {
            if (expanded == this.expanded) {
                return;
            }
            this.expanded = expanded;
            if (this.children.size == 0) {
                return;
            }
            Tree tree = this.getTree();
            if (tree == null) {
                return;
            }
            if (expanded) {
                int n = this.children.size;
                for (int i = 0; i < n; ++i) {
                    this.children.get(i).addToTree(tree);
                }
            } else {
                int n = this.children.size;
                for (int i = 0; i < n; ++i) {
                    this.children.get(i).removeFromTree(tree);
                }
            }
            tree.invalidateHierarchy();
        }

        protected void addToTree(Tree tree) {
            tree.addActor(this.actor);
            if (!this.expanded) {
                return;
            }
            int n = this.children.size;
            for (int i = 0; i < n; ++i) {
                this.children.get(i).addToTree(tree);
            }
        }

        protected void removeFromTree(Tree tree) {
            tree.removeActor(this.actor);
            if (!this.expanded) {
                return;
            }
            T[] children = this.children.items;
            int n = this.children.size;
            for (int i = 0; i < n; ++i) {
                ((Node)children[i]).removeFromTree(tree);
            }
        }

        public void add(Node node) {
            this.insert(this.children.size, node);
        }

        public void addAll(Array<Node> nodes) {
            int n = nodes.size;
            for (int i = 0; i < n; ++i) {
                this.insert(this.children.size, nodes.get(i));
            }
        }

        public void insert(int index, Node node) {
            node.parent = this;
            this.children.insert(index, node);
            this.updateChildren();
        }

        public void remove() {
            Tree tree = this.getTree();
            if (tree != null) {
                tree.remove(this);
            } else if (this.parent != null) {
                this.parent.remove(this);
            }
        }

        public void remove(Node node) {
            this.children.removeValue(node, true);
            if (!this.expanded) {
                return;
            }
            Tree tree = this.getTree();
            if (tree == null) {
                return;
            }
            node.removeFromTree(tree);
            if (this.children.size == 0) {
                this.expanded = false;
            }
        }

        public void removeAll() {
            Tree tree = this.getTree();
            if (tree != null) {
                int n = this.children.size;
                for (int i = 0; i < n; ++i) {
                    this.children.get(i).removeFromTree(tree);
                }
            }
            this.children.clear();
        }

        public Tree getTree() {
            Group parent = this.actor.getParent();
            if (!(parent instanceof Tree)) {
                return null;
            }
            return (Tree)parent;
        }

        public Actor getActor() {
            return this.actor;
        }

        public boolean isExpanded() {
            return this.expanded;
        }

        public Array<Node> getChildren() {
            return this.children;
        }

        public void updateChildren() {
            if (!this.expanded) {
                return;
            }
            Tree tree = this.getTree();
            if (tree == null) {
                return;
            }
            int n = this.children.size;
            for (int i = 0; i < n; ++i) {
                this.children.get(i).addToTree(tree);
            }
        }

        public Node getParent() {
            return this.parent;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public Object getObject() {
            return this.object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public int getLevel() {
            int level = 0;
            Node current = this;
            do {
                ++level;
            } while ((current = current.getParent()) != null);
            return level;
        }

        public Node findNode(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            if (object.equals(this.object)) {
                return this;
            }
            return Tree.findNode(this.children, object);
        }

        public void collapseAll() {
            this.setExpanded(false);
            Tree.collapseAll(this.children);
        }

        public void expandAll() {
            this.setExpanded(true);
            if (this.children.size > 0) {
                Tree.expandAll(this.children);
            }
        }

        public void expandTo() {
            Node node = this.parent;
            while (node != null) {
                node.setExpanded(true);
                node = node.parent;
            }
        }

        public boolean isSelectable() {
            return this.selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public void findExpandedObjects(Array objects) {
            if (this.expanded && !Tree.findExpandedObjects(this.children, objects)) {
                objects.add(this.object);
            }
        }

        public void restoreExpandedObjects(Array objects) {
            int n = objects.size;
            for (int i = 0; i < n; ++i) {
                Node node = this.findNode(objects.get(i));
                if (node == null) continue;
                node.setExpanded(true);
                node.expandTo();
            }
        }
    }
}

