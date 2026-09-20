package ru.core.accessories.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import ru.core.accessories.manager.PluginServices;
import ru.core.accessories.ui.AccessoryMenu;
import ru.core.accessories.ui.AccessoryMenuHolder;

import java.util.Map;

/**
 * Защищает GUI, обрабатывает обычный клик, shift-click, drag и hotbar swap.
 * Используется Bukkit-событиями инвентаря и не доверяет клиентскому слоту.
 * Новое правило перемещения добавляется в handleMenuClick до вызова save.
 */
public final class AccessoryListener implements Listener {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final PluginServices services;

    public AccessoryListener(org.bukkit.plugin.java.JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getHolder() instanceof AccessoryMenuHolder holder) {
            event.setCancelled(true);
            handleMenuClick(event, player, holder);
            return;
        }
        if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING
                && event.getRawSlot() == 1) {
            ItemStack item = event.getCurrentItem();
            event.setCancelled(true);
            if (services.interfaceItems().isUi(item)) {
                services.menu().open(player);
            }
        }
    }

    private void handleMenuClick(InventoryClickEvent event, Player player, AccessoryMenuHolder holder) {
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.SWAP_OFFHAND) return;
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int button = event.getHotbarButton();
            if (button >= 0) {
                swapWithHotbar(player, holder, event.getRawSlot(), button);
            }
            return;
        }
        if (event.isShiftClick() && event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                for (String slot : AccessoryMenu.SLOT_INDEXES.keySet()) {
                    if (player.hasPermission(services.config().slotPermission(slot))
                            && services.menu().accepts(slot, item)
                            && !isReal(holder.inventory().getItem(AccessoryMenu.SLOT_INDEXES.get(slot)))) {
                        holder.inventory().setItem(AccessoryMenu.SLOT_INDEXES.get(slot), item.clone());
                        event.getClickedInventory().setItem(event.getSlot(), null);
                        break;
                    }
                }
            }
            services.menu().save(holder);
            services.effects().refresh(player);
            player.updateInventory();
            return;
        }
        String slot = slotByIndex(event.getRawSlot());
        if (slot == null || !player.hasPermission(services.config().slotPermission(slot))) return;
        ItemStack current = realItem(holder.inventory().getItem(event.getRawSlot()))
                ? holder.inventory().getItem(event.getRawSlot()).clone() : null;
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            if (!services.menu().accepts(slot, cursor)) return;
            holder.inventory().setItem(event.getRawSlot(), cursor.clone());
            player.setItemOnCursor(current == null ? new ItemStack(org.bukkit.Material.AIR) : current);
        } else if (current != null) {
            holder.inventory().setItem(event.getRawSlot(), services.interfaceItems().silhouette(slot));
            player.setItemOnCursor(current);
        }
        services.menu().save(holder);
        services.effects().refresh(player);
        player.updateInventory();
    }

    private void swapWithHotbar(Player player, AccessoryMenuHolder holder, int rawSlot, int hotbar) {
        String slot = slotByIndex(rawSlot);
        if (slot == null || !player.hasPermission(services.config().slotPermission(slot))) return;
        ItemStack hotbarItem = player.getInventory().getItem(hotbar);
        if (hotbarItem != null && !hotbarItem.getType().isAir()
                && !services.menu().accepts(slot, hotbarItem)) return;
        ItemStack current = realItem(holder.inventory().getItem(rawSlot))
                ? holder.inventory().getItem(rawSlot).clone() : null;
        holder.inventory().setItem(rawSlot, hotbarItem == null
                ? services.interfaceItems().silhouette(slot) : hotbarItem.clone());
        player.getInventory().setItem(hotbar, current);
    }

    private String slotByIndex(int raw) {
        for (Map.Entry<String, Integer> entry : AccessoryMenu.SLOT_INDEXES.entrySet()) {
            if (entry.getValue() == raw) return entry.getKey();
        }
        return null;
    }

    private boolean realItem(ItemStack item) {
        return item != null && !item.getType().isAir() && !services.interfaceItems().isUi(item)
                && services.library().getId(item).flatMap(id -> java.util.Optional.ofNullable(
                        services.config().definition(id))).isPresent();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AccessoryMenuHolder) {
            event.setCancelled(true);
        } else if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING
                && event.getRawSlots().contains(1)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AccessoryMenuHolder holder) {
            services.menu().save(holder);
            services.effects().refresh(holder.player());
            Bukkit.getScheduler().runTask(plugin, () ->
                    holder.player().getOpenInventory().setItem(1, services.interfaceItems().button()));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (services.interfaceItems().isUi(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getMatrix() != null) {
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (services.interfaceItems().isUi(item)) {
                    event.getInventory().setResult(new ItemStack(org.bukkit.Material.AIR));
                    return;
                }
            }
        }
    }
}