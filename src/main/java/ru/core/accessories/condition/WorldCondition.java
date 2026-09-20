package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.state.PlayerStateTracker;

/**
 * Проверяет мир игрока по имени.
 * Используется type: world.
 * Для нового строкового сравнения добавьте отдельный Condition-класс.
 */
public final class WorldCondition implements Condition {
    private final String expected;
    public WorldCondition(String expected) { this.expected = expected; }
    @Override public boolean test(Player player, PlayerStateTracker.State state) {
        return player.getWorld().getName().equalsIgnoreCase(expected);
    }
}