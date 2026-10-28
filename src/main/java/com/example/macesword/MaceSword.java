package com.example.macesword;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
public final class MaceSword extends JavaPlugin {
    private SwordItem sword;
    private FormManager forms;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        sword = new SwordItem(this);
        forms = new FormManager(this);
        getServer().getPluginManager().registerEvents(new SmashListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(forms, this);
        SwordCommand command = new SwordCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("macesword"));
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        forms.start();
    }
    @Override
    public void onDisable() {
        if (forms != null) forms.shutdown();
    }
    public SwordItem sword() {
        return sword;
    }
    /** Blocks you must fall before the sword turns into a mace. */
    public double activationDistance() {
        double value = getConfig().getDouble("sword.activation_fall_distance", 2.0);
        return Math.max(0.5, Math.min(50.0, value));
    }
    public boolean visualForm() {
        return getConfig().getBoolean("sword.visual_mace_form", true);
    }
    public double smashMultiplier() {
        double value = getConfig().getDouble("smash.damage_multiplier", 1.0);
        return Math.max(0.0, Math.min(10.0, value));
    }
    public int anvilCostPerLevel() {
        int value = getConfig().getInt("anvil.cost_per_level", 2);
        return Math.max(1, Math.min(10, value));
    }
}
