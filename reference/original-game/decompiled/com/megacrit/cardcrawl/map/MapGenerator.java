/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.map;

import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.map.EdgeComparator;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.random.Random;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapGenerator {
    private static final Logger logger = LogManager.getLogger(MapGenerator.class.getName());

    public static ArrayList<ArrayList<MapRoomNode>> generateDungeon(int height, int width, int pathDensity, Random rng) {
        ArrayList<ArrayList<MapRoomNode>> map = MapGenerator.createNodes(height, width);
        map = ModHelper.isModEnabled("Uncertain Future") ? MapGenerator.createPaths(map, 1, rng) : MapGenerator.createPaths(map, pathDensity, rng);
        map = MapGenerator.filterRedundantEdgesFromRow(map);
        return map;
    }

    private static ArrayList<ArrayList<MapRoomNode>> filterRedundantEdgesFromRow(ArrayList<ArrayList<MapRoomNode>> map) {
        ArrayList<MapEdge> existingEdges = new ArrayList<MapEdge>();
        ArrayList<MapEdge> deleteList = new ArrayList<MapEdge>();
        for (MapRoomNode node : map.get(0)) {
            if (!node.hasEdges()) continue;
            for (MapEdge edge : node.getEdges()) {
                for (MapEdge prevEdge : existingEdges) {
                    if (edge.dstX != prevEdge.dstX || edge.dstY != prevEdge.dstY) continue;
                    deleteList.add(edge);
                }
                existingEdges.add(edge);
            }
            for (MapEdge edge : deleteList) {
                node.delEdge(edge);
            }
            deleteList.clear();
        }
        return map;
    }

    private static ArrayList<ArrayList<MapRoomNode>> createNodes(int height, int width) {
        ArrayList<ArrayList<MapRoomNode>> nodes = new ArrayList<ArrayList<MapRoomNode>>();
        for (int y = 0; y < height; ++y) {
            ArrayList<MapRoomNode> row = new ArrayList<MapRoomNode>();
            for (int x = 0; x < width; ++x) {
                row.add(new MapRoomNode(x, y));
            }
            nodes.add(row);
        }
        return nodes;
    }

    private static ArrayList<ArrayList<MapRoomNode>> createPaths(ArrayList<ArrayList<MapRoomNode>> nodes, int pathDensity, Random rng) {
        int first_row = 0;
        int row_size = nodes.get(first_row).size() - 1;
        int firstStartingNode = -1;
        for (int i = 0; i < pathDensity; ++i) {
            int startingNode = MapGenerator.randRange(rng, 0, row_size);
            if (i == 0) {
                firstStartingNode = startingNode;
            }
            while (startingNode == firstStartingNode && i == 1) {
                startingNode = MapGenerator.randRange(rng, 0, row_size);
            }
            MapGenerator._createPaths(nodes, new MapEdge(startingNode, -1, startingNode, 0), rng);
        }
        return nodes;
    }

    private static MapEdge getMaxEdge(ArrayList<MapEdge> edges) {
        Collections.sort(edges, new EdgeComparator());
        assert (!edges.isEmpty()) : "Somehow the edges are empty. This shouldn't happen.";
        return edges.get(edges.size() - 1);
    }

    private static MapEdge getMinEdge(ArrayList<MapEdge> edges) {
        Collections.sort(edges, new EdgeComparator());
        assert (!edges.isEmpty()) : "Somehow the edges are empty. This shouldn't happen.";
        return edges.get(0);
    }

    private static MapRoomNode getNodeWithMaxX(ArrayList<MapRoomNode> nodes) {
        assert (!nodes.isEmpty()) : "The nodes are empty, this shouldn't happen.";
        MapRoomNode max = nodes.get(0);
        for (MapRoomNode node : nodes) {
            if (node.x <= max.x) continue;
            max = node;
        }
        return max;
    }

    private static MapRoomNode getNodeWithMinX(ArrayList<MapRoomNode> nodes) {
        assert (!nodes.isEmpty()) : "The nodes are empty, this shouldn't happen.";
        MapRoomNode min = nodes.get(0);
        for (MapRoomNode node : nodes) {
            if (node.x >= min.x) continue;
            min = node;
        }
        return min;
    }

    private static MapRoomNode getCommonAncestor(MapRoomNode node1, MapRoomNode node2, int max_depth) {
        MapRoomNode r_node;
        MapRoomNode l_node;
        assert (node1.y == node2.y);
        assert (node1 != node2);
        if (node1.x < node2.y) {
            l_node = node1;
            r_node = node2;
        } else {
            l_node = node2;
            r_node = node1;
        }
        for (int current_y = node1.y; current_y >= 0 && current_y >= node1.y - max_depth; --current_y) {
            if (l_node.getParents().isEmpty() || r_node.getParents().isEmpty()) {
                return null;
            }
            if ((l_node = MapGenerator.getNodeWithMaxX(l_node.getParents())) != (r_node = MapGenerator.getNodeWithMinX(r_node.getParents()))) continue;
            return l_node;
        }
        return null;
    }

    private static ArrayList<ArrayList<MapRoomNode>> _createPaths(ArrayList<ArrayList<MapRoomNode>> nodes, MapEdge edge, Random rng) {
        MapRoomNode right_node;
        MapRoomNode left_node;
        int max;
        int min;
        MapRoomNode currentNode = MapGenerator.getNode(edge.dstX, edge.dstY, nodes);
        if (edge.dstY + 1 >= nodes.size()) {
            MapEdge newEdge = new MapEdge(edge.dstX, edge.dstY, currentNode.offsetX, currentNode.offsetY, 3, edge.dstY + 2, 0.0f, 0.0f, true);
            currentNode.addEdge(newEdge);
            Collections.sort(currentNode.getEdges(), new EdgeComparator());
            return nodes;
        }
        int row_width = nodes.get(edge.dstY).size();
        int row_end_node = row_width - 1;
        if (edge.dstX == 0) {
            min = 0;
            max = 1;
        } else if (edge.dstX == row_end_node) {
            min = -1;
            max = 0;
        } else {
            min = -1;
            max = 1;
        }
        int newEdgeX = edge.dstX + MapGenerator.randRange(rng, min, max);
        int newEdgeY = edge.dstY + 1;
        MapRoomNode targetNodeCandidate = MapGenerator.getNode(newEdgeX, newEdgeY, nodes);
        int min_ancestor_gap = 3;
        int max_ancestor_gap = 5;
        ArrayList<MapRoomNode> parents = targetNodeCandidate.getParents();
        if (!parents.isEmpty()) {
            for (MapRoomNode parent : parents) {
                MapRoomNode ancestor;
                if (parent == currentNode || (ancestor = MapGenerator.getCommonAncestor(parent, currentNode, max_ancestor_gap)) == null) continue;
                int ancestor_gap = newEdgeY - ancestor.y;
                if (ancestor_gap < min_ancestor_gap) {
                    if (targetNodeCandidate.x > currentNode.x) {
                        newEdgeX = edge.dstX + MapGenerator.randRange(rng, -1, 0);
                        if (newEdgeX < 0) {
                            newEdgeX = edge.dstX;
                        }
                    } else if (targetNodeCandidate.x == currentNode.x) {
                        newEdgeX = edge.dstX + MapGenerator.randRange(rng, -1, 1);
                        if (newEdgeX > row_end_node) {
                            newEdgeX = edge.dstX - 1;
                        } else if (newEdgeX < 0) {
                            newEdgeX = edge.dstX + 1;
                        }
                    } else {
                        newEdgeX = edge.dstX + MapGenerator.randRange(rng, 0, 1);
                        if (newEdgeX > row_end_node) {
                            newEdgeX = edge.dstX;
                        }
                    }
                    targetNodeCandidate = MapGenerator.getNode(newEdgeX, newEdgeY, nodes);
                    continue;
                }
                if (ancestor_gap < max_ancestor_gap) continue;
            }
        }
        if (edge.dstX != 0 && (left_node = nodes.get(edge.dstY).get(edge.dstX - 1)).hasEdges()) {
            MapEdge right_edge_of_left_node = MapGenerator.getMaxEdge(left_node.getEdges());
            if (right_edge_of_left_node.dstX > newEdgeX) {
                newEdgeX = right_edge_of_left_node.dstX;
            }
        }
        if (edge.dstX < row_end_node && (right_node = nodes.get(edge.dstY).get(edge.dstX + 1)).hasEdges()) {
            MapEdge left_edge_of_right_node = MapGenerator.getMinEdge(right_node.getEdges());
            if (left_edge_of_right_node.dstX < newEdgeX) {
                newEdgeX = left_edge_of_right_node.dstX;
            }
        }
        targetNodeCandidate = MapGenerator.getNode(newEdgeX, newEdgeY, nodes);
        MapEdge newEdge = new MapEdge(edge.dstX, edge.dstY, currentNode.offsetX, currentNode.offsetY, newEdgeX, newEdgeY, targetNodeCandidate.offsetX, targetNodeCandidate.offsetY, false);
        currentNode.addEdge(newEdge);
        Collections.sort(currentNode.getEdges(), new EdgeComparator());
        targetNodeCandidate.addParent(currentNode);
        return MapGenerator._createPaths(nodes, newEdge, rng);
    }

    private static MapRoomNode getNode(int x, int y, ArrayList<ArrayList<MapRoomNode>> nodes) {
        return nodes.get(y).get(x);
    }

    private static String paddingGenerator(int length) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            str.append(" ");
        }
        return str.toString();
    }

    public static String toString(ArrayList<ArrayList<MapRoomNode>> nodes) {
        return MapGenerator.toString(nodes, false);
    }

    public static String toString(ArrayList<ArrayList<MapRoomNode>> nodes, Boolean showRoomSymbols) {
        StringBuilder str = new StringBuilder();
        int left_padding_size = 5;
        for (int row_num = nodes.size() - 1; row_num >= 0; --row_num) {
            str.append("\n ").append(MapGenerator.paddingGenerator(left_padding_size));
            for (MapRoomNode node : nodes.get(row_num)) {
                String right = " ";
                String mid = " ";
                String left = " ";
                for (MapEdge edge : node.getEdges()) {
                    if (edge.dstX < node.x) {
                        left = "\\";
                    }
                    if (edge.dstX == node.x) {
                        mid = "|";
                    }
                    if (edge.dstX <= node.x) continue;
                    right = "/";
                }
                str.append(left).append(mid).append(right);
            }
            str.append("\n").append(row_num).append(" ");
            str.append(MapGenerator.paddingGenerator(left_padding_size - String.valueOf(row_num).length()));
            for (MapRoomNode node : nodes.get(row_num)) {
                String node_symbol = " ";
                if (row_num == nodes.size() - 1) {
                    for (MapRoomNode lower_node : nodes.get(row_num - 1)) {
                        for (MapEdge edge : lower_node.getEdges()) {
                            if (edge.dstX != node.x) continue;
                            node_symbol = node.getRoomSymbol(showRoomSymbols);
                        }
                    }
                } else if (node.hasEdges()) {
                    node_symbol = node.getRoomSymbol(showRoomSymbols);
                }
                str.append(" ").append(node_symbol).append(" ");
            }
        }
        return str.toString();
    }

    private static int randRange(Random rng, int min, int max) {
        if (rng == null) {
            logger.info("RNG WAS NULL, REPORT IMMEDIATELY");
            rng = new Random(1L);
        }
        return rng.random(max - min) + min;
    }
}

