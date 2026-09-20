package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.state.PlayerStateTracker;

/**
 * Проверяет здоровье игрока в единицах Minecraft.
 * Используется группами с type: health.
 * Для нового источника числового значения добавьте аналогичный Condition-класс и регистрацию.
 */
public final class HealthCondition implements Condition {
    private final String value;
    public HealthCondition(String value) { this.value = value; }
    @Override public boolean test(Player player, PlayerStateTracker.State state) {
        return NumberMatcher.matches(player.getHealth(), value);
    }
}