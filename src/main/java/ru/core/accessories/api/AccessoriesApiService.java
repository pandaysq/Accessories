package ru.core.accessories.api;

import org.bukkit.entity.Player;
import ru.core.accessories.effect.EffectEngine;

import java.util.Map;

/**
 * Адаптер между публичным AccessoriesAPI и кэшем EffectEngine.
 * Используется ServicesManager и не содержит собственной логики пересчёта;
 * новые API-методы делегируются сюда после добавления в EffectEngine.
 */
public final class AccessoriesApiService implements AccessoriesAPI {
    private final EffectEngine effects;

    public AccessoriesApiService(EffectEngine effects) {
        this.effects = effects;
    }

    @Override public double getStat(Player player, String stat) { return effects.getStat(player, stat); }
    @Override public Map<String, Double> getStats(Player player) { return effects.getStats(player); }
    @Override public boolean hasAccessory(Player player, String accessoryId) {
        return effects.hasAccessory(player, accessoryId);
    }
}