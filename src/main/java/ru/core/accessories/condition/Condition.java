package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.state.PlayerStateTracker;

/**
 * Проверяет одно условие группы эффекта.
 * Используется ConditionRegistry и EffectEngine.
 * Новое условие добавляется отдельным классом и регистрируется в ConditionRegistry.
 */
public interface Condition {
    boolean test(Player player, PlayerStateTracker.State state);
}