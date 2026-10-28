package com.example.macesword;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.Locale;
/** Creates the Mace Sword, recognises it, and swaps its look between sword and mace. */
public final class SwordItem {
    private static final NamespacedKey MACE_MODEL = NamespacedKey.minecraft("mace");
    private final MaceSword plugin;
    private final NamespacedKey tagKey;
    public SwordItem(MaceSword plugin) {
        this.plugin = plugin;
        this.tagKey = new NamespacedKey(plugin, "mace_sword");
    }
    public ItemStack create() {
        ItemStack stack = new ItemStack(baseMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.bold("Mace Sword", NamedTextColor.AQUA));
        meta.lore(List.<Component>of(
                Text.plain("A blade that remembers the mace.", NamedTextColor.GRAY),
                Text.plain("", NamedTextColor.GRAY),
                Text.plain("Fall onto a target to smash like a mace.", NamedTextColor.GRAY),
                Text.plain("Takes sword and mace enchantments", NamedTextColor.DARK_GRAY),
                Text.plain("(Density or Breach, never both).", NamedTextColor.DARK_GRAY)));
        meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }
    private Material baseMaterial() {
        String name = plugin.getConfig().getString("sword.base_material", "NETHERITE_SWORD");
        Material material = Material.matchMaterial(name == null ? "" : name);
        if (material == null || !material.name().toUpperCase(Locale.ROOT).endsWith("_SWORD")) {
            return Material.NETHERITE_SWORD;
        }
        return material;
    }
    public boolean isMaceSword(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(tagKey, PersistentDataType.BYTE);
    }
    /** True while the sword is showing the mace model. */
    public boolean isMaceForm(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta.hasItemModel() && MACE_MODEL.equals(meta.getItemModel());
    }
    /** Only the look changes. It stays a real sword, so it keeps the sword's attack cooldown. */
    public void setForm(ItemStack stack, boolean mace) {
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(mace ? MACE_MODEL : null);
        stack.setItemMeta(meta);
    }
}
