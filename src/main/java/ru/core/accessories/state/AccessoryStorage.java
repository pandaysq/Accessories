package ru.core.accessories.state;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сохраняет четыре слота аксессуаров в PDC игрока.
 * Используется меню при загрузке, закрытии и смерти игрока.
 * Новый слот добавляется в SLOT_NAMES и получает отдельный ключ PDC.
 */
public final class AccessoryStorage {
    public static final String[] SLOT_NAMES = {"necklace", "emblem", "ring1", "ring2"};
    private final NamespacedKey namespace;

    public AccessoryStorage(org.bukkit.plugin.java.JavaPlugin plugin) {
        namespace = new NamespacedKey(plugin, "slots");
    }

    public Map<String, ItemStack> load(Player player) {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        for (String slot : SLOT_NAMES) {
            byte[] bytes = player.getPersistentDataContainer().get(key(slot), PersistentDataType.BYTE_ARRAY);
            if (bytes != null) {
                try {
                    result.put(slot, ItemStack.deserializeBytes(bytes));
                } catch (IllegalArgumentException ignored) {
                    result.remove(slot);
                }
            }
        }
        return result;
    }

    public void save(Player player, Map<String, ItemStack> items) {
        for (String slot : SLOT_NAMES) {
            ItemStack item = items.get(slot);
            if (item == null || item.getType().isAir()) {
                player.getPersistentDataContainer().remove(key(slot));
            } else {
                player.getPersistentDataContainer().set(key(slot), PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
            }
        }
    }

    private NamespacedKey key(String slot) {
        return new NamespacedKey(namespace.getNamespace(), namespace.getKey() + "_" + slot);
    }
}