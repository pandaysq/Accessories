package ru.core.accessories.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.accessories.integration.LibraryBridge;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Загружает config.yml и accessories.yml и синхронизирует предметы с библиотекой.
 * Используется всеми сервисами как единственный источник свойств аксессуара.
 * Новые конфигурационные поля читаются здесь; библиотечные вызовы добавляются только через LibraryBridge.
 */
public final class AccessoryConfig {
    private final JavaPlugin plugin;
    private FileConfiguration main;
    private FileConfiguration accessoriesFile;
    private final Map<String, AccessoryDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, String> registeredIds = new HashMap<>();

    public AccessoryConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadFiles();
    }

    public void reloadAndRegister(LibraryBridge library) {
        reloadFiles();
        for (String id : new ArrayList<>(registeredIds.keySet())) {
            if (!definitions.containsKey(id)) {
                library.unregister(id);
                registeredIds.remove(id);
            }
        }
        for (AccessoryDefinition definition : definitions.values()) {
            register(library, definition);
        }
    }

    private void register(LibraryBridge library, AccessoryDefinition definition) {
        Material material = Material.matchMaterial(definition.material());
        if (material == null) {
            plugin.getLogger().warning("Неизвестный материал аксессуара " + definition.id());
            return;
        }
        library.register(definition.id(), material, definition.name(), definition.modelNumber(),
                definition.lore(), List.of(), List.of(), List.of(), false);
        registeredIds.put(definition.id(), definition.id());
    }

    private void reloadFiles() {
        plugin.reloadConfig();
        File file = new File(plugin.getDataFolder(), "accessories.yml");
        accessoriesFile = YamlConfiguration.loadConfiguration(file);
        main = plugin.getConfig();
        definitions.clear();
        ConfigurationSection section = accessoriesFile.getConfigurationSection("accessories");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(id);
            if (item == null) continue;
            AccessoryDefinition definition = readDefinition(id, item);
            if (definition != null) definitions.put(id, definition);
        }
    }

    private AccessoryDefinition readDefinition(String id, ConfigurationSection item) {
        Material material = Material.matchMaterial(item.getString("material", ""));
        AccessoryDefinition.SlotType type;
        try {
            type = AccessoryDefinition.SlotType.valueOf(item.getString("type", "RING").toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Неизвестный тип аксессуара " + id);
            return null;
        }
        if (material == null) {
            plugin.getLogger().warning("Неизвестный материал аксессуара " + id);
            return null;
        }
        List<EffectGroup> effects = new ArrayList<>();
        for (Map<?, ?> raw : item.getMapList("effects")) {
            effects.add(readEffect(id, raw, effects.size() + 1));
        }
        int chance = Math.max(0, Math.min(100, item.getInt("death-keep-chance", 0)));
        return new AccessoryDefinition(id, item.getString("name", "<white>" + id),
                item.getStringList("lore"), type, material.name(), item.getInt("model-number", 0), chance, effects);
    }

    private EffectGroup readEffect(String id, Map<?, ?> raw, int number) {
        String key = id + "-" + value(raw, "group", number);
        String mode = String.valueOf(value(raw, "mode", "all")).toLowerCase();
        int duration = Integer.parseInt(String.valueOf(value(raw, "duration-after-trigger", 0)));
        List<ConditionDefinition> conditions = new ArrayList<>();
        Object rawConditions = raw.get("conditions");
        if (rawConditions instanceof List<?> list) {
            for (Object condition : list) {
                if (condition instanceof Map<?, ?> values) {
                    Map<String, Object> copy = new HashMap<>();
                    values.forEach((keyObject, value) -> copy.put(String.valueOf(keyObject), value));
                    String type = String.valueOf(copy.remove("type"));
                    conditions.add(new ConditionDefinition(type, copy));
                }
            }
        }
        List<EffectGroup.AttributeDefinition> attributes = new ArrayList<>();
        Object rawAttributes = raw.get("attributes");
        if (rawAttributes instanceof List<?> list) {
            for (Object attribute : list) {
                if (attribute instanceof Map<?, ?> values) {
                    attributes.add(new EffectGroup.AttributeDefinition(
                            String.valueOf(value(values, "attribute", "")),
                            String.valueOf(value(values, "operation", "ADD_NUMBER")),
                            Double.parseDouble(String.valueOf(value(values, "amount", 0)))));
                }
            }
        }
        List<EffectGroup.PotionDefinition> potions = new ArrayList<>();
        Object rawPotions = raw.get("potion-effects");
        if (rawPotions instanceof List<?> list) {
            for (Object potion : list) {
                if (potion instanceof Map<?, ?> values) {
                    potions.add(new EffectGroup.PotionDefinition(
                            String.valueOf(value(values, "type", "")),
                            Integer.parseInt(String.valueOf(value(values, "amplifier", 0))),
                            Boolean.parseBoolean(String.valueOf(value(values, "particles", true))),
                            Boolean.parseBoolean(String.valueOf(value(values, "icon", true)))));
                }
            }
        }
        for (ConditionDefinition condition : conditions) {
            if (isEventCondition(condition.type()) && duration <= 0) {
                plugin.getLogger().warning("Эффект " + key + " использует событие без duration-after-trigger > 0");
            }
        }
        return new EffectGroup(key, mode, duration, conditions, attributes, potions);
    }

    private Object value(Map<?, ?> values, Object key, Object fallback) {
        return values.containsKey(key) ? values.get(key) : fallback;
    }

    private boolean isEventCondition(String type) {
        return type.equals("killed_player") || type.equals("killed_mob")
                || type.equals("killed_any") || type.equals("died");
    }

    public FileConfiguration main() { return main; }
    public Map<String, AccessoryDefinition> definitions() {
        return Collections.unmodifiableMap(definitions);
    }
    public AccessoryDefinition definition(String id) { return definitions.get(id); }
    public ConfigurationSection section(String path) { return main.getConfigurationSection(path); }
    public String slotPermission(String slot) { return main.getString("slots." + slot + ".permission", ""); }
    public AccessoryDefinition.SlotType slotType(String slot) {
        return AccessoryDefinition.SlotType.valueOf(main.getString("slots." + slot + ".type", "RING"));
    }
    public int checkInterval() { return Math.max(1, main.getInt("settings.condition-check-interval-ticks", 10)); }
    public int targetLastHitSeconds() { return main.getInt("settings.target-last-hit-seconds", 15); }
    public int targetRayDistance() { return main.getInt("settings.target-ray-distance", 20); }
}