/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tiledmappacker;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.tiledmappacker.TileSetLayout;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TiledMapPacker {
    private TexturePacker packer;
    private TiledMap map;
    private TmxMapLoader mapLoader = new TmxMapLoader(new AbsoluteFileHandleResolver());
    private TiledMapPackerSettings settings;
    private static final String TilesetsOutputDir = "tileset";
    static String AtlasOutputName = "packed";
    private HashMap<String, IntArray> tilesetUsedIds = new HashMap();
    private ObjectMap<String, TiledMapTileSet> tilesetsToPack;
    static File inputDir;
    static File outputDir;
    private FileHandle currentDir;

    public TiledMapPacker() {
        this(new TiledMapPackerSettings());
    }

    public TiledMapPacker(TiledMapPackerSettings settings) {
        this.settings = settings;
    }

    public void processInputDir(TexturePacker.Settings texturePackerSettings) throws IOException {
        FileHandle inputDirHandle = new FileHandle(inputDir.getCanonicalPath());
        File[] mapFilesInCurrentDir = inputDir.listFiles(new TmxFilter());
        this.tilesetsToPack = new ObjectMap();
        for (File mapFile : mapFilesInCurrentDir) {
            this.processSingleMap(mapFile, inputDirHandle, texturePackerSettings);
        }
        this.processSubdirectories(inputDirHandle, texturePackerSettings);
        boolean combineTilesets = this.settings.combineTilesets;
        if (combineTilesets) {
            this.packTilesets(inputDirHandle, texturePackerSettings);
        }
    }

    private void processSubdirectories(FileHandle currentDir, TexturePacker.Settings texturePackerSettings) throws IOException {
        File[] directories;
        File parentPath = new File(currentDir.path());
        for (File directory : directories = parentPath.listFiles(new DirFilter())) {
            File[] mapFilesInCurrentDir;
            currentDir = new FileHandle(directory.getCanonicalPath());
            for (File mapFile : mapFilesInCurrentDir = directory.listFiles(new TmxFilter())) {
                this.processSingleMap(mapFile, currentDir, texturePackerSettings);
            }
            this.processSubdirectories(currentDir, texturePackerSettings);
        }
    }

    private void processSingleMap(File mapFile, FileHandle dirHandle, TexturePacker.Settings texturePackerSettings) throws IOException {
        boolean combineTilesets = this.settings.combineTilesets;
        if (!combineTilesets) {
            this.tilesetUsedIds = new HashMap();
            this.tilesetsToPack = new ObjectMap();
        }
        this.map = this.mapLoader.load(mapFile.getCanonicalPath());
        boolean stripUnusedTiles = this.settings.stripUnusedTiles;
        if (stripUnusedTiles) {
            this.stripUnusedTiles();
        } else {
            for (TiledMapTileSet tileset : this.map.getTileSets()) {
                String tilesetName = tileset.getName();
                if (this.tilesetsToPack.containsKey(tilesetName)) continue;
                this.tilesetsToPack.put(tilesetName, tileset);
            }
        }
        if (!combineTilesets) {
            FileHandle tmpHandle = new FileHandle(mapFile.getName());
            this.settings.atlasOutputName = tmpHandle.nameWithoutExtension();
            this.packTilesets(dirHandle, texturePackerSettings);
        }
        FileHandle tmxFile = new FileHandle(mapFile.getCanonicalPath());
        this.writeUpdatedTMX(this.map, tmxFile);
    }

    private void stripUnusedTiles() {
        int mapWidth = this.map.getProperties().get("width", Integer.class);
        int mapHeight = this.map.getProperties().get("height", Integer.class);
        int numlayers = this.map.getLayers().getCount();
        int bucketSize = mapWidth * mapHeight * numlayers;
        for (MapLayer layer : this.map.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer)) continue;
            TiledMapTileLayer tlayer = (TiledMapTileLayer)layer;
            for (int y = 0; y < mapHeight; ++y) {
                for (int x = 0; x < mapWidth; ++x) {
                    if (tlayer.getCell(x, y) == null) continue;
                    TiledMapTile tile = tlayer.getCell(x, y).getTile();
                    if (tile instanceof AnimatedTiledMapTile) {
                        AnimatedTiledMapTile aTile = (AnimatedTiledMapTile)tile;
                        for (StaticTiledMapTile t : aTile.getFrameTiles()) {
                            this.addTile(t, bucketSize);
                        }
                    }
                    this.addTile(tile, bucketSize);
                }
            }
        }
    }

    private void addTile(TiledMapTile tile, int bucketSize) {
        int tileid = tile.getId() & 0x1FFFFFFF;
        String tilesetName = this.tilesetNameFromTileId(this.map, tileid);
        IntArray usedIds = this.getUsedIdsBucket(tilesetName, bucketSize);
        usedIds.add(tileid);
        if (!this.tilesetsToPack.containsKey(tilesetName)) {
            this.tilesetsToPack.put(tilesetName, this.map.getTileSets().getTileSet(tilesetName));
        }
    }

    private String tilesetNameFromTileId(TiledMap map, int tileid) {
        String name = "";
        if (tileid == 0) {
            return "";
        }
        for (TiledMapTileSet tileset : map.getTileSets()) {
            int firstgid = tileset.getProperties().get("firstgid", -1, Integer.class);
            if (firstgid == -1) continue;
            if (tileid >= firstgid) {
                name = tileset.getName();
                continue;
            }
            return name;
        }
        return name;
    }

    private IntArray getUsedIdsBucket(String tilesetName, int size) {
        if (this.tilesetUsedIds.containsKey(tilesetName)) {
            return this.tilesetUsedIds.get(tilesetName);
        }
        if (size <= 0) {
            return null;
        }
        IntArray bucket = new IntArray(size);
        this.tilesetUsedIds.put(tilesetName, bucket);
        return bucket;
    }

    private void packTilesets(FileHandle inputDirHandle, TexturePacker.Settings texturePackerSettings) throws IOException {
        this.packer = new TexturePacker(texturePackerSettings);
        for (TiledMapTileSet set : this.tilesetsToPack.values()) {
            String tilesetName = set.getName();
            System.out.println("Processing tileset " + tilesetName);
            IntArray usedIds = this.settings.stripUnusedTiles ? this.getUsedIdsBucket(tilesetName, -1) : null;
            int tileWidth = set.getProperties().get("tilewidth", Integer.class);
            int tileHeight = set.getProperties().get("tileheight", Integer.class);
            int firstgid = set.getProperties().get("firstgid", Integer.class);
            String imageName = set.getProperties().get("imagesource", String.class);
            TileSetLayout layout = new TileSetLayout(firstgid, set, inputDirHandle);
            int gid = layout.firstgid;
            for (int i = 0; i < layout.numTiles; ++i) {
                boolean verbose = this.settings.verbose;
                if (usedIds != null && !usedIds.contains(gid)) {
                    if (verbose) {
                        System.out.println("Stripped id #" + gid + " from tileset \"" + tilesetName + "\"");
                    }
                } else {
                    Vector2 tileLocation = layout.getLocation(gid);
                    BufferedImage tile = new BufferedImage(tileWidth, tileHeight, 6);
                    Graphics2D g = tile.createGraphics();
                    g.drawImage(layout.image, 0, 0, tileWidth, tileHeight, (int)tileLocation.x, (int)tileLocation.y, (int)tileLocation.x + tileWidth, (int)tileLocation.y + tileHeight, null);
                    if (verbose) {
                        System.out.println("Adding " + tileWidth + "x" + tileHeight + " (" + (int)tileLocation.x + ", " + (int)tileLocation.y + ")");
                    }
                    int adjustedGid = gid - layout.firstgid;
                    String separator = "_";
                    String regionName = tilesetName + "_" + adjustedGid;
                    this.packer.addImage(tile, regionName);
                }
                ++gid;
            }
        }
        String tilesetOutputDir = outputDir.toString() + "/" + this.settings.tilesetOutputDirectory;
        File relativeTilesetOutputDir = new File(tilesetOutputDir);
        File outputDirTilesets = new File(relativeTilesetOutputDir.getCanonicalPath());
        outputDirTilesets.mkdirs();
        this.packer.pack(outputDirTilesets, this.settings.atlasOutputName + ".atlas");
    }

    private void writeUpdatedTMX(TiledMap tiledMap, FileHandle tmxFileHandle) throws IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(tmxFileHandle.read());
            Node map = doc.getFirstChild();
            while (map.getNodeType() != 1 || map.getNodeName() != "map") {
                if ((map = map.getNextSibling()) != null) continue;
                throw new GdxRuntimeException("Couldn't find map node!");
            }
            TiledMapPacker.setProperty(doc, map, "atlas", this.settings.tilesetOutputDirectory + "/" + this.settings.atlasOutputName + ".atlas");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            outputDir.mkdirs();
            StreamResult result = new StreamResult(new File(outputDir, tmxFileHandle.name()));
            transformer.transform(source, result);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException("ParserConfigurationException: " + e.getMessage());
        }
        catch (SAXException e) {
            throw new RuntimeException("SAXException: " + e.getMessage());
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException("TransformerConfigurationException: " + e.getMessage());
        }
        catch (TransformerException e) {
            throw new RuntimeException("TransformerException: " + e.getMessage());
        }
    }

    private static void setProperty(Document doc, Node parent, String name, String value) {
        Node properties = TiledMapPacker.getFirstChildNodeByName(parent, "properties");
        Node property = TiledMapPacker.getFirstChildByNameAttrValue(properties, "property", "name", name);
        NamedNodeMap attributes = property.getAttributes();
        Node valueNode = attributes.getNamedItem("value");
        if (valueNode == null) {
            valueNode = doc.createAttribute("value");
            valueNode.setNodeValue(value);
            attributes.setNamedItem(valueNode);
        } else {
            valueNode.setNodeValue(value);
        }
    }

    private static Node getFirstChildNodeByName(Node parent, String child) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            if (!childNodes.item(i).getNodeName().equals(child)) continue;
            return childNodes.item(i);
        }
        Element newNode = parent.getOwnerDocument().createElement(child);
        if (childNodes.item(0) != null) {
            return parent.insertBefore(newNode, childNodes.item(0));
        }
        return parent.appendChild(newNode);
    }

    private static Node getFirstChildByNameAttrValue(Node node, String childName, String attr, String value) {
        NamedNodeMap attributes;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node attribute;
            if (!childNodes.item(i).getNodeName().equals(childName) || !(attribute = (attributes = childNodes.item(i).getAttributes()).getNamedItem(attr)).getNodeValue().equals(value)) continue;
            return childNodes.item(i);
        }
        Element newNode = node.getOwnerDocument().createElement(childName);
        attributes = newNode.getAttributes();
        Attr nodeAttr = node.getOwnerDocument().createAttribute(attr);
        nodeAttr.setNodeValue(value);
        attributes.setNamedItem(nodeAttr);
        if (childNodes.item(0) != null) {
            return node.insertBefore(newNode, childNodes.item(0));
        }
        return node.appendChild(newNode);
    }

    public static void main(String[] args) {
        final TexturePacker.Settings texturePackerSettings = new TexturePacker.Settings();
        texturePackerSettings.paddingX = 2;
        texturePackerSettings.paddingY = 2;
        texturePackerSettings.edgePadding = true;
        texturePackerSettings.duplicatePadding = true;
        texturePackerSettings.bleed = true;
        texturePackerSettings.alias = true;
        texturePackerSettings.useIndexes = true;
        final TiledMapPackerSettings packerSettings = new TiledMapPackerSettings();
        if (args.length == 0) {
            TiledMapPacker.printUsage();
            System.exit(0);
        } else if (args.length == 1) {
            inputDir = new File(args[0]);
            outputDir = new File(inputDir, "../output/");
        } else if (args.length == 2) {
            inputDir = new File(args[0]);
            outputDir = new File(args[1]);
        } else {
            inputDir = new File(args[0]);
            outputDir = new File(args[1]);
            TiledMapPacker.processExtraArgs(args, packerSettings);
        }
        TiledMapPacker packer = new TiledMapPacker(packerSettings);
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.forceExit = false;
        config.width = 100;
        config.height = 50;
        config.title = "TiledMapPacker";
        new LwjglApplication(new ApplicationListener(){

            @Override
            public void resume() {
            }

            @Override
            public void resize(int width, int height) {
            }

            @Override
            public void render() {
            }

            @Override
            public void pause() {
            }

            @Override
            public void dispose() {
            }

            @Override
            public void create() {
                TiledMapPacker packer = new TiledMapPacker(packerSettings);
                if (!inputDir.exists()) {
                    System.out.println(inputDir.getAbsolutePath());
                    throw new RuntimeException("Input directory does not exist: " + inputDir);
                }
                try {
                    packer.processInputDir(texturePackerSettings);
                }
                catch (IOException e) {
                    throw new RuntimeException("Error processing map: " + e.getMessage());
                }
                System.out.println("Finished processing.");
                Gdx.app.exit();
            }
        }, config);
    }

    private static void processExtraArgs(String[] args, TiledMapPackerSettings packerSettings) {
        String stripUnused = "--strip-unused";
        String combineTilesets = "--combine-tilesets";
        String verbose = "-v";
        int length = args.length - 2;
        String[] argsNotDir = new String[length];
        System.arraycopy(args, 2, argsNotDir, 0, length);
        for (String string : argsNotDir) {
            if (stripUnused.equals(string)) {
                packerSettings.stripUnusedTiles = true;
                continue;
            }
            if (combineTilesets.equals(string)) {
                packerSettings.combineTilesets = true;
                continue;
            }
            if (verbose.equals(string)) {
                packerSettings.verbose = true;
                continue;
            }
            System.out.println("\nOption \"" + string + "\" not recognized.\n");
            TiledMapPacker.printUsage();
            System.exit(0);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: INPUTDIR [OUTPUTDIR] [--strip-unused] [--combine-tilesets] [-v]");
        System.out.println("Processes a directory of Tiled .tmx maps. Unable to process maps with XML");
        System.out.println("tile layer format.");
        System.out.println("  --strip-unused             omits all tiles that are not used. Speeds up");
        System.out.println("                             the processing. Smaller tilesets.");
        System.out.println("  --combine-tilesets         instead of creating a tileset for each map,");
        System.out.println("                             this combines the tilesets into some kind");
        System.out.println("                             of monster tileset. Has problems with tileset");
        System.out.println("                             location. Has problems with nested folders.");
        System.out.println("                             Not recommended.");
        System.out.println("  -v                         outputs which tiles are stripped and included");
        System.out.println();
    }

    public static class TiledMapPackerSettings {
        public boolean stripUnusedTiles = false;
        public boolean combineTilesets = false;
        public boolean verbose = false;
        public String tilesetOutputDirectory = "tileset";
        public String atlasOutputName = AtlasOutputName;
    }

    private static class DirFilter
    implements FilenameFilter {
        @Override
        public boolean accept(File f, String s) {
            return new File(f, s).isDirectory();
        }
    }

    private static class TmxFilter
    implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".tmx");
        }
    }
}

