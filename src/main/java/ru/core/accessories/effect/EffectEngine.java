package ru.core.accessories.effect;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.accessories.api.AccessoryStatsChangeEvent;
import ru.core.accessories.config.AccessoryConfig;
import ru.core.accessories.config.AccessoryDefinition;
import ru.core.accessories.config.AttributeRegistry;
import ru.core.accessories.config.EffectGroup;
import ru.core.accessories.condition.Condition;
import ru.core.accessories.condition.ConditionRegistry;
import ru.core.accessories.integration.LibraryBridge;
import ru.core.accessories.state.ManaProvider;
import ru.core.accessories.state.PlayerStateTracker;
import ru.core.accessories.state.AccessoryStorage;
import ru.core.accessories.stats.StatCalculator;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Пересчитывает атрибуты и зелья надетых аксессуаров на основном потоке.
 * Используется планировщиком и слушателями после изменения экипировки или событий.
 * Новый эффект добавляется в applyGroup, а новое условие — через ConditionRegistry.
 */
public final class EffectEngine {
    private final JavaPlugin plugin;
    private final AccessoryConfig config;
    private final LibraryBridge library;
    private final PlayerStateTracker state;
    private final AccessoryStorage storage;
    private final ManaProvider mana;
    private final StatCalculator statCalculator = new StatCalculator();
    private ConditionRegistry conditions;
    private final Map<UUID, Set<AttributeModifier>> modifiers = new HashMap<>();
    private final Map<UUID, Set<PotionEffectType>> potions = new HashMap<>();
    private final Map<UUID, Map<String, Double>> statCache = new HashMap<>();
    private final Map<UUID, Map<String, Map<String, Double>>> statContributions = new HashMap<>();
    private final Map<UUID, Set<String>> manaModifierKeys = new HashMap<>();
    private int taskId = -1;

    public EffectEngine(JavaPlugin plugin, AccessoryConfig config, LibraryBridge library,
                        PlayerStateTracker state, ManaProvider mana) {
        this.plugin = plugin;
        this.config = config;
        this.library = library;
        this.state = state;
        this.storage = new AccessoryStorage(plugin);
        this.mana = mana;
        this.conditions = new ConditionRegistry(mana,
                config.targetRayDistance(), config.targetLastHitSeconds(), config.targetSource());
    }

    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOnline,
                config.checkInterval(), config.checkInterval()).getTaskId();
    }

    public void shutdown() {
        if (taskId >= 0) Bukkit.getScheduler().cancelTask(taskId);
        for (Player player : Bukkit.getOnlinePlayers()) clear(player);
    }

    public void refreshOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void reloadConfiguration() {
        conditions = new ConditionRegistry(mana,
                config.targetRayDistance(), config.targetLastHitSeconds(), config.targetSource());
    }

    public void refresh(Player player) {
        clearModifiers(player);
        clearPotions(player);
        clearManaModifiers(player);
        Map<String, ItemStack> equipped = storage.load(player);
        PlayerStateTracker.State playerState = state.state(player);
        Map<String, List<EffectGroup.StatDefinition>> statDefinitions = new HashMap<>();
        Map<String, Map<String, List<EffectGroup.StatDefinition>>> contributionDefinitions = new HashMap<>();
        for (String slot : AccessoryStorage.SLOT_NAMES) {
            if (!player.hasPermission(config.slotPermission(slot))) continue;
            ItemStack item = equipped.get(slot);
            String id = library.getId(item).orElse(null);
            AccessoryDefinition definition = id == null ? null : config.definition(id);
            if (definition == null) continue;
            applyManaModifiers(player, slot, definition);
            for (EffectGroup group : definition.effects()) {
                if (active(group, player, playerState)) {
                    applyGroup(player, slot, definition.id(), group);
                    for (EffectGroup.StatDefinition stat : group.stats()) {
                        statDefinitions.computeIfAbsent(stat.stat(), ignored -> new java.util.ArrayList<>()).add(stat);
                        contributionDefinitions.computeIfAbsent(definition.id(), ignored -> new HashMap<>())
                                .computeIfAbsent(stat.stat(), ignored -> new java.util.ArrayList<>()).add(stat);
                    }
                }
            }
        }
        updateStats(player, statDefinitions, calculateContributions(contributionDefinitions));
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    public void clear(Player player) {
        clearModifiers(player);
        clearPotions(player);
        clearManaModifiers(player);
        statCache.remove(player.getUniqueId());
        statContributions.remove(player.getUniqueId());
    }

    public double getStat(Player player, String stat) {
        return statCache.getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault(stat.toLowerCase(), 0.0);
    }

    public Map<String, Double> getStats(Player player) {
        return Map.copyOf(statCache.getOrDefault(player.getUniqueId(), Map.of()));
    }

    public boolean hasAccessory(Player player, String accessoryId) {
        Map<String, ItemStack> equipped = storage.load(player);
        for (String slot : AccessoryStorage.SLOT_NAMES) {
            if (!player.hasPermission(config.slotPermission(slot))) continue;
            ItemStack item = equipped.get(slot);
            String id = library.getId(item).orElse(null);
            if (accessoryId.equals(id)) return true;
        }
        return false;
    }

    public Map<String, Map<String, Double>> statContributions(Player player) {
        return Map.copyOf(statContributions.getOrDefault(player.getUniqueId(), Map.of()));
    }

    private boolean active(EffectGroup group, Player player, PlayerStateTracker.State playerState) {
        for (ru.core.accessories.config.ConditionDefinition definition : group.conditions()) {
            if (isEvent(definition.type())) {
                if (group.durationAfterTrigger() <= 0) return false;
                long triggeredAt = playerState.eventTriggeredAt(definition.type());
                if (triggeredAt == 0 || System.currentTimeMillis() - triggeredAt
                        > group.durationAfterTrigger() * 1000L) return false;
            }
        }
        if (group.conditions().isEmpty()) return true;
        boolean any = group.mode().equalsIgnoreCase("any");
        if (any) {
            for (Condition condition : conditions.create(group.conditions())) {
                if (condition.test(player, playerState)) return true;
            }
            return false;
        }
        for (Condition condition : conditions.create(group.conditions())) {
            if (!condition.test(player, playerState)) return false;
        }
        return true;
    }

    private boolean isEvent(String type) {
        return type.equals("killed_player") || type.equals("killed_mob")
                || type.equals("killed_any") || type.equals("died");
    }

    private void applyGroup(Player player, String slot, String id, EffectGroup group) {
        for (EffectGroup.AttributeDefinition definition : group.attributes()) {
            Attribute attribute = AttributeRegistry.attribute(definition.attribute());
            if (attribute == null) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            String keyName = "effect_" + slot + "_" + id + "_" + group.key().replaceAll("[^a-zA-Z0-9_.-]", "_");
            NamespacedKey key = new NamespacedKey(plugin, keyName);
            AttributeModifier modifier = new AttributeModifier(key, definition.amount(),
                    AttributeRegistry.operation(definition.operation()));
            instance.addModifier(modifier);
            modifiers.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(modifier);
        }
        for (EffectGroup.PotionDefinition definition : group.potions()) {
            PotionEffectType type = PotionEffectType.getByName(definition.type().toUpperCase());
            if (type == null) continue;
            player.addPotionEffect(new PotionEffect(type, Math.max(20, config.checkInterval() * 3),
                    definition.amplifier(), false, definition.particles(), definition.icon()), true);
            potions.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(type);
        }
    }

    private void updateStats(Player player, Map<String, java.util.List<EffectGroup.StatDefinition>> definitions,
                             Map<String, Map<String, Double>> contributions) {
        Map<String, Double> next = new HashMap<>();
        for (Map.Entry<String, java.util.List<EffectGroup.StatDefinition>> entry : definitions.entrySet()) {
            double value = statCalculator.calculate(entry.getValue());
            if (Math.abs(value) > 0.0000001) next.put(entry.getKey(), value);
        }
        Map<String, Double> old = statCache.put(player.getUniqueId(), Map.copyOf(next));
        statContributions.put(player.getUniqueId(), deepCopy(contributions));
        if (old == null) old = Map.of();
        if (!old.equals(next)) Bukkit.getPluginManager().callEvent(
                new AccessoryStatsChangeEvent(player, old, next));
    }

    private Map<String, Map<String, Double>> deepCopy(Map<String, Map<String, Double>> source) {
        Map<String, Map<String, Double>> copy = new HashMap<>();
        source.forEach((id, values) -> copy.put(id, Map.copyOf(values)));
        return Map.copyOf(copy);
    }

    private Map<String, Map<String, Double>> calculateContributions(
            Map<String, Map<String, List<EffectGroup.StatDefinition>>> definitions) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        definitions.forEach((accessory, stats) -> {
            Map<String, Double> values = new HashMap<>();
            stats.forEach((stat, entries) -> values.put(stat, statCalculator.calculate(entries)));
            result.put(accessory, values);
        });
        return result;
    }

    private void applyManaModifiers(Player player, String slot, AccessoryDefinition definition) {
        String key = "accessories:" + slot + ":" + definition.id();
        if (definition.manaMaxBonus() != 0.0) {
            mana.addMaxModifier(player, key, definition.manaMaxBonus());
            manaModifierKeys.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(key + ":max");
        }
        if (definition.manaRegenBonus() != 0.0) {
            mana.addRegenModifier(player, key, definition.manaRegenBonus());
            manaModifierKeys.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(key + ":regen");
        }
    }

    private void clearManaModifiers(Player player) {
        Set<String> keys = manaModifierKeys.remove(player.getUniqueId());
        if (keys == null) return;
        for (String key : keys) {
            if (key.endsWith(":max")) mana.removeMaxModifier(player, key.substring(0, key.length() - 4));
            if (key.endsWith(":regen")) mana.removeRegenModifier(player, key.substring(0, key.length() - 6));
        }
    }

    private void clearModifiers(Player player) {
        Set<AttributeModifier> active = modifiers.remove(player.getUniqueId());
        if (active == null) return;
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : active) instance.removeModifier(modifier);
        }
    }

    private void clearPotions(Player player) {
        Set<PotionEffectType> active = potions.remove(player.getUniqueId());
        if (active != null) {
            for (PotionEffectType type : active) player.removePotionEffect(type);
        }
    }
}