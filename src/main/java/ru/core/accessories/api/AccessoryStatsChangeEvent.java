package ru.core.accessories.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * Уведомляет об изменении итоговых custom stats игрока после пересчёта.
 * Событие неотменяемое; внешние плагины используют его для обновления своего
 * кэша урона или защиты.
 */
public final class AccessoryStatsChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Map<String, Double> oldStats;
    private final Map<String, Double> newStats;

    public AccessoryStatsChangeEvent(Player player, Map<String, Double> oldStats,
                                     Map<String, Double> newStats) {
        this.player = player;
        this.oldStats = Map.copyOf(oldStats);
        this.newStats = Map.copyOf(newStats);
    }

    public Player getPlayer() { return player; }
    public Map<String, Double> getOldStats() { return oldStats; }
    public Map<String, Double> getNewStats() { return newStats; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}