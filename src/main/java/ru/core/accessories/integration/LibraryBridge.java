package ru.core.accessories.integration;

import com.github.MrFrizz69.customitemslibary.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Единственная точка обращения к CustomItemsLibrary.
 * Используется AccessoryConfig, командой выдачи и меню для чтения custom_id.
 * При изменении API библиотеки меняется только этот класс.
 */
public final class LibraryBridge {
    public void register(String id, Material material, String name, int model, List<String> lore,
                         List<Enchantment> enchantments, List<Integer> levels,
                         List<ItemFlag> flags, boolean unbreakable) {
        ItemRegistry.getInstance().register(id, material, name, model, lore,
                Map.<Attribute, Double>of(), enchantments, levels, flags, unbreakable);
    }

    public void unregister(String id) {
        ItemRegistry.getInstance().unregister(id);
    }

    public Optional<ItemStack> create(String id) {
        return ItemRegistry.getInstance().createItem(id);
    }

    public Optional<String> getId(ItemStack item) {
        return item == null ? Optional.empty() : ItemRegistry.getInstance().getCustomId(item);
    }
}