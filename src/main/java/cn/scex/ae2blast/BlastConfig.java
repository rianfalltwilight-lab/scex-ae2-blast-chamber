package cn.scex.ae2blast;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class BlastConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue AE_TICKS, AE_CHARGE;
    public static final ModConfigSpec.BooleanValue DISCOVER_AE;
    static {
        var b = new ModConfigSpec.Builder();
        DISCOVER_AE = b.comment("Discover all loaded AE2 explosion transform recipes, including datapacks and scripts.").define("discoverAE2", true);
        AE_TICKS = b.comment("Ticks per automatically discovered AE2 recipe.").defineInRange("ae2ProcessingTicks", 60, 1, 1200);
        AE_CHARGE = b.comment("Blast charges per AE2 operation. Gunpowder = 1; TNT = 5.").defineInRange("ae2BlastCharges", 1, 1, 64);
        SPEC = b.build();
    }
    private BlastConfig() {}
}
