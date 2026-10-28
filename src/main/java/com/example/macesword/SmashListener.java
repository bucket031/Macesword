package com.example.macesword;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * The mace smash attack, done by hand because the vanilla smash only works for the real mace item.
 *
 * Follows the vanilla mace: bonus damage grows with the fall, Density adds more per block,
 * Breach cuts through armor, Wind Burst launches you, and nearby mobs get knocked back.
 */
public final class SmashListener implements Listener {
    private static final long SAFE_LANDING_MILLIS = 8000;
    private final MaceSword plugin;
    /** Players launched by Wind Burst who shouldn't take fall damage on the way down. */
    private final Map<UUID, Long> safeLanding = new HashMap<>();
    public SmashListener(MaceSword plugin) {
        this.plugin = plugin;
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!plugin.sword().isMaceSword(weapon)) return;
        double fall = player.getFallDistance();
        if (fall < plugin.activationDistance() || player.isGliding()) return;
        int density = weapon.getEnchantmentLevel(Enchantment.DENSITY);
        int breach = weapon.getEnchantmentLevel(Enchantment.BREACH);
        int windBurst = weapon.getEnchantmentLevel(Enchantment.WIND_BURST);
        double bonus = smashBonus(fall) + 0.5 * density * fall;
        bonus *= plugin.smashMultiplier();
        double damage = event.getDamage() + bonus;
        if (breach > 0) {
            damage = applyBreach(target, damage, breach);
        }
        event.setDamage(damage);
        // A smash resets the fall, so there is no fall damage and no second smash from the same fall.
        player.setFallDistance(0f);
        Location impact = target.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> afterSmash(player, target, impact, fall, windBurst));
    }
    /** Vanilla mace smash bonus: 4 per block up to 3, then 2 per block up to 8, then 1 per block. */
    private static double smashBonus(double fall) {
        if (fall <= 3.0) return 4.0 * fall;
        if (fall <= 8.0) return 12.0 + 2.0 * (fall - 3.0);
        return 22.0 + (fall - 8.0);
    }
    /**
     * Breach makes armor 15% less effective per level. Works out how much damage the target's armor
     * would normally absorb and scales the damage up to account for the reduced protection.
     */
    private static double applyBreach(LivingEntity target, double damage, int level) {
        double armor = attribute(target, Attribute.ARMOR);
        double toughness = attribute(target, Attribute.ARMOR_TOUGHNESS);
        double reduction = Math.min(20.0, Math.max(armor * 0.2, armor - damage / (2.0 + toughness / 4.0)));
        double fraction = reduction / 25.0;
        if (fraction <= 0.0 || fraction >= 1.0) return damage;
        double breached = fraction * Math.max(0.0, 1.0 - 0.15 * level);
        return damage * (1.0 - breached) / (1.0 - fraction);
    }
    private static double attribute(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }
    // -------------------------------------------------------- after the hit
    private void afterSmash(Player player, LivingEntity target, Location impact, double fall, int windBurst) {
        World world = impact.getWorld();
        if (world == null) return;
        world.playSound(impact,
                fall > 5.0 ? Sound.ITEM_MACE_SMASH_GROUND_HEAVY : Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 1.0f);
        world.spawnParticle(Particle.CRIT, impact.clone().add(0, 1, 0), 30, 0.6, 0.4, 0.6, 0.2);
        knockbackNearby(player, target, impact, fall);
        if (windBurst > 0 && player.isOnline()) {
            double launch = 0.7 + 0.4 * Math.min(3, windBurst); // about 1.1 / 1.5 / 1.9
            Vector velocity = player.getVelocity();
            velocity.setY(launch);
            player.setVelocity(velocity);
            safeLanding.put(player.getUniqueId(), System.currentTimeMillis() + SAFE_LANDING_MILLIS);
        }
    }
    /** Vanilla mace shockwave: everything within 3.5 blocks is thrown away from the target. */
    private void knockbackNearby(Player player, LivingEntity target, Location impact, double fall) {
        for (Entity entity : impact.getWorld().getNearbyEntities(impact, 3.5, 3.5, 3.5)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand) continue;
            if (entity.equals(player) || entity.equals(target)) continue;
            if (living instanceof Tameable pet && pet.isTamed()
                    && player.getUniqueId().equals(pet.getOwnerUniqueId())) continue;
            Vector away = living.getLocation().toVector().subtract(impact.toVector());
            double distance = away.length();
            if (distance < 1.0e-4 || distance > 3.5) continue;
            double resistance = attribute(living, Attribute.KNOCKBACK_RESISTANCE);
            double power = (3.5 - distance) * 0.7 * (fall > 5.0 ? 2.0 : 1.0) * (1.0 - resistance);
            if (power <= 0.0) continue;
            Vector push = away.normalize().multiply(power);
            living.setVelocity(living.getVelocity().add(new Vector(push.getX(), 0.7, push.getZ())));
        }
    }
    // ----------------------------------------------------------- fall damage
    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Long until = safeLanding.remove(player.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        safeLanding.remove(event.getPlayer().getUniqueId());
    }
}
