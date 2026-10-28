package com.example.macesword;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/** /macesword give [player] | enchant <density|breach|wind_burst> <level> | reload */
public final class SwordCommand implements TabExecutor {
    private final MaceSword plugin;
    public SwordCommand(MaceSword plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "enchant" -> enchant(sender, args);
            case "config" -> config(sender);
            case "reload" -> {
                plugin.reloadConfig();
                msg(sender, "MaceSword config reloaded.", NamedTextColor.GREEN);
            }
            default -> help(sender, label);
        }
        return true;
    }
    // ----------------------------------------------------------------- give
    private void give(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg(sender, "Player '" + args[1] + "' is not online.", NamedTextColor.RED);
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            msg(sender, "Specify a player: /macesword give <player>", NamedTextColor.RED);
            return;
        }
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(plugin.sword().create());
        leftover.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        msg(sender, "Gave the Mace Sword to " + target.getName() + ".", NamedTextColor.GREEN);
        if (!target.equals(sender)) {
            msg(target, "You received a Mace Sword.", NamedTextColor.AQUA);
        }
    }
    // -------------------------------------------------------------- enchant
    private void enchant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg(sender, "Run this in-game while holding the Mace Sword.", NamedTextColor.RED);
            return;
        }
        if (args.length < 3) {
            msg(sender, "Usage: /macesword enchant <density|breach|wind_burst> <level>  (level 0 removes it)",
                    NamedTextColor.YELLOW);
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.sword().isMaceSword(hand)) {
            msg(sender, "Hold the Mace Sword in your main hand first.", NamedTextColor.RED);
            return;
        }
        Enchantment enchantment = MaceEnchants.byName(args[1]);
        if (enchantment == null) {
            msg(sender, "Unknown enchantment. Use density, breach or wind_burst.", NamedTextColor.RED);
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msg(sender, "Level must be a number.", NamedTextColor.RED);
            return;
        }
        if (level < 0 || level > enchantment.getMaxLevel()) {
            msg(sender, "Level must be between 0 and " + enchantment.getMaxLevel() + ".", NamedTextColor.RED);
            return;
        }
        ItemMeta meta = hand.getItemMeta();
        Enchantment rival = MaceEnchants.rival(enchantment);
        if (level > 0 && rival != null && meta.hasEnchant(rival)) {
            msg(sender, "This sword already has that enchantment's rival. "
                    + "Density and Breach can't be combined. Remove the other one first.", NamedTextColor.RED);
            return;
        }
        if (level == 0) {
            meta.removeEnchant(enchantment);
        } else {
            meta.addEnchant(enchantment, level, true);
        }
        hand.setItemMeta(meta);
        player.getInventory().setItemInMainHand(hand);
        msg(sender, level == 0 ? "Removed it from the sword." : "Added it to the sword.", NamedTextColor.GREEN);
    }
    // --------------------------------------------------------------- config
    private void config(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "The config menu can only be opened in-game.", NamedTextColor.RED);
            return;
        }
        new ConfigMenu(plugin).open(player);
    }
    // ----------------------------------------------------------------- help
    private void help(CommandSender sender, String label) {
        msg(sender, "MaceSword commands:", NamedTextColor.GOLD);
        msg(sender, "/" + label + " give [player]  - give the Mace Sword", NamedTextColor.WHITE);
        msg(sender, "/" + label + " enchant <density|breach|wind_burst> <level>  - enchant the held sword", NamedTextColor.WHITE);
        msg(sender, "/" + label + " config  - open the settings menu", NamedTextColor.WHITE);
        msg(sender, "/" + label + " reload  - reload config.yml", NamedTextColor.WHITE);
    }
    private static void msg(CommandSender to, String text, NamedTextColor color) {
        to.sendMessage(Component.text(text, color));
    }
    // --------------------------------------------------------- tab complete
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("give", "enchant", "config", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enchant")) {
            out.addAll(List.of("density", "breach", "wind_burst"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("enchant")) {
            out.addAll(List.of("0", "1", "2", "3", "4", "5"));
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(typed));
        return out;
    }
}
