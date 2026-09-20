package ru.core.accessories.ui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.core.accessories.config.AccessoryConfig;

import java.util.List;

/**
 * Создаёт служебные предметы GUI и кнопку в крафтовом инвентаре.
 * Используется меню, событиями клика и PlayerListener.
 * Новая визуальная роль добавляется через путь интерфейса в config.yml.
 */
public final class InterfaceItems {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final AccessoryConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey uiKey;

    public InterfaceItems(org.bukkit.plugin.java.JavaPlugin plugin, AccessoryConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.uiKey = new NamespacedKey(plugin, "ui");
    }

    public ItemStack button() {
        return item("interface.button");
    }

    public ItemStack background() {
        return item("interface.background");
    }

    public ItemStack silhouette(String slot) {
        return item("interface.slot-" + (slot.startsWith("ring") ? "ring" : slot));
    }

    public ItemStack locked() {
        return item("interface.slot-locked");
    }

    public boolean isUi(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(uiKey, PersistentDataType.STRING);
    }

    private ItemStack item(String path) {
        FileConfiguration yaml = config.main();
        Material material = Material.matchMaterial(yaml.getString(path + ".material", "PAPER"));
        ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(yaml.getString(path + ".name", " ")));
        List<net.kyori.adventure.text.Component> lore = yaml.getStringList(path + ".lore").stream()
                .map(miniMessage::deserialize).toList();
        if (!lore.isEmpty()) meta.lore(lore);
        int model = yaml.getInt(path + ".model-number", 0);
        if (model > 0) meta.setCustomModelData(model);
        meta.getPersistentDataContainer().set(uiKey, PersistentDataType.STRING, path);
        item.setItemMeta(meta);
        return item;
    }
}