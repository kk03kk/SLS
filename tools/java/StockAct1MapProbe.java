import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapGenerator;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.map.RoomTypeAssigner;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sun.misc.Unsafe;

/** Headless stock-bytecode map path probe; does not start the game UI. */
public final class StockAct1MapProbe {
    private static void prepareRoomLocalization() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        CardCrawlGame.languagePack = (LocalizedStrings) unsafe.allocateInstance(LocalizedStrings.class);
        UIStrings placeholder = new UIStrings();
        placeholder.TEXT = new String[40];
        Map<String, UIStrings> ui = new HashMap<String, UIStrings>() {
            @Override public UIStrings get(Object key) { return placeholder; }
        };
        Field uiField = LocalizedStrings.class.getDeclaredField("ui");
        uiField.setAccessible(true);
        uiField.set(null, ui);
    }

    public static void main(String[] args) throws Exception {
        long seed = Long.parseLong(args[0]);
        Random rng = new Random(seed + 1L);
        ArrayList<ArrayList<MapRoomNode>> map = MapGenerator.generateDungeon(15, 7, 6, rng);
        if (args.length > 1 && args[1].equals("rooms")) {
            prepareRoomLocalization();
            int count = 0;
            for (ArrayList<MapRoomNode> row : map) {
                for (MapRoomNode node : row) {
                    if (node.hasEdges() && node.y != 13) count++;
                }
            }
            ArrayList<AbstractRoom> roomList = new ArrayList<>();
            for (int i = 0; i < Math.round(count * 0.05f); i++) roomList.add(new ShopRoom());
            for (int i = 0; i < Math.round(count * 0.12f); i++) roomList.add(new RestRoom());
            for (int i = 0; i < Math.round(count * 0.08f * 1.6f); i++) roomList.add(new MonsterRoomElite());
            for (int i = 0; i < Math.round(count * 0.22f); i++) roomList.add(new EventRoom());
            RoomTypeAssigner.assignRowAsRoomType(map.get(14), RestRoom.class);
            RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);
            RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class);
            map = RoomTypeAssigner.distributeRoomsAcrossMap(rng, map, roomList);
        }
        for (ArrayList<MapRoomNode> row : map) {
            for (MapRoomNode node : row) {
                if (!node.hasEdges()) continue;
                StringBuilder edges = new StringBuilder();
                for (MapEdge edge : node.getEdges()) {
                    if (edges.length() > 0) edges.append(',');
                    edges.append(edge.dstX).append(':').append(edge.dstY);
                }
                String room = node.room == null ? "" : "|" + node.room.getClass().getSimpleName();
                System.out.println(node.x + ":" + node.y + room + ">" + edges);
            }
        }
    }
}
