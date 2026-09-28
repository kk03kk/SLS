import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.random.Random;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import sun.misc.Unsafe;

/** Execute stock bytecode only; Unsafe supplies inert containers, not game rules. */
public final class StockAct1MechanicsProbe {
    private static final class DisplayMode extends com.badlogic.gdx.Graphics.DisplayMode {
        DisplayMode() { super(1280, 720, 60, 32); }
    }
    public static final class SilentSound extends com.megacrit.cardcrawl.audio.SoundMaster {
        @Override public long play(String key) { return 0L; }
        @Override public long play(String key, float pitch) { return 0L; }
        @Override public long play(String key, boolean immediate) { return 0L; }
    }
    private static Unsafe unsafe;
    private static final Gson gson = new Gson();

    private static Map<String, Object> state(Random rng) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counter", rng.counter);
        out.put("seed0", Long.toUnsignedString(rng.random.getState(0)));
        out.put("seed1", Long.toUnsignedString(rng.random.getState(1)));
        return out;
    }

    private static void prepare() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe)field.get(null);
        CardCrawlGame.sound = (SilentSound)unsafe.allocateInstance(SilentSound.class);
        for (Class<?> owner : new Class<?>[]{com.megacrit.cardcrawl.core.Settings.class,
                com.megacrit.cardcrawl.unlock.UnlockTracker.class}) {
            for (Field prefs : owner.getDeclaredFields()) {
                if (prefs.getType() == com.megacrit.cardcrawl.helpers.Prefs.class) {
                    prefs.setAccessible(true); prefs.set(null, new com.megacrit.cardcrawl.helpers.Prefs());
                }
            }
        }
        Gdx.graphics = (com.badlogic.gdx.Graphics)java.lang.reflect.Proxy.newProxyInstance(
            com.badlogic.gdx.Graphics.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Graphics.class},
            (proxy, method, args) -> {
                if (method.getReturnType() == com.badlogic.gdx.Graphics.DisplayMode.class) return new DisplayMode();
                if (method.getReturnType() == com.badlogic.gdx.Graphics.DisplayMode[].class) return new DisplayMode[]{new DisplayMode()};
                if (method.getReturnType() == int.class) return 1280;
                if (method.getReturnType() == float.class) return 0.0f;
                if (method.getReturnType() == long.class) return 0L;
                if (method.getReturnType() == boolean.class) return false;
                return null;
            });
        // Presentation assets are deliberately unavailable; do not load GL or
        // native rendering libraries while initializing stock static fields.
        Gdx.files = (com.badlogic.gdx.Files)java.lang.reflect.Proxy.newProxyInstance(
            com.badlogic.gdx.Files.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Files.class},
            (proxy, method, args) -> {
                if (method.getReturnType() == FileHandle.class) return new FileHandle("headless-missing-assets");
                if (method.getReturnType() == String.class) return "";
                return false;
            });
        CardCrawlGame.languagePack = (LocalizedStrings)unsafe.allocateInstance(LocalizedStrings.class);
        for (Field map : LocalizedStrings.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(map.getType()) && java.lang.reflect.Modifier.isStatic(map.getModifiers())) {
                map.setAccessible(true); map.set(null, new HashMap<>());
            }
        }
        UIStrings ui = new UIStrings(); ui.TEXT = new String[256]; ui.EXTRA_TEXT = new String[256];
        java.util.Arrays.fill(ui.TEXT, ""); java.util.Arrays.fill(ui.EXTRA_TEXT, "");
        Field uiField = LocalizedStrings.class.getDeclaredField("ui"); uiField.setAccessible(true);
        uiField.set(null, new HashMap<String, UIStrings>() {
            @Override public UIStrings get(Object key) { return ui; }
        });
        MonsterStrings monster = new MonsterStrings();
        monster.NAME = "probe"; monster.MOVES = new String[10]; monster.DIALOG = new String[10];
        Field monsterField = LocalizedStrings.class.getDeclaredField("monsters"); monsterField.setAccessible(true);
        monsterField.set(null, new HashMap<String, MonsterStrings>() {
            @Override public MonsterStrings get(Object key) { return monster; }
        });
        var characters = new com.megacrit.cardcrawl.localization.CharacterStrings();
        characters.NAMES = new String[256]; characters.TEXT = new String[256]; characters.OPTIONS = new String[256];
        java.util.Arrays.fill(characters.NAMES, ""); java.util.Arrays.fill(characters.TEXT, "");
        java.util.Arrays.fill(characters.OPTIONS, "");
        Field characterField = LocalizedStrings.class.getDeclaredField("characters"); characterField.setAccessible(true);
        characterField.set(null, new HashMap<String, com.megacrit.cardcrawl.localization.CharacterStrings>() {
            @Override public com.megacrit.cardcrawl.localization.CharacterStrings get(Object key) { return characters; }
        });
        com.badlogic.gdx.graphics.g2d.BitmapFont font =
            (com.badlogic.gdx.graphics.g2d.BitmapFont)unsafe.allocateInstance(com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        var data = new com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData();
        data.missingGlyph = new com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph();
        data.missingGlyph.xadvance = 1;
        Field fontData = font.getClass().getDeclaredField("data"); fontData.setAccessible(true); fontData.set(font, data);
        Field regions = font.getClass().getDeclaredField("regions"); regions.setAccessible(true);
        var regionList = new com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion>();
        regionList.add(new com.badlogic.gdx.graphics.g2d.TextureRegion()); regions.set(font, regionList);
        Field cache = font.getClass().getDeclaredField("cache"); cache.setAccessible(true);
        cache.set(font, new com.badlogic.gdx.graphics.g2d.BitmapFontCache(font));
        for (Field f : FontHelper.class.getDeclaredFields()) {
            if (f.getType() == font.getClass() && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true); f.set(null, font);
            }
        }
        var texture = (com.badlogic.gdx.graphics.Texture)unsafe.allocateInstance(com.badlogic.gdx.graphics.Texture.class);
        Field textureData = texture.getClass().getDeclaredField("data"); textureData.setAccessible(true);
        textureData.set(texture, java.lang.reflect.Proxy.newProxyInstance(
            com.badlogic.gdx.graphics.TextureData.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.graphics.TextureData.class},
            (proxy, method, args) -> method.getReturnType() == int.class ? 1 :
                method.getReturnType() == boolean.class ? false : null));
        for (Field f : com.megacrit.cardcrawl.helpers.ImageMaster.class.getDeclaredFields()) {
            if (f.getType() == texture.getClass() && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true); f.set(null, texture);
            }
        }
    }

    public static final class ProbeCard extends AbstractCard {
        private ProbeCard() { super("", "", "", 0, "", CardType.SKILL, CardColor.RED, CardRarity.COMMON, CardTarget.NONE); }
        public void upgrade() {}
        public void use(AbstractPlayer p, AbstractMonster m) {}
        public AbstractCard makeCopy() { return this; }
    }

    private static ProbeCard card(String id, String kind) throws Exception {
        ProbeCard card = (ProbeCard)unsafe.allocateInstance(ProbeCard.class);
        card.cardID = id;
        card.color = kind.equals("colored") ? AbstractCard.CardColor.RED :
                     kind.equals("curse") ? AbstractCard.CardColor.CURSE : AbstractCard.CardColor.COLORLESS;
        return card;
    }

    private static Map<String, Object> execute(JsonObject row) throws Exception {
        long seed = Long.parseUnsignedLong(row.get("seed").getAsString());
        Random rng = new Random(seed);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("initial", state(rng));
        String mode = row.get("mode").getAsString();
        if (mode.equals("rng")) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("range_999", rng.random(999));
            values.put("between_5_12", rng.random(5, 12));
            values.put("long_range", rng.random(1000000000000L));
            values.put("random_long", rng.randomLong());
            values.put("boolean", rng.randomBoolean());
            values.put("chance_0_375", rng.randomBoolean(0.375f));
            values.put("unit_float", rng.random());
            values.put("float_range", rng.random(5.0f));
            values.put("float_between", rng.random(-2.0f, 3.0f));
            result.put("values", values);
            ArrayList<Integer> shuffled = new ArrayList<>();
            for (int i=0; i<10; i++) shuffled.add(i);
            java.util.Collections.shuffle(shuffled, new java.util.Random(seed));
            result.put("shuffle", shuffled);
        } else if (mode.equals("transform")) {
            String kind = row.get("kind").getAsString();
            CardGroup group = (CardGroup)unsafe.allocateInstance(CardGroup.class);
            group.group = new ArrayList<>();
            LinkedHashMap<String, AbstractCard> cursePool = new LinkedHashMap<>();
            for (var item : row.getAsJsonArray("pool")) {
                ProbeCard c = card(item.getAsString(), kind); group.group.add(c); cursePool.put(c.cardID, c);
            }
            ProbeCard excluded = card(row.get("exclude").getAsString(), kind);
            AbstractCard selected;
            if (kind.equals("curse")) {
                // Deliberately preserve the supplied pool order. This isolates
                // stock filtering/drawing, not CardLibrary's real pool order.
                Field curses = CardLibrary.class.getDeclaredField("curses"); curses.setAccessible(true);
                curses.set(null, cursePool); CardLibrary.cards = cursePool;
                selected = CardLibrary.getCurse(excluded, rng);
            } else if (kind.equals("colorless")) {
                AbstractDungeon.srcColorlessCardPool = group;
                selected = AbstractDungeon.returnTrulyRandomColorlessCardFromAvailable(excluded, rng);
            } else {
                AbstractDungeon.commonCardPool = group;
                CardGroup empty = (CardGroup)unsafe.allocateInstance(CardGroup.class); empty.group = new ArrayList<>();
                AbstractDungeon.srcUncommonCardPool = empty; AbstractDungeon.srcRareCardPool = empty;
                selected = AbstractDungeon.returnTrulyRandomCardFromAvailable(excluded, rng);
            }
            result.put("selected", selected.cardID);
        } else if (mode.equals("block")) {
            var group = (com.megacrit.cardcrawl.monsters.MonsterGroup)unsafe.allocateInstance(com.megacrit.cardcrawl.monsters.MonsterGroup.class);
            group.monsters = new ArrayList<>();
            for (var alive : row.getAsJsonArray("alive")) {
                var m = (AbstractMonster)unsafe.allocateInstance(com.megacrit.cardcrawl.monsters.exordium.GremlinThief.class);
                m.isDying = !alive.getAsBoolean(); m.powers = new ArrayList<>();
                m.hb = new com.megacrit.cardcrawl.helpers.Hitbox(10.0f, 10.0f);
                m.intent = AbstractMonster.Intent.ATTACK;
                for (Field color : com.megacrit.cardcrawl.core.AbstractCreature.class.getDeclaredFields()) {
                    if (color.getType() == com.badlogic.gdx.graphics.Color.class && !java.lang.reflect.Modifier.isStatic(color.getModifiers())) {
                        color.setAccessible(true); color.set(m, com.badlogic.gdx.graphics.Color.WHITE.cpy());
                    }
                }
                group.monsters.add(m);
            }
            var room = (com.megacrit.cardcrawl.rooms.MonsterRoom)unsafe.allocateInstance(com.megacrit.cardcrawl.rooms.MonsterRoom.class);
            room.monsters = group;
            AbstractDungeon.currMapNode = new com.megacrit.cardcrawl.map.MapRoomNode(0, 0);
            AbstractDungeon.currMapNode.room = room; AbstractDungeon.aiRng = rng;
            new com.megacrit.cardcrawl.actions.unique.GainBlockRandomMonsterAction(group.monsters.get(0), 11).update();
            ArrayList<Integer> blocks = new ArrayList<>();
            for (var m : group.monsters) blocks.add(m.currentBlock);
            result.put("blocks", blocks);
        } else if (mode.equals("move")) {
            String name = row.get("monster").getAsString();
            Class<?> klass = Class.forName("com.megacrit.cardcrawl.monsters.exordium." + name);
            AbstractMonster monster = (AbstractMonster)unsafe.allocateInstance(klass);
            monster.moveHistory = new ArrayList<>();
            for (var move : row.getAsJsonArray("history")) monster.moveHistory.add(move.getAsByte());
            monster.damage = new ArrayList<>();
            for (int damage : new int[]{16, 8}) {
                DamageInfo info = (DamageInfo)unsafe.allocateInstance(DamageInfo.class); info.base = damage;
                monster.damage.add(info);
            }
            if (name.equals("GremlinNob")) {
                Field used = klass.getDeclaredField("usedBellow"); used.setAccessible(true);
                used.setBoolean(monster, !monster.moveHistory.isEmpty());
                Field vuln = klass.getDeclaredField("canVuln"); vuln.setAccessible(true); vuln.setBoolean(monster, true);
            }
            AbstractDungeon.ascensionLevel = 20;
            Method getMove = klass.getDeclaredMethod("getMove", int.class); getMove.setAccessible(true);
            getMove.invoke(monster, row.get("roll").getAsInt());
            Field moveField = AbstractMonster.class.getDeclaredField("move"); moveField.setAccessible(true);
            Object move = moveField.get(monster);
            result.put("selected", move.getClass().getField("nextMove").get(move));
        } else { throw new IllegalArgumentException("unknown mode"); }
        result.put("final", state(rng));
        return result;
    }

    public static void main(String[] args) throws Exception {
        prepare();
        JsonArray input = gson.fromJson(Files.readString(Paths.get(args[0])), JsonArray.class);
        ArrayList<Object> output = new ArrayList<>();
        for (var row : input) output.add(execute(row.getAsJsonObject()));
        System.out.println("SLS_STOCK_JSON=" + gson.toJson(output));
    }
}
