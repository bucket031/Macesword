package com.example.macesword;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Locale;
/**
 * Every number the config menu can change with a slider.
 * Values are stored as whole "units" so decimals don't drift: units / scale = the real value.
 */
public enum Slider {
    FALL_DISTANCE("sword.activation_fall_distance", "Fall distance", 1, 40, 1, 2, 4, 2, Kind.BLOCKS),
    SMASH_DAMAGE("smash.damage_multiplier", "Smash damage", 0, 30, 1, 5, 10, 10, Kind.MULTIPLIER),
    ANVIL_COST("anvil.cost_per_level", "Anvil cost", 1, 10, 1, 1, 2, 1, Kind.LEVELS);
    public enum Kind { BLOCKS, MULTIPLIER, LEVELS }
    /** How many positions the clickable bar has at most (menu rows are 9 wide, minus 2 buttons). */
    public static final int MAX_POSITIONS = 7;
    private final String path;
    private final String label;
    private final int min;
    private final int max;
    private final int step;
    private final int bigStep;
    private final int def;
    private final int scale;
    private final Kind kind;
    Slider(String path, String label, int min, int max, int step, int bigStep, int def, int scale, Kind kind) {
        this.path = path;
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.bigStep = bigStep;
        this.def = def;
        this.scale = scale;
        this.kind = kind;
    }
    public String label() { return label; }
    public int step() { return step; }
    public int bigStep() { return bigStep; }
    public int def() { return def; }
    public int get(FileConfiguration config) {
        double stored = config.getDouble(path, def / (double) scale);
        return clamp((int) Math.round(stored * scale));
    }
    public void set(FileConfiguration config, int units) {
        int value = clamp(units);
        if (scale == 1) {
            config.set(path, value);
        } else {
            config.set(path, value / (double) scale);
        }
    }
    public int clamp(int units) {
        return Math.max(min, Math.min(max, units));
    }
    /** Number of clickable positions on the bar. */
    public int positions() {
        return Math.max(1, Math.min(MAX_POSITIONS, (max - min) / step + 1));
    }
    /** The value you get by clicking position idx (0 = far left). */
    public int valueAtPosition(int idx) {
        int n = positions();
        if (n == 1) return min;
        double fraction = idx / (double) (n - 1);
        int steps = (int) Math.round(fraction * ((max - min) / (double) step));
        return clamp(min + steps * step);
    }
    /** Which position the "knob" sits on for a value. */
    public int positionOf(int units) {
        int n = positions();
        if (n == 1) return 0;
        double fraction = (clamp(units) - min) / (double) (max - min);
        return (int) Math.round(fraction * (n - 1));
    }
    public String format(int units) {
        return switch (kind) {
            case BLOCKS -> String.format(Locale.ROOT, "%.1f blocks", units / (double) scale);
            case MULTIPLIER -> String.format(Locale.ROOT, "%.1fx", units / (double) scale);
            case LEVELS -> units + (units == 1 ? " level" : " levels");
        };
    }
    /** How much one click changes the value, for button tooltips. */
    public String amount(int delta) {
        return switch (kind) {
            case BLOCKS -> String.format(Locale.ROOT, "%.1f blocks", delta / (double) scale);
            case MULTIPLIER -> String.format(Locale.ROOT, "%.1fx", delta / (double) scale);
            case LEVELS -> delta + (delta == 1 ? " level" : " levels");
        };
    }
}
