package ru.core.accessories.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import ru.core.accessories.config.AccessoryDefinition;
import ru.core.accessories.manager.PluginServices;
import ru.core.accessories.state.AccessoryStorage;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Загружает и сохраняет аксессуары, восстанавливает кнопку и записывает события для условий.
 * Используется событиями жизненного цикла, урона, убийства и смерти игрока.
 * Новое событие для условий добавляется отдельным обработчиком с recordEvent.
 */
public final class PlayerListener implements Listener {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final PluginServices services;

    public PlayerListener(org.bukkit.plugin.java.JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            restoreButton(event.getPlayer());
            services.effects().refresh(event.getPlayer());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        services.effects().clear(event.getPlayer());
        services.state().remove(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            restoreButton(event.getPlayer());
            services.effects().refresh(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) services.state().damage(player);
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) services.state().hit(player, event.getEntity());
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        services.state().recordEvent(killer, "killed_any");
        if (event.getEntity() instanceof Player) services.state().recordEvent(killer, "killed_player");
        else services.state().recordEvent(killer, "killed_mob");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        services.state().recordEvent(player, "died");
        services.effects().clear(player);
        Map<String, ItemStack> equipped = services.menu().storage().load(player);
        for (String slot : AccessoryStorage.SLOT_NAMES) {
            ItemStack item = equipped.get(slot);
            if (item == null) continue;
            AccessoryDefinition definition = services.config().definition(
                    services.library().getId(item).orElse(""));
            boolean keep = definition != null && ThreadLocalRandom.current().nextInt(100)
                    < definition.deathKeepChance();
            if (!keep) {
                event.getDrops().add(item.clone());
                equipped.remove(slot);
            }
        }
        services.menu().storage().save(player, equipped);
        event.getDrops().removeIf(services.interfaceItems()::isUi);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ())) {
            services.state().move(event.getPlayer());
        }
    }

    private void restoreButton(Player player) {
        if (player.getOpenInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) return;
        ItemStack current = player.getOpenInventory().getItem(1);
        if (current == null || services.interfaceItems().isUi(current)) {
            player.getOpenInventory().setItem(1, services.interfaceItems().button());
        }
    }
}