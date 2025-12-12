package org.speedrainxd.CustomEnchantsX;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Set defaults if they don't exist
        if (!config.contains("gui.menu-title")) {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }
    }
    
    // ========== GUI SETTINGS ==========
    public String getMenuTitle() {
        return translateColors(config.getString("gui.menu-title", "&5Custom Enchants Menu (Click to get)"));
    }
    
    public String getBookDisplayFormat() {
        return translateColors(config.getString("gui.book-display-format", "&6Ancient Tome: {enchant}"));
    }
    
    public String getBookLoreFormat() {
        return translateColors(config.getString("gui.book-lore-format", "&7{enchant} {level}"));
    }
    
    // ========== MESSAGES ==========
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "");
        return translateColors(message);
    }
    
    // ========== VILLAGER TRADING ==========
    public boolean isVillagerTradingEnabled() {
        return config.getBoolean("villager-trading.enabled", true);
    }
    
    public int getVillagerTradeChance() {
        return config.getInt("villager-trading.trade-chance", 10);
    }
    
    public int getVillagerMaxTrades() {
        return config.getInt("villager-trading.max-trades", 20);
    }
    
    public int getEmeraldCostMin() {
        return config.getInt("villager-trading.emerald-cost-min", 10);
    }
    
    public int getEmeraldCostMax() {
        return config.getInt("villager-trading.emerald-cost-max", 20);
    }
    
    public boolean requireBookInput() {
        return config.getBoolean("villager-trading.require-book-input", true);
    }
    
    // ========== ENCHANT MECHANICS ==========
    public boolean isEnchantEnabled(String enchantName) {
        return config.getBoolean("enchant-mechanics." + convertToKey(enchantName) + ".enabled", true);
    }
    
    public double getExperienceMultiplier() {
        return config.getDouble("enchant-mechanics.experience.xp-multiplier-per-level", 1.0);
    }
    
    public double getLifeStealHearts() {
        return config.getDouble("enchant-mechanics.life-steal.hearts-per-level", 0.5);
    }
    
    public double getBleedingDamagePerLevel() {
        return config.getDouble("enchant-mechanics.bleeding.damage-per-level", 0.10);
    }
    
    public int getBleedingPoisonDurationBase() {
        return config.getInt("enchant-mechanics.bleeding.poison-duration-base", 40);
    }
    
    public double getAxeBreachBaseMultiplier() {
        return config.getDouble("enchant-mechanics.axe-breach.base-multiplier", 1.1);
    }
    
    public double getAxeBreachPerLevel() {
        return config.getDouble("enchant-mechanics.axe-breach.per-level-bonus", 0.05);
    }
    
    public double getSwordBreachBaseMultiplier() {
        return config.getDouble("enchant-mechanics.sword-breach.base-multiplier", 1.1);
    }
    
    public double getSwordBreachPerLevel() {
        return config.getDouble("enchant-mechanics.sword-breach.per-level-bonus", 0.05);
    }
    
    public double getEndSlayerBonus() {
        return config.getDouble("enchant-mechanics.end-slayer.bonus-damage-per-level", 3.0);
    }
    
    public int getBeheadingDropChance() {
        return config.getInt("enchant-mechanics.beheading.drop-chance-per-level", 5);
    }
    
    public int getAgilityDodgeChance() {
        return config.getInt("enchant-mechanics.agility.dodge-chance-per-level", 10);
    }
    
    public double getTankReductionPerLevel() {
        return config.getDouble("enchant-mechanics.tank.reduction-per-level", 0.15);
    }
    
    public int getAuraDamageBurnChance() {
        return config.getInt("enchant-mechanics.aura-damage.burn-chance-per-level", 20);
    }
    
    public int getAuraDamageFireTicks() {
        return config.getInt("enchant-mechanics.aura-damage.base-fire-ticks", 60);
    }
    
    public int getAuraDamagePerLevelTicks() {
        return config.getInt("enchant-mechanics.aura-damage.per-level-ticks", 20);
    }
    
    public int getMagneticBaseRadius() {
        return config.getInt("enchant-mechanics.magnetic.base-radius", 5);
    }
    
    public int getMagneticPerLevelRadius() {
        return config.getInt("enchant-mechanics.magnetic.per-level-radius", 3);
    }
    
    public double getVitalityHeartsPerLevel() {
        return config.getDouble("enchant-mechanics.vitality.hearts-per-level", 2.0);
    }
    
    public int getVeinMinerMaxBlocks() {
        return config.getInt("enchant-mechanics.vein-miner.max-blocks", 20);
    }
    
    public int getTimberMaxBlocks() {
        return config.getInt("enchant-mechanics.timber.max-blocks", 100);
    }
    
    public int getPassiveEffectsTickRate() {
        return config.getInt("enchant-mechanics.passive-effects-tick-rate", 40);
    }
    
    // ========== ANVIL SETTINGS ==========
    public boolean isAnvilEnabled() {
        return config.getBoolean("anvil.enabled", true);
    }
    
    public boolean allowUpgrades() {
        return config.getBoolean("anvil.allow-upgrades", true);
    }
    
    public boolean allowNewEnchants() {
        return config.getBoolean("anvil.allow-new-enchants", true);
    }
    
    // ========== DEBUG ==========
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    public boolean shouldLogEnchants() {
        return config.getBoolean("debug.log-enchants", true);
    }
    
    public boolean shouldLogCommands() {
        return config.getBoolean("debug.log-commands", true);
    }
    
    // ========== UTILITY ==========
    public FileConfiguration getConfig() {
        return config;
    }
    
    private String translateColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    private String convertToKey(String name) {
        return name.toLowerCase().replace(" ", "-");
    }
}
