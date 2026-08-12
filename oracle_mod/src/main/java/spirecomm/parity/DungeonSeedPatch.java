package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public final class DungeonSeedPatch {
    @SpirePatch(clz = AbstractDungeon.class, method = "generateSeeds")
    public static class NewRun {
        public static void Postfix() {
            ParityRng.reset();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "loadSeeds")
    public static class LoadedRun {
        public static void Postfix() {
            ParityRng.reset();
        }
    }
}
