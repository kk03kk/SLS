import java.util.ArrayList;
import spirecomm.parity.CardGroupRngPatch;
import spirecomm.parity.DungeonSeedPatch;
import spirecomm.parity.OracleMode;
import spirecomm.parity.OracleScenarioPatch;
import spirecomm.parity.ParityRng;

/** Runs in a fresh JVM against the real dependency JARs, without a game window. */
public class OracleModeProbe {
    public static void main(String[] args) {
        if (OracleMode.validation() != args[0].equals("validation")) {
            throw new AssertionError("incorrect mode");
        }
        if (!OracleMode.validation()) {
            // Null game objects are deliberate: production must delegate before touching them.
            if (CardGroupRngPatch.AnyCard.Prefix(null, false).isPresent()
                || CardGroupRngPatch.ByRarity.Prefix(null, false, null).isPresent()
                || CardGroupRngPatch.ByType.Prefix(null, null, false).isPresent()) {
                throw new AssertionError("production overrides stock random selection");
            }
            DungeonSeedPatch.NewRun.Postfix();
            DungeonSeedPatch.LoadedRun.Postfix();
            if (ParityRng.mathRng != null) throw new AssertionError("production initializes replacement RNG");
            ArrayList<String> commands = new ArrayList<String>();
            commands.add("state");
            if (OracleScenarioPatch.Advertise.Postfix(commands).size() != 1
                || OracleScenarioPatch.Execute.Prefix("parity_card STRIKE_RED 0").isPresent()) {
                throw new AssertionError("production permits scenario mutation");
            }
        } else {
            try {
                System.setProperty("spirecomm.math_seed", "123");
                java.lang.reflect.Field baseline = ParityRng.class.getDeclaredField("relicProbeBaseline");
                baseline.setAccessible(true);
                Object old = java.lang.reflect.Array.newInstance(baseline.getType().getComponentType(), 1);
                baseline.set(null, old);
                ParityRng.reset();
                if (baseline.get(null) != null) throw new AssertionError("old probe baseline survives a reset");
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        }
        System.out.println("ORACLE_MODE_OK " + OracleMode.MODE);
    }
}
