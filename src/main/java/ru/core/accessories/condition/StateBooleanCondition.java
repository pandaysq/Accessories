package ru.core.accessories.condition;

import org.bukkit.entity.Player;
import ru.core.accessories.state.PlayerStateTracker;

/**
 * Проверяет флаги движения и состояния игрока.
 * Используется not_on_ground, swimming, in_water, sprinting, sneaking, gliding, on_fire и riding.
 * Новое логическое состояние добавляется в switch.
 */
public final class StateBooleanCondition implements Condition {
    private final String type;
    private final boolean expected;
    public StateBooleanCondition(String type, boolean expected) {
        this.type = type;
        this.expected = expected;
    }
    @Override public boolean test(Player player, PlayerStateTracker.State state) {
        boolean actual = switch (type) {
            case "not_on_ground" -> !player.isOnGround();
            case "swimming" -> player.isSwimming();
            case "in_water" -> player.isInWater();
            case "sprinting" -> player.isSprinting();
            case "sneaking" -> player.isSneaking();
            case "gliding" -> player.isGliding();
            case "on_fire" -> player.getFireTicks() > 0;
            case "riding" -> player.isInsideVehicle();
            case "blocking" -> player.isBlocking();
            default -> false;
        };
        return actual == expected;
    }
}