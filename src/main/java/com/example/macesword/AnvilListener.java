package com.example.macesword;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;
/**
 * Lets enchanted books put sword enchantments AND mace enchantments on the Mace Sword.
 * A vanilla anvil refuses Density, Breach and Wind Burst on a sword, so this builds the result itself.
 * Density and Breach can never be on the same sword.
 */
public final class AnvilListener implements Listener {
    private final MaceSword plugin;
    public AnvilListener(MaceSword plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack sword = anvil.getItem(0);
        ItemStack book = anvil.getItem(1);
        if (!plugin.sword().isMaceSword(sword)) return;
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) return;
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta storage)) return;
        Map<Enchantment, Integer> stored = storage.getStoredEnchants();
        boolean hasMaceEnchant = false;
        for (Enchantment enchantment : stored.keySet()) {
            if (MaceEnchants.isMace(enchantment)) hasMaceEnchant = true;
        }
        if (!hasMaceEnchant) return; // an ordinary book: vanilla already handles it
        ItemStack result = sword.clone();
        ItemMeta meta = result.getItemMeta();
        int levelsAdded = 0;
        for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int bookLevel = entry.getValue();
            if (MaceEnchants.isMace(enchantment)) {
                // Density XOR Breach.
                Enchantment rival = MaceEnchants.rival(enchantment);
                if (rival != null && meta.hasEnchant(rival)) continue;
            } else {
                if (!enchantment.canEnchantItem(sword)) continue;
                if (conflictsWithSwordEnchant(meta, enchantment)) continue;
            }
            int current = meta.getEnchantLevel(enchantment);
            int target = current == bookLevel ? current + 1 : Math.max(current, bookLevel);
            target = Math.min(target, enchantment.getMaxLevel());
            if (target <= current) continue;
            meta.addEnchant(enchantment, target, true);
            levelsAdded += target - current;
        }
        if (levelsAdded == 0) {
            event.setResult(null);
            return;
        }
        result.setItemMeta(meta);
        event.setResult(result);
        int cost = Math.max(1, Math.min(39, levelsAdded * plugin.anvilCostPerLevel()));
        event.getView().setRepairCost(cost);
    }
    /** Sharpness, Smite and Bane of Arthropods still exclude each other. Mace enchantments are not checked here. */
    private static boolean conflictsWithSwordEnchant(ItemMeta meta, Enchantment adding) {
        for (Enchantment existing : meta.getEnchants().keySet()) {
            if (MaceEnchants.isMace(existing) || existing.equals(adding)) continue;
            if (adding.conflictsWith(existing)) return true;
        }
        return false;
    }
}
