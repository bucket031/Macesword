package com.example.macesword;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * The /macesword config menu.
 *
 * Minecraft inventories have no real sliders, so each slider is one row:
 *   [ - ] [ bar: up to 7 clickable positions ] [ + ]
 * Click the bar to jump to a value, or use - / + to fine tune (shift-click for bigger steps).
 */
public final class ConfigMenu implements InventoryHolder {
    private static final int SIZE = 45;
    private static final int VISUAL_SLOT = 38;
    private static final int MATERIAL_SLOT = 40;
    private static final int RESET_SLOT = 42;
    private static final List<Material> SWORDS = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);
    private static final Slider[] SLIDERS = {Slider.FALL_DISTANCE, Slider.SMASH_DAMAGE, Slider.ANVIL_COST};
    private final MaceSword plugin;
    private final Inventory inventory;
    public ConfigMenu(MaceSword plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text("MaceSword Settings"));
        render();
    }
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    public void open(Player player) {
        player.openInventory(inventory);
    }
    // ------------------------------------------------------------- drawing
    private void render() {
        FileConfiguration config = plugin.getConfig();
        inventory.clear();
        ItemStack filler = button(Material.BLACK_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY, List.of());
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(4, button(currentSword(), "Mace Sword Settings", NamedTextColor.AQUA, List.of(
                "Click a bar to jump to a value.",
                "Use - / + to fine tune.",
                "Shift-click - / + for bigger steps.")));
        for (int i = 0; i < SLIDERS.length; i++) {
            renderSlider(SLIDERS[i], (i + 1) * 9);
        }
        boolean visual = config.getBoolean("sword.visual_mace_form", true);
        inventory.setItem(VISUAL_SLOT, button(visual ? Material.LIME_DYE : Material.GRAY_DYE,
                "Mace look while falling: " + (visual ? "ON" : "OFF"),
                visual ? NamedTextColor.GREEN : NamedTextColor.RED,
                List.of("Show the mace model once it", "has fallen far enough.", "Click to toggle")));
        inventory.setItem(MATERIAL_SLOT, button(currentSword(),
                "Base sword: " + pretty(currentSword()), NamedTextColor.AQUA,
                List.of("Which sword newly given", "Mace Swords are made from.", "Click: next   Shift-click: previous")));
        inventory.setItem(RESET_SLOT, button(Material.BARRIER, "Reset to defaults", NamedTextColor.RED,
                List.of("Reset every setting on this page")));
    }
    private void renderSlider(Slider slider, int base) {
        int value = slider.get(plugin.getConfig());
        int positions = slider.positions();
        int thumb = slider.positionOf(value);
        int start = trackStart(slider);
        inventory.setItem(base, button(Material.RED_CONCRETE, "- " + slider.label(), NamedTextColor.RED, List.of(
                "Click: -" + slider.amount(slider.step()),
                "Shift-click: -" + slider.amount(slider.bigStep()))));
        inventory.setItem(base + 8, button(Material.GREEN_CONCRETE, "+ " + slider.label(), NamedTextColor.GREEN, List.of(
                "Click: +" + slider.amount(slider.step()),
                "Shift-click: +" + slider.amount(slider.bigStep()))));
        for (int idx = 0; idx < positions; idx++) {
            int slot = base + start + idx;
            if (idx == thumb) {
                ItemStack knob = button(Material.GOLD_BLOCK, slider.label() + ": " + slider.format(value),
                        NamedTextColor.GOLD, List.of("Current value"));
                ItemMeta meta = knob.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                knob.setItemMeta(meta);
                inventory.setItem(slot, knob);
            } else {
                Material material = idx < thumb
                        ? Material.YELLOW_STAINED_GLASS_PANE
                        : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                inventory.setItem(slot, button(material,
                        slider.label() + ": set to " + slider.format(slider.valueAtPosition(idx)),
                        NamedTextColor.YELLOW, List.of("Click to set this value")));
            }
        }
    }
    /** Slot offset (within the row) of the bar's first position, so short bars sit in the middle. */
    private static int trackStart(Slider slider) {
        return 1 + (Slider.MAX_POSITIONS - slider.positions()) / 2;
    }
    private Material currentSword() {
        String name = plugin.getConfig().getString("sword.base_material", "NETHERITE_SWORD");
        Material material = Material.matchMaterial(name == null ? "" : name);
        return material != null && SWORDS.contains(material) ? material : Material.NETHERITE_SWORD;
    }
    private static String pretty(Material material) {
        StringBuilder out = new StringBuilder();
        for (String word : material.name().toLowerCase(Locale.ROOT).split("_")) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
    private static ItemStack button(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.plain(name, color));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Text.plain(line, NamedTextColor.GRAY));
        }
        meta.lore(lines);
        stack.setItemMeta(meta);
        return stack;
    }
    // -------------------------------------------------------------- clicks
    public void handleClick(Player player, int slot, boolean shift) {
        FileConfiguration config = plugin.getConfig();
        if (slot == VISUAL_SLOT) {
            config.set("sword.visual_mace_form", !config.getBoolean("sword.visual_mace_form", true));
            changed(player);
            return;
        }
        if (slot == MATERIAL_SLOT) {
            int index = SWORDS.indexOf(currentSword());
            int next = Math.floorMod(index + (shift ? -1 : 1), SWORDS.size());
            config.set("sword.base_material", SWORDS.get(next).name());
            changed(player);
            return;
        }
        if (slot == RESET_SLOT) {
            for (Slider slider : SLIDERS) {
                slider.set(config, slider.def());
            }
            config.set("sword.visual_mace_form", true);
            config.set("sword.base_material", "NETHERITE_SWORD");
            changed(player);
            return;
        }
        int row = slot / 9 - 1;
        if (row < 0 || row >= SLIDERS.length) return;
        Slider slider = SLIDERS[row];
        int column = slot % 9;
        int current = slider.get(config);
        int amount = shift ? slider.bigStep() : slider.step();
        Integer updated = null;
        if (column == 0) {
            updated = current - amount;
        } else if (column == 8) {
            updated = current + amount;
        } else {
            int idx = column - trackStart(slider);
            if (idx >= 0 && idx < slider.positions()) {
                updated = slider.valueAtPosition(idx);
            }
        }
        if (updated == null) return;
        slider.set(config, updated);
        changed(player);
    }
    private void changed(Player player) {
        plugin.saveConfig();
        render();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
    }
}
