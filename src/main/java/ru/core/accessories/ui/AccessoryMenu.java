package ru.core.accessories.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.core.accessories.config.AccessoryConfig;
import ru.core.accessories.config.AccessoryDefinition;
import ru.core.accessories.integration.LibraryBridge;
import ru.core.accessories.state.AccessoryStorage;

import java.util.Map;

/**
 * Открывает девятислотовое меню и отображает четыре слота аксессуаров.
 * Используется кнопкой интерфейса и командными/инвентарными слушателями.
 * Новый слот добавляется в slotIndexes и в правила отображения этого класса.
 */
public final class AccessoryMenu {
    public static final Map<String, Integer> SLOT_INDEXES = Map.of(
            "necklace", 1, "emblem", 3, "ring1", 5, "ring2", 7);
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final AccessoryConfig config;
    private final LibraryBridge library;
    private final InterfaceItems interfaceItems;
    private final AccessoryStorage storage;

    public AccessoryMenu(org.bukkit.plugin.java.JavaPlugin plugin, AccessoryConfig config,
                         LibraryBridge library, InterfaceItems interfaceItems) {
        this.plugin = plugin;
        this.config = config;
        this.library = library;
        this.interfaceItems = interfaceItems;
        this.storage = new AccessoryStorage(plugin);
    }

    public void open(Player player) {
        AccessoryMenuHolder holder = new AccessoryMenuHolder(player, storage.load(player));
        Inventory inventory = Bukkit.createInventory(holder, 9, net.kyori.adventure.text.Component.text("Аксессуары"));
        holder.inventory(inventory);
        for (int index = 0; index < inventory.getSize(); index++) inventory.setItem(index, interfaceItems.background());
        for (Map.Entry<String, Integer> entry : SLOT_INDEXES.entrySet()) {
            String slot = entry.getKey();
            if (!player.hasPermission(config.slotPermission(slot))) {
                inventory.setItem(entry.getValue(), interfaceItems.locked());
            } else {
                ItemStack item = holder.items().get(slot);
                inventory.setItem(entry.getValue(), item == null ? interfaceItems.silhouette(slot) : item.clone());
            }
        }
        player.openInventory(inventory);
    }

    public void save(AccessoryMenuHolder holder) {
        for (Map.Entry<String, Integer> entry : SLOT_INDEXES.entrySet()) {
            ItemStack item = holder.inventory().getItem(entry.getValue());
            if (item == null || interfaceItems.isUi(item)
                    || library.getId(item).flatMap(id -> java.util.Optional.ofNullable(config.definition(id))).isEmpty()) {
                holder.items().remove(entry.getKey());
            } else {
                holder.items().put(entry.getKey(), item.clone());
            }
        }
        storage.save(holder.player(), holder.items());
    }

    public AccessoryStorage storage() {
        return storage;
    }

    public boolean accepts(String slot, ItemStack item) {
        if (item == null || interfaceItems.isUi(item)) return false;
        String id = library.getId(item).orElse(null);
        if (id == null) return false;
        AccessoryDefinition definition = config.definition(id);
        if (definition == null) return false;
        return definition.type() == config.slotType(slot);
    }
}