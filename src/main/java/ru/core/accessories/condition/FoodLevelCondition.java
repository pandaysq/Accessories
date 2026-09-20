package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.state.PlayerStateTracker;

/**
 * Проверяет food level игрока.
 * Используется type: food_level.
 * Для нового числового состояния добавьте отдельный класс по образцу.
 */
public final class FoodLevelCondition implements Condition {
    private final String value;
    public FoodLevelCondition(String value) { this.value = value; }
    @Override public boolean test(Player player, PlayerStateTracker.State state) {
        return NumberMatcher.matches(player.getFoodLevel(), value);
    }
}