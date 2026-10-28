package com.example.macesword;
import org.bukkit.enchantments.Enchantment;
import java.util.Locale;
/** The three enchantments that normally only a mace can carry. */
final class MaceEnchants {
    private MaceEnchants() {}
    static boolean isMace(Enchantment enchantment) {
        return enchantment.equals(Enchantment.DENSITY)
                || enchantment.equals(Enchantment.BREACH)
                || enchantment.equals(Enchantment.WIND_BURST);
    }
    /** The enchantment that can't share a sword with this one (Density and Breach), or null. */
    static Enchantment rival(Enchantment enchantment) {
        if (enchantment.equals(Enchantment.DENSITY)) return Enchantment.BREACH;
        if (enchantment.equals(Enchantment.BREACH)) return Enchantment.DENSITY;
        return null;
    }
    static Enchantment byName(String name) {
        return switch (name.toLowerCase(Locale.ROOT).replace("-", "_")) {
            case "density" -> Enchantment.DENSITY;
            case "breach" -> Enchantment.BREACH;
            case "wind_burst", "windburst" -> Enchantment.WIND_BURST;
            default -> null;
        };
    }
}
