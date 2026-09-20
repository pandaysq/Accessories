package ru.core.accessories.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Хранит игрока и реальные предметы открытого меню аксессуаров.
 * Используется AccessoryMenu и AccessoryListener, чтобы отличать меню от обычного инвентаря.
 * Новый рабочий слот добавляется в slotIndexes и в карту holder.
 */
public final class AccessoryMenuHolder implements InventoryHolder {
    private final org.bukkit.entity.Player player;
    private final Map<String, ItemStack> items;
    private Inventory inventory;

    public AccessoryMenuHolder(org.bukkit.entity.Player player, Map<String, ItemStack> items) {
        this.player = player;
        this.items = new LinkedHashMap<>(items);
    }

    public org.bukkit.entity.Player player() { return player; }
    public Map<String, ItemStack> items() { return items; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    public Inventory inventory() { return inventory; }
    @Override public Inventory getInventory() { return inventory; }
}