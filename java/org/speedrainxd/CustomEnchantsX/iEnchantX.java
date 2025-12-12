package org.speedrainxd.CustomEnchantsX;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class iEnchantX extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Random random = new Random();
    private ConfigManager config;

    private static final String EXPERIENCE = "Experience";
    private static final String END_SLAYER = "End_Slayer";
    private static final String AXE_BREACH = "Axe_Breach";
    private static final String SWORD_BREACH = "Sword_Breach";
    private static final String BLEEDING = "Bleeding";
    private static final String WITHERING = "Withering";
    private static final String LIFE_STEAL = "Life_Steal";
    private static final String BEHEADING = "Beheading";
    private static final String FROST_ASPECT = "Frost_Aspect";
    private static final String LAVAWALKER = "Lavawalker";
    private static final String SPEED_BOOST = "Speed_Boost";
    private static final String VITALITY = "Vitality";
    private static final String AGILITY = "Agility";
    private static final String NIGHT_OWL = "Night_Owl";
    private static final String TANK = "Tank";
    private static final String MELTING = "Melting";
    private static final String VEIN_MINER = "Vein_Miner";
    private static final String TIMBER = "Timber";
    private static final String TELEPATHY = "Telepathy";
    private static final String AURA_DAMAGE = "Aura_Damage";
    private static final String MAGNETIC = "Magnetic";

    private final String[] ALL_ENCHANTS = {
            EXPERIENCE, END_SLAYER, AXE_BREACH, SWORD_BREACH, BLEEDING, WITHERING, LIFE_STEAL, BEHEADING, FROST_ASPECT,
            LAVAWALKER, SPEED_BOOST, VITALITY, AGILITY, NIGHT_OWL, TANK,
            MELTING, VEIN_MINER, TIMBER, TELEPATHY, AURA_DAMAGE, MAGNETIC
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.config = new ConfigManager(this);
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("enchants") != null) {
            getCommand("enchants").setExecutor(this);
            getCommand("enchants").setTabCompleter(this);
        }
        if (getCommand("ienchant") != null) {
            getCommand("ienchant").setExecutor(this);
            getCommand("ienchant").setTabCompleter(this);
        }

        getLogger().info(config.getMessage("plugin-enabled"));
        getLogger().info(config.getMessage("enchants-available").replace("{count}", String.valueOf(ALL_ENCHANTS.length)));
        getLogger().info("Config saved to: plugins/iEnchantX/config.yml");

        Bukkit.getScheduler().runTaskTimer(this, this::handlePassiveEffects, 0L, config.getPassiveEffectsTickRate());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();
        Player player = (Player) sender;

        if (!player.isOp()) return new ArrayList<>();

        if (command.getName().equalsIgnoreCase("ienchant")) {
            if (args.length == 2) {
                List<String> enchants = new ArrayList<>();
                String input = args[1].toLowerCase();
                for (String enchant : ALL_ENCHANTS) {
                    if (enchant.toLowerCase().startsWith(input)) {
                        enchants.add(enchant);
                    }
                }
                return enchants;
            } else if (args.length == 3) {
                String enchantName = args[1];
                int maxLevel = getMaxLevel(enchantName);
                List<String> levels = new ArrayList<>();
                for (int i = 1; i <= maxLevel; i++) {
                    levels.add(String.valueOf(i));
                }
                return levels;
            }
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reload")) {
            if (sender.isOp()) {
                config.reload();
                sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
            }
            return true;
        }
        return handleMainCommand(sender, command, label, args);
    }

    public boolean handleMainCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getMessage("players-only"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("enchants")) {
            openEnchantMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("ienchant")) {
            return handleEnchantCommand(player, args);
        }

        return false;
    }

    private boolean handleEnchantCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(config.getMessage("enchant-usage"));
            player.sendMessage(config.getMessage("enchant-example"));
            player.sendMessage(config.getMessage("enchant-available").replace("{enchants}", String.join(", ", ALL_ENCHANTS)));
            return true;
        }

        String playerTarget = args[0];
        String enchantName = args[1];
        String levelStr = args[2];

        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            player.sendMessage(config.getMessage("enchant-invalid-level").replace("{level}", levelStr));
            return true;
        }

        List<Player> targets = getTargetPlayers(playerTarget, player);
        if (targets.isEmpty()) {
            player.sendMessage(config.getMessage("enchant-no-players").replace("{player}", playerTarget));
            return true;
        }

        boolean enchantFound = false;
        for (String enchant : ALL_ENCHANTS) {
            if (enchant.equalsIgnoreCase(enchantName)) {
                enchantName = enchant;
                enchantFound = true;
                break;
            }
        }

        if (!enchantFound) {
            player.sendMessage(config.getMessage("enchant-not-found").replace("{enchant}", enchantName));
            player.sendMessage(config.getMessage("enchant-available").replace("{enchants}", String.join(", ", ALL_ENCHANTS)));
            return true;
        }

        int maxLevel = getMaxLevel(enchantName);
        if (level < 1 || level > maxLevel) {
            player.sendMessage(config.getMessage("enchant-max-level").replace("{enchant}", enchantName).replace("{max}", String.valueOf(maxLevel)));
            return true;
        }

        for (Player target : targets) {
            ItemStack mainHand = target.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.AIR) {
                target.sendMessage(config.getMessage("enchant-no-item"));
                continue;
            }

            ItemMeta meta = mainHand.getItemMeta();
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(mainHand.getType());
            }

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String displayEnchantName = enchantName.replaceAll("_", " ");

            boolean alreadyHas = false;
            for (int i = 0; i < lore.size(); i++) {
                String line = ChatColor.stripColor(lore.get(i));
                if (line.equals(displayEnchantName) || line.startsWith(displayEnchantName + " ")) {
                    String newLine = displayEnchantName;
                    if (hasMultipleLevels(enchantName) || level > 1) {
                        newLine += " " + intToRoman(level);
                    }
                    lore.set(i, newLine);
                    alreadyHas = true;
                    break;
                }
            }

            if (!alreadyHas) {
                String loreLine = displayEnchantName;
                if (hasMultipleLevels(enchantName) || level > 1) {
                    loreLine += " " + intToRoman(level);
                }
                lore.add(loreLine);
            }

            meta.setLore(lore);
            mainHand.setItemMeta(meta);

            target.sendMessage(config.getMessage("enchant-applied").replace("{enchant}", displayEnchantName).replace("{level}", intToRoman(level)));
        }

        player.sendMessage(config.getMessage("enchant-applied-broadcast").replace("{enchant}", enchantName.replaceAll("_", " ")).replace("{level}", intToRoman(level)).replace("{count}", String.valueOf(targets.size())));
        return true;
    }

    private List<Player> getTargetPlayers(String selector, Player executor) {
        List<Player> targets = new ArrayList<>();

        if (selector.equalsIgnoreCase("@p")) {
            Player closest = null;
            double closestDist = Double.MAX_VALUE;
            for (Player p : Bukkit.getOnlinePlayers()) {
                double dist = executor.getLocation().distance(p.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = p;
                }
            }
            if (closest != null) targets.add(closest);
        } else if (selector.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else if (selector.equalsIgnoreCase("@s")) {
            targets.add(executor);
        } else {
            Player p = Bukkit.getPlayerExact(selector);
            if (p != null) targets.add(p);
        }

        return targets;
    }

    private void openEnchantMenu(Player player) {
        int totalItems = 0;
        for (String enchant : ALL_ENCHANTS) {
            totalItems += getMaxLevel(enchant);
        }
        int rows = (totalItems + 8) / 9;
        int size = Math.min(rows * 9, 54);

        Inventory menu = Bukkit.createInventory(null, size, config.getMenuTitle());

        for (String enchant : ALL_ENCHANTS) {
            int maxLevel = getMaxLevel(enchant);
            for (int level = 1; level <= maxLevel; level++) {
                menu.addItem(createEnchantedBook(enchant, level));
            }
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(config.getMenuTitle())) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                Player player = (Player) event.getWhoClicked();
                ItemStack book = event.getCurrentItem().clone();

                player.getInventory().addItem(book);

                String displayName = "Enchant Book";
                if (book.hasItemMeta() && book.getItemMeta().hasDisplayName()) {
                    displayName = book.getItemMeta().getDisplayName();
                }
                player.sendMessage(config.getMessage("book-taken").replace("{enchant}", displayName));
            }
        }
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!config.isVillagerTradingEnabled()) return;
        if (!(event.getRightClicked() instanceof Merchant)) return;

        Merchant merchant = (Merchant) event.getRightClicked();
        List<MerchantRecipe> recipes = new ArrayList<>(merchant.getRecipes());

        int tradeChance = config.getVillagerTradeChance();
        int maxTrades = config.getVillagerMaxTrades();
        int costMin = config.getEmeraldCostMin();
        int costMax = config.getEmeraldCostMax();

        if (recipes.size() >= maxTrades) return;
        if (random.nextInt(100) >= tradeChance) return;

        String randomEnchant = ALL_ENCHANTS[random.nextInt(ALL_ENCHANTS.length)];
        boolean enchantExists = false;

        for (MerchantRecipe recipe : recipes) {
            ItemStack result = recipe.getResult();
            if (result.getType() == Material.ENCHANTED_BOOK && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
                String displayName = result.getItemMeta().getDisplayName();
                if (displayName.contains(randomEnchant.replaceAll("_", " "))) {
                    enchantExists = true;
                    break;
                }
            }
        }

        if (enchantExists) return;

        ItemStack book = createEnchantedBook(randomEnchant);
        int emeraldCost = random.nextInt((costMax - costMin) + 1) + costMin;
        ItemStack cost = new ItemStack(Material.EMERALD, emeraldCost);

        MerchantRecipe newTrade = new MerchantRecipe(book, 3);
        newTrade.addIngredient(cost);

        if (config.requireBookInput()) {
            newTrade.addIngredient(new ItemStack(Material.BOOK));
        }

        recipes.add(newTrade);
        merchant.setRecipes(recipes);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader)) return;

        WanderingTrader trader = (WanderingTrader) event.getEntity();

        if (random.nextInt(100) >= 75) return;

        List<MerchantRecipe> recipes = new ArrayList<>(trader.getRecipes());

        int numBooks = random.nextInt(2) + 2;
        for (int i = 0; i < numBooks; i++) {
            String randomEnchant = ALL_ENCHANTS[random.nextInt(ALL_ENCHANTS.length)];

            boolean enchantExists = false;
            for (MerchantRecipe recipe : recipes) {
                ItemStack result = recipe.getResult();
                if (result.getType() == Material.ENCHANTED_BOOK && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
                    String displayName = result.getItemMeta().getDisplayName();
                    if (displayName.contains(randomEnchant.replaceAll("_", " "))) {
                        enchantExists = true;
                        break;
                    }
                }
            }

            if (enchantExists) continue;

            ItemStack book = createEnchantedBook(randomEnchant, random.nextInt(getMaxLevel(randomEnchant)) + 1);
            int emeraldCost = random.nextInt(11) + 10;
            ItemStack cost = new ItemStack(Material.EMERALD, emeraldCost);

            MerchantRecipe newTrade = new MerchantRecipe(book, 3);
            newTrade.addIngredient(cost);

            recipes.add(newTrade);
        }

        trader.setRecipes(recipes);
    }

    private ItemStack createEnchantedBook(String name) {
        return createEnchantedBook(name, 1);
    }

    private ItemStack createEnchantedBook(String name, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return book;

        int maxLevel = getMaxLevel(name);
        if (level > maxLevel) level = maxLevel;
        if (level < 1) level = 1;

        List<String> lore = new ArrayList<>();
        String displayName = name.replaceAll("_", " ");

        String loreFormat;
        if (hasMultipleLevels(name) || level > 1) {
            loreFormat = config.getBookLoreFormat().replace("{enchant}", displayName).replace("{level}", intToRoman(level));
        } else {
            loreFormat = config.getBookLoreFormat().replace("{enchant}", displayName).replace(" {level}", "");
        }
        lore.add(loreFormat);
        meta.setLore(lore);

        String displayFormat = config.getBookDisplayFormat().replace("{enchant}", displayName);
        meta.setDisplayName(displayFormat);

        book.setItemMeta(meta);
        return book;
    }

    private int getMaxLevel(String enchantName) {
        if (config.getConfig().getStringList("enchant-levels.three-level").contains(enchantName)) {
            return 3;
        } else if (config.getConfig().getStringList("enchant-levels.two-level").contains(enchantName)) {
            return 2;
        }
        return 1;
    }

    private boolean hasMultipleLevels(String enchantName) {
        int maxLevel = getMaxLevel(enchantName);
        return maxLevel > 1;
    }

    private String intToRoman(int num) {
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            default: return String.valueOf(num);
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (!config.isAnvilEnabled()) return;

        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);

        if (first == null || second == null) return;
        if (!second.hasItemMeta() || !second.getItemMeta().hasLore()) return;

        String enchantName = null;
        int enchantLevel = 0;
        for (String line : second.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.matches("^.+ [IVX]+$")) {
                enchantName = stripped.replaceAll(" [IVX]+$", "");
                enchantLevel = romanToInt(stripped.substring(stripped.lastIndexOf(" ") + 1));
                break;
            }
        }

        if (enchantName != null) {
            ItemStack result = first.clone();
            ItemMeta resultMeta = result.getItemMeta();

            if (resultMeta == null) {
                resultMeta = Bukkit.getItemFactory().getItemMeta(result.getType());
            }

            List<String> lore = resultMeta.hasLore() ? new ArrayList<>(resultMeta.getLore()) : new ArrayList<>();
            String enchantNameUnderscore = enchantName.replaceAll(" ", "_");

            boolean hasEnchantAlready = false;
            for (int i = 0; i < lore.size(); i++) {
                String line = ChatColor.stripColor(lore.get(i));
                if (line.equals(enchantName) || line.startsWith(enchantName + " ")) {
                    hasEnchantAlready = true;

                    if (config.allowUpgrades()) {
                        int currentLevel = 1;
                        if (line.contains(" ")) {
                            currentLevel = romanToInt(line.substring(line.lastIndexOf(" ") + 1));
                        }
                        int newLevel = currentLevel + 1;
                        int maxLevel = getMaxLevel(enchantNameUnderscore);

                        if (newLevel <= maxLevel) {
                            String newLine = enchantName;
                            if (hasMultipleLevels(enchantNameUnderscore) || newLevel > 1) {
                                newLine += " " + intToRoman(newLevel);
                            }
                            lore.set(i, newLine);
                            resultMeta.setLore(lore);

                            result.setItemMeta(resultMeta);
                            event.setResult(result);
                        }
                    }
                    break;
                }
            }

            if (!hasEnchantAlready && config.allowNewEnchants()) {
                int finalLevel = enchantLevel > 0 ? enchantLevel : 1;
                String newLine = enchantName;
                if (hasMultipleLevels(enchantNameUnderscore) || finalLevel > 1) {
                    newLine += " " + intToRoman(finalLevel);
                }
                lore.add(newLine);
                resultMeta.setLore(lore);

                result.setItemMeta(resultMeta);
                event.setResult(result);
            }
        }
    }

    private int romanToInt(String roman) {
        switch (roman.toUpperCase()) {
            case "I": return 1;
            case "II": return 2;
            case "III": return 3;
            default: return 1;
        }
    }

    private boolean hasEnchant(ItemStack item, String name) {
        return getEnchantLevel(item, name) > 0;
    }

    private int getEnchantLevel(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return 0;

        String displayName = name.replaceAll("_", " ");
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith(displayName + " ")) {
                String levelStr = stripped.substring(stripped.lastIndexOf(" ") + 1);
                return romanToInt(levelStr);
            }
        }
        return 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player) {
            Player p = (Player) event.getDamager();
            ItemStack weapon = p.getInventory().getItemInMainHand();

            if (hasEnchant(weapon, WITHERING) && config.isEnchantEnabled(WITHERING) && event.getEntity() instanceof LivingEntity) {
                int level = getEnchantLevel(weapon, WITHERING);
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, level - 1));
            }
            if (hasEnchant(weapon, FROST_ASPECT) && config.isEnchantEnabled(FROST_ASPECT) && event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 10));
                target.getWorld().spawnParticle(org.bukkit.Particle.ITEM_SNOWBALL, target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);

                if (target instanceof Player) {
                    Player playerTarget = (Player) target;
                    playerTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                }
            }
            if (hasEnchant(weapon, BLEEDING) && config.isEnchantEnabled(BLEEDING) && event.getEntity() instanceof LivingEntity) {
                int level = getEnchantLevel(weapon, BLEEDING);
                double damagePerLevel = config.getBleedingDamagePerLevel();
                double damageMultiplier = 1.0 + (damagePerLevel * level);
                event.setDamage(event.getDamage() * damageMultiplier);
                int poisonDuration = config.getBleedingPoisonDurationBase() + (level * 10);
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, level - 1));
            }
            if (hasEnchant(weapon, LIFE_STEAL) && config.isEnchantEnabled(LIFE_STEAL)) {
                int level = getEnchantLevel(weapon, LIFE_STEAL);
                double heartsPerLevel = config.getLifeStealHearts();
                double healAmount = heartsPerLevel * level;
                double newHealth = Math.min(p.getHealth() + healAmount, p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                p.setHealth(newHealth);
            }
            if (hasEnchant(weapon, AXE_BREACH) && config.isEnchantEnabled(AXE_BREACH)) {
                int level = getEnchantLevel(weapon, AXE_BREACH);
                double baseMult = config.getAxeBreachBaseMultiplier();
                double perLevel = config.getAxeBreachPerLevel();
                event.setDamage(event.getDamage() * (baseMult + (perLevel * level)));
            }
            if (hasEnchant(weapon, SWORD_BREACH) && config.isEnchantEnabled(SWORD_BREACH)) {
                int level = getEnchantLevel(weapon, SWORD_BREACH);
                double baseMult = config.getSwordBreachBaseMultiplier();
                double perLevel = config.getSwordBreachPerLevel();
                event.setDamage(event.getDamage() * (baseMult + (perLevel * level)));
            }
            if (hasEnchant(weapon, END_SLAYER) && config.isEnchantEnabled(END_SLAYER)) {
                int level = getEnchantLevel(weapon, END_SLAYER);
                EntityType type = event.getEntity().getType();
                if (type == EntityType.ENDERMAN || type == EntityType.ENDERMITE || type == EntityType.SHULKER || type == EntityType.ENDER_DRAGON) {
                    double bonus = config.getEndSlayerBonus();
                    event.setDamage(event.getDamage() + (bonus * level));
                }
            }
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();

            if (hasEnchant(victim.getInventory().getLeggings(), AGILITY) && config.isEnchantEnabled(AGILITY)) {
                int level = getEnchantLevel(victim.getInventory().getLeggings(), AGILITY);
                int dodgeChancePerLevel = config.getAgilityDodgeChance();
                int dodgeChance = dodgeChancePerLevel * level;
                if (random.nextInt(100) < dodgeChance) {
                    event.setCancelled(true);
                    victim.sendMessage(ChatColor.GREEN + "*Dodged!*");
                }
            }
            if (hasEnchant(victim.getInventory().getChestplate(), TANK) && config.isEnchantEnabled(TANK)) {
                int level = getEnchantLevel(victim.getInventory().getChestplate(), TANK);
                double reductionPerLevel = config.getTankReductionPerLevel();
                double reduction = reductionPerLevel * level;
                event.setDamage(event.getDamage() * (1.0 - reduction));
            }
            if (event.getDamager() instanceof LivingEntity && hasEnchant(victim.getInventory().getItemInOffHand(), AURA_DAMAGE) && config.isEnchantEnabled(AURA_DAMAGE)) {
                int level = getEnchantLevel(victim.getInventory().getItemInOffHand(), AURA_DAMAGE);
                int burnChancePerLevel = config.getAuraDamageBurnChance();
                int burnChance = Math.min(burnChancePerLevel * level, 100);
                if (random.nextInt(100) < burnChance) {
                    int fireTicks = config.getAuraDamageFireTicks() + (config.getAuraDamagePerLevelTicks() * level);
                    ((LivingEntity) event.getDamager()).setFireTicks(fireTicks);
                }
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (killer.getInventory() == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() == Material.AIR) return;

        if (hasEnchant(weapon, EXPERIENCE) && config.isEnchantEnabled(EXPERIENCE)) {
            int level = getEnchantLevel(weapon, EXPERIENCE);
            double multiplier = config.getExperienceMultiplier();
            int xpMult = (int) (1 + (multiplier * level));
            event.setDroppedExp(event.getDroppedExp() * xpMult);
        }
        if (hasEnchant(weapon, BEHEADING) && config.isEnchantEnabled(BEHEADING)) {
            int level = getEnchantLevel(weapon, BEHEADING);
            int dropChancePerLevel = config.getBeheadingDropChance();
            int dropChance = dropChancePerLevel * level;
            if (random.nextInt(100) < dropChance) {
                ItemStack head = getHeadForEntity(event.getEntity());
                if (head != null) {
                    event.getDrops().add(head);
                }
            }
        }
        if (hasEnchant(weapon, TELEPATHY) && config.isEnchantEnabled(TELEPATHY)) {
            for (ItemStack drop : event.getDrops()) killer.getInventory().addItem(drop);
            event.getDrops().clear();
        }
    }

    private ItemStack getHeadForEntity(Entity entity) {
        EntityType type = entity.getType();
        ItemStack head = null;

        switch (type) {
            case PLAYER:
                head = new ItemStack(Material.PLAYER_HEAD);
                break;
            case ZOMBIE:
                head = new ItemStack(Material.ZOMBIE_HEAD);
                break;
            case SKELETON:
                head = new ItemStack(Material.SKELETON_SKULL);
                break;
            case CREEPER:
                head = new ItemStack(Material.CREEPER_HEAD);
                break;
            case WITHER_SKELETON:
                head = new ItemStack(Material.WITHER_SKELETON_SKULL);
                break;
            case ENDER_DRAGON:
                head = new ItemStack(Material.DRAGON_HEAD);
                break;
            case PIGLIN:
            case PIGLIN_BRUTE:
                head = new ItemStack(Material.PIGLIN_HEAD);
                break;
            default:
                return null;
        }

        return head;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        Block b = event.getBlock();

        if (hasEnchant(tool, TELEPATHY) && config.isEnchantEnabled(TELEPATHY)) {
            event.setDropItems(false);
            for (ItemStack drop : b.getDrops(tool)) p.getInventory().addItem(drop);
        }

        if (hasEnchant(tool, MELTING) && config.isEnchantEnabled(MELTING)) {
            Material blockType = b.getType();
            Material dropItem = null;

            if (blockType == Material.IRON_ORE || blockType == Material.DEEPSLATE_IRON_ORE || blockType == Material.RAW_IRON_BLOCK) {
                dropItem = Material.IRON_INGOT;
            } else if (blockType == Material.GOLD_ORE || blockType == Material.DEEPSLATE_GOLD_ORE) {
                dropItem = Material.GOLD_INGOT;
            } else if (blockType == Material.COPPER_ORE || blockType == Material.DEEPSLATE_COPPER_ORE) {
                dropItem = Material.RAW_COPPER;
            }

            if (dropItem != null) {
                event.setDropItems(false);
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(dropItem));
            }
        }

        if (hasEnchant(tool, TIMBER) && config.isEnchantEnabled(TIMBER) && b.getType().name().contains("LOG")) {
            breakVein(b, b.getType(), 0, config.getTimberMaxBlocks(), tool, false);
        }
        if (hasEnchant(tool, VEIN_MINER) && config.isEnchantEnabled(VEIN_MINER) && b.getType().name().contains("ORE")) {
            boolean hasMelting = hasEnchant(tool, MELTING) && config.isEnchantEnabled(MELTING);
            breakVein(b, b.getType(), 0, config.getVeinMinerMaxBlocks(), tool, hasMelting);
        }
    }

    private void breakVein(Block b, Material type, int count, int limit, ItemStack tool, boolean applyMelting) {
        if (count > limit || b.getType() != type) return;

        if (applyMelting) {
            Material dropItem = null;

            if (type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE || type == Material.RAW_IRON_BLOCK) {
                dropItem = Material.IRON_INGOT;
            } else if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE) {
                dropItem = Material.GOLD_INGOT;
            } else if (type == Material.COPPER_ORE || type == Material.DEEPSLATE_COPPER_ORE) {
                dropItem = Material.RAW_COPPER;
            }

            if (dropItem != null) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(dropItem));
                b.setType(Material.AIR);
            } else {
                b.breakNaturally();
            }
        } else {
            b.breakNaturally();
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block relative = b.getRelative(x, y, z);
                    if (relative.getType() == type) breakVein(relative, type, count + 1, limit, tool, applyMelting);
                }
            }
        }
    }

    private void handlePassiveEffects() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack boots = p.getInventory().getBoots();
            ItemStack helmet = p.getInventory().getHelmet();
            ItemStack leggings = p.getInventory().getLeggings();
            ItemStack chestplate = p.getInventory().getChestplate();

            if (hasEnchant(boots, SPEED_BOOST) && config.isEnchantEnabled(SPEED_BOOST)) {
                int level = getEnchantLevel(boots, SPEED_BOOST);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, level - 1));
            }
            if (hasEnchant(helmet, NIGHT_OWL) && config.isEnchantEnabled(NIGHT_OWL)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0));
            }
            if (hasEnchant(leggings, MAGNETIC) && config.isEnchantEnabled(MAGNETIC)) {
                int level = getEnchantLevel(leggings, MAGNETIC);
                int baseRadius = config.getMagneticBaseRadius();
                int perLevelRadius = config.getMagneticPerLevelRadius();
                int radius = baseRadius + (perLevelRadius * level);
                for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
                    if (e instanceof Item) e.teleport(p.getLocation());
                }
            }
            if (hasEnchant(chestplate, VITALITY) && config.isEnchantEnabled(VITALITY)) {
                int level = getEnchantLevel(chestplate, VITALITY);
                double heartsPerLevel = config.getVitalityHeartsPerLevel();
                double maxHealth = 20.0 + (heartsPerLevel * level);
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        Player p = event.getPlayer();
        if (hasEnchant(p.getInventory().getBoots(), LAVAWALKER) && config.isEnchantEnabled(LAVAWALKER)) {
            Block b = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (b.getType() == Material.LAVA) b.setType(Material.BASALT);
        }
    }
}
