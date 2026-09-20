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
import ru.core.accessories.config.AccessoryConfig;
import ru.core.accessories.config.AccessoryDefinition;
import ru.core.accessories.config.AttributeRegistry;
import ru.core.accessories.config.EffectGroup;
import ru.core.accessories.condition.Condition;
import ru.core.accessories.condition.ConditionRegistry;
import ru.core.accessories.integration.LibraryBridge;
import ru.core.accessories.state.NoManaProvider;
import ru.core.accessories.state.PlayerStateTracker;
import ru.core.accessories.state.AccessoryStorage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
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
    private ConditionRegistry conditions;
    private final Map<UUID, Set<AttributeModifier>> modifiers = new HashMap<>();
    private final Map<UUID, Set<PotionEffectType>> potions = new HashMap<>();
    private int taskId = -1;

    public EffectEngine(JavaPlugin plugin, AccessoryConfig config, LibraryBridge library,
                        PlayerStateTracker state) {
        this.plugin = plugin;
        this.config = config;
        this.library = library;
        this.state = state;
        this.storage = new AccessoryStorage(plugin);
        this.conditions = new ConditionRegistry(new NoManaProvider(),
                config.targetRayDistance(), config.targetLastHitSeconds());
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
        conditions = new ConditionRegistry(new NoManaProvider(),
                config.targetRayDistance(), config.targetLastHitSeconds());
    }

    public void refresh(Player player) {
        clearModifiers(player);
        clearPotions(player);
        Map<String, ItemStack> equipped = storage.load(player);
        PlayerStateTracker.State playerState = state.state(player);
        for (String slot : AccessoryStorage.SLOT_NAMES) {
            if (!player.hasPermission(config.slotPermission(slot))) continue;
            ItemStack item = equipped.get(slot);
            String id = library.getId(item).orElse(null);
            AccessoryDefinition definition = id == null ? null : config.definition(id);
            if (definition == null) continue;
            for (EffectGroup group : definition.effects()) {
                if (active(group, player, playerState)) {
                    applyGroup(player, slot, definition.id(), group);
                }
            }
        }
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    public void clear(Player player) {
        clearModifiers(player);
        clearPotions(player);
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